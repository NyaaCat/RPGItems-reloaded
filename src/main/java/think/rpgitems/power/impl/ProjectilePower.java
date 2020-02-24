package think.rpgitems.power.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;
import think.rpgitems.utils.cast.CastUtils;
import think.rpgitems.utils.cast.RangedDoubleValue;
import think.rpgitems.utils.cast.RangedValueSerializer;
import think.rpgitems.utils.cast.RoundedConeInfo;

import java.util.Deque;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power projectile.
 * <p>
 * Launches projectile of type {@link #projectileType} with {@link #gravity} when right clicked.
 * If use {@link #isCone} mode, {@link #amount} of projectiles will randomly distributed in the cone
 * with angle {@link #range} centered with player's direction.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = ProjectilePower.Impl.class)
public class ProjectilePower extends BasePower {
    /**
     * Z_axis.
     */
    private static final Vector z_axis = new Vector(0, 0, 1);
    /**
     * X_axis.
     */
    private static final Vector x_axis = new Vector(1, 0, 0);
    /**
     * Y_axis.
     */
    private static final Vector y_axis = new Vector(0, 1, 0);
    @Property(order = 0)
    public int cooldown = 0;
    @Property(order = 1)
    public boolean isCone = false;
    @Property
    public boolean gravity = true;
    @Property(order = 3)
    public int range = 15;
    @Property(order = 4)
    public int amount = 5;
    @Property(order = 5)
    public double speed = 1;
    @Property
    public int cost = 0;
    @Property
    public int burstCount = 1;
    @Property
    public int burstInterval = 1;
    @Property
    public boolean setFireballDirection = false;
    @Property
    public Double yield = 0d;
    @Property
    public Boolean isIncendiary = false;
    @AcceptedValue({
            "skull",
            "fireball",
            "snowball",
            "smallfireball",
            "llamaspit",
            "arrow",
            "shulkerbullet",
            "dragonfireball",
            "trident",
    })
    @Deserializer(ProjectileType.class)
    @Serializer(ProjectileType.class)
    @Property(order = 2, required = true)
    public Class<? extends org.bukkit.entity.Projectile> projectileType = Snowball.class;
    @Property
    public boolean suppressArrow = false;
    @Property
    public boolean applyForce = false;
    @Property
    public boolean requireHurtByEntity = true;

    @Property
    public FiringLocation firingLocation = FiringLocation.SELF;

    @Property
    @Serializer(RangedValueSerializer.class)
    @Deserializer(RangedValueSerializer.class)
    public RangedDoubleValue firingR = RangedDoubleValue.of("10");

    @Property
    @Serializer(RangedValueSerializer.class)
    @Deserializer(RangedValueSerializer.class)
    public RangedDoubleValue firingTheta = RangedDoubleValue.of("0,10");

    @Property
    @Serializer(RangedValueSerializer.class)
    @Deserializer(RangedValueSerializer.class)
    public RangedDoubleValue firingPhi = RangedDoubleValue.of("0,360");

    @Property
    public double initialRotation = 0;

    @Property
    public double firingRange = 64;

    @Property
    public boolean castOff = false;

    public double getInitialRotation() {
        return initialRotation;
    }

    public FiringLocation getFiringLocation() {
        return firingLocation;
    }

    public RangedDoubleValue getFiringR() {
        return firingR;
    }

    public RangedDoubleValue getFiringTheta() {
        return firingTheta;
    }

    public RangedDoubleValue getFiringPhi() {
        return firingPhi;
    }

    public double getFiringRange() {
        return firingRange;
    }

    public boolean isCastOff() {
        return castOff;
    }

    enum FiringLocation {
        SELF, TARGET;
    }

    private Cache<UUID, Integer> burstTask = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).concurrencyLevel(2).build();

    @Override
    public void init(ConfigurationSection section) {
        cooldown = section.getInt("cooldownTime");
        super.init(section);
        if (getYield() != null && getYield() == -1) {
            yield = null;
        }
    }

    public Double getYield() {
        return yield;
    }

    /**
     * Amount of projectiles
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Burst count of one shoot
     */
    public int getBurstCount() {
        return burstCount;
    }

    /**
     * Interval between bursts
     */
    public int getBurstInterval() {
        return burstInterval;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    public Boolean getIncendiary() {
        return isIncendiary;
    }

    @Override
    public String getName() {
        return "projectile";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault(isCone() ? "power.projectile.cone" : "power.projectile.display", getProjectileType(getProjectileType()), (double) getCooldown() / 20d);
    }

    /**
     * Whether launch projectiles in cone
     */
    public boolean isCone() {
        return isCone;
    }

    public static String getProjectileType(Class<? extends Projectile> projectileType) {
        if (projectileType == WitherSkull.class)
            return "skull";
        else if (projectileType == Fireball.class)
            return "fireball";
        else if (projectileType == SmallFireball.class)
            return "smallfireball";
        else if (projectileType == Arrow.class)
            return "arrow";
        else if (projectileType == LlamaSpit.class)
            return "llamaspit";
        else if (projectileType == ShulkerBullet.class)
            return "shulkerbullet";
        else if (projectileType == DragonFireball.class)
            return "dragonfireball";
        else if (projectileType == Trident.class)
            return "trident";
        else
            return "snowball";
    }

    /**
     * Type of projectiles
     */
    public Class<? extends Projectile> getProjectileType() {
        return projectileType;
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Range will projectiles spread, in degree
     */
    public int getRange() {
        return range;
    }

    /**
     * Speed of projectiles
     */
    public double getSpeed() {
        return speed;
    }

    public boolean isApplyForce() {
        return applyForce;
    }

    /**
     * Whether the projectile have gravity
     */
    public boolean isGravity() {
        return gravity;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    /**
     * Whether to set Fireball' direction so it won't curve
     */
    public boolean isSetFireballDirection() {
        return setFireballDirection;
    }

    public boolean isSuppressArrow() {
        return suppressArrow;
    }

    public static class ProjectileType implements Getter, Setter {
        /**
         * Gets type name
         *
         * @return Type name
         */
        @Override
        @SuppressWarnings("unchecked")
        public String get(Object pt) {
            Class<? extends org.bukkit.entity.Projectile> projectileType = (Class<? extends org.bukkit.entity.Projectile>) pt;
            return getProjectileType(projectileType);
        }

        /**
         * Sets type from type name
         *
         * @param type Type name
         */
        @Override
        public Optional<Class<? extends Projectile>> set(String type) {
            switch (type) {
                case "skull":
                    return Optional.of(WitherSkull.class);
                case "fireball":
                    return Optional.of(Fireball.class);
                case "smallfireball":
                    return Optional.of(SmallFireball.class);
                case "arrow":
                    return Optional.of(Arrow.class);
                case "llamaspit":
                    return Optional.of(LlamaSpit.class);
                case "shulkerbullet":
                    return Optional.of(ShulkerBullet.class);
                case "dragonfireball":
                    return Optional.of(DragonFireball.class);
                case "trident":
                    return Optional.of(Trident.class);
                default:
                    return Optional.of(Snowball.class);
            }
        }
    }

    public class Impl implements PowerRightClick, PowerLeftClick, PowerSneak, PowerSprint, PowerHitTaken, PowerHit, PowerLivingEntity, PowerPlain, PowerBowShoot, PowerHurt {


        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            return fire(player, stack, 1);
        }

        public PowerResult<Void> fire(Player player, ItemStack stack, float speedFactor) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            CastUtils.CastLocation castLocation = null;
            if (getFiringLocation().equals(FiringLocation.TARGET)) {
                castLocation = CastUtils.rayTrace(player, player.getEyeLocation(), player.getEyeLocation().getDirection(), getFiringRange());
            }
            fire(player, stack, castLocation, speedFactor);
            UUID uuid = player.getUniqueId();
            if (getBurstCount() > 1) {
                Integer prev = burstTask.getIfPresent(uuid);
                if (prev != null) {
                    Bukkit.getScheduler().cancelTask(prev);
                }
                CastUtils.CastLocation finalCastLocation = castLocation;
                BukkitTask bukkitTask = (new BukkitRunnable() {
                    int count = getBurstCount() - 1;

                    @Override
                    public void run() {
                        if (player.getInventory().getItemInMainHand().equals(stack)) {
                            CastUtils.CastLocation castLocation1 = finalCastLocation;
                            if (!isCastOff()){
                                castLocation1 = CastUtils.rayTrace(player, player.getEyeLocation(), player.getEyeLocation().getDirection(), getFiringRange());
                            }
                            burstTask.put(uuid, this.getTaskId());
                            if (count-- > 0) {
                                fire(player, stack, castLocation1, speedFactor);
                                return;
                            }
                        }
                        burstTask.invalidate(uuid);
                        this.cancel();
                    }
                }).runTaskTimer(RPGItems.plugin, getBurstInterval(), getBurstInterval());
                burstTask.put(uuid, bukkitTask.getTaskId());
            }
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return ProjectilePower.this;
        }

        private RoundedConeInfo generateConeInfo(double cone, RangedDoubleValue firingR, RangedDoubleValue firingTheta, RangedDoubleValue firingPhi, double initialRotation) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            double phi = random.nextDouble() * 360;
            double theta = 0;
            if (cone != 0) {
                theta = random.nextDouble() * cone;
            }
            double r = firingR.random();
            double rPhi = firingPhi.random();
            double rTheta = firingTheta.random();
            return new RoundedConeInfo(theta, phi, r, rPhi, rTheta, initialRotation);
        }

        private void fire(Player player, ItemStack stack, CastUtils.CastLocation castLocation, float speedFactor) {

            for (int i = 0; i < (isCone() ? getAmount() : 1); i++) {
                LivingEntity source = player;
                RoundedConeInfo roundedConeInfo = generateConeInfo(isCone() ? getRange() : 0, getFiringR(), getFiringTheta(), getFiringPhi(), getInitialRotation());
                if (getFiringLocation().equals(FiringLocation.TARGET) && castLocation != null) {
                    Location targetLocation = castLocation.getTargetLocation();
                    Location fireLocation = CastUtils.parseFiringLocation(targetLocation, y_axis, player.getEyeLocation(), roundedConeInfo);
                    World world = player.getWorld();
                    ArmorStand spawn = world.spawn(fireLocation, ArmorStand.class, armorStand -> {
                        armorStand.setVisible(false);
                        armorStand.setInvulnerable(true);
                        armorStand.setSmall(true);
                        armorStand.setMarker(true);
                        armorStand.setCollidable(false);
                        Location fireLocation1 = fireLocation.clone();
                        fireLocation1.setDirection(targetLocation.toVector().subtract(fireLocation.toVector()));
                        armorStand.teleport(fireLocation1);
                        armorStand.addScoreboardTag("casted_projectile_source");
                    });
                    (new BukkitRunnable() {
                        @Override
                        public void run() {
                            spawn.remove();
                        }
                    }).runTaskLater(RPGItems.plugin, 1);
                    source = spawn;
                }
                fire(player, source, stack, roundedConeInfo, speedFactor);
            }

        }

        private void fire(Player player, LivingEntity source, ItemStack stack, RoundedConeInfo roundedConeInfo, float speedFactor) {
            Vector direction1 = source.getEyeLocation().getDirection();
            Vector v = CastUtils.makeCone(source.getEyeLocation(), direction1, roundedConeInfo);
            Events.registerRPGProjectile(getPower().getItem(), stack, player, source);
            org.bukkit.entity.Projectile projectile = source.launchProjectile(getProjectileType(), v.clone().normalize().multiply(getSpeed() * speedFactor));
            if (projectile instanceof AbstractArrow) {
                ((AbstractArrow) projectile).setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                projectile.addScoreboardTag("rgi_projectile");
            }
            projectile.setShooter(player);
            handleProjectile(v, projectile);
        }

        @SuppressWarnings("deprecation")
        private void handleProjectile(Vector v, org.bukkit.entity.Projectile projectile) {
            projectile.setPersistent(false);
            projectile.setGravity(isGravity());
            if (projectile instanceof Explosive) {
                if (getYield() != null) {
                    ((Explosive) projectile).setYield(getYield().floatValue());
                }
                if (getIncendiary() != null) {
                    ((Explosive) projectile).setIsIncendiary(getIncendiary());
                }
            }
            if (projectile instanceof Fireball && isSetFireballDirection()) {
                ((Fireball) projectile).setDirection(v.clone().normalize().multiply(getSpeed()));
            }
            if (Arrow.class.isAssignableFrom(getProjectileType())) {
                Events.autoRemoveProjectile(projectile.getEntityId());
                ((Arrow) projectile).setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            }
            if (!isGravity()) {
                (new BukkitRunnable() {
                    @Override
                    public void run() {
                        projectile.remove();
                    }
                }).runTaskLater(RPGItems.plugin, 80);
            }
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack).with(damage);
        }

        @Override
        public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, Double value) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            CastUtils.CastLocation castLocation = null;
            if (getFiringLocation().equals(FiringLocation.TARGET)) {
                castLocation = CastUtils.rayTrace(player, player.getEyeLocation(), player.getEyeLocation().getDirection(), getFiringRange());
            }
            fire(player, stack, castLocation, 1);
            UUID uuid = player.getUniqueId();
            if (getBurstCount() > 1) {
                Integer prev = burstTask.getIfPresent(uuid);
                if (prev != null) {
                    Bukkit.getScheduler().cancelTask(prev);
                }
                CastUtils.CastLocation finalCastLocation = castLocation;
                BukkitTask bukkitTask = (new BukkitRunnable() {
                    int count = getBurstCount() - 1;

                    @Override
                    public void run() {
                        if (player.getInventory().getItemInMainHand().equals(stack)) {
                            CastUtils.CastLocation castLocation1 = finalCastLocation;
                            if (!isCastOff()){
                                castLocation1 = CastUtils.rayTrace(player, player.getEyeLocation(), player.getEyeLocation().getDirection(), getFiringRange());
                            }
                            burstTask.put(uuid, this.getTaskId());
                            if (count-- > 0) {
                                fire(player, stack, castLocation1, 1);
                                return;
                            }
                        }
                        burstTask.invalidate(uuid);
                        this.cancel();
                    }
                }).runTaskTimer(RPGItems.plugin, getBurstInterval(), getBurstInterval());
                burstTask.put(uuid, bukkitTask.getTaskId());
            }
            return PowerResult.ok();
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
            if (isSuppressArrow()) {
                e.setCancelled(true);
            }
            return fire(player, itemStack, isApplyForce() ? e.getForce() : 1).with(isSuppressArrow() ? -1 : e.getForce());
        }
    }
}
