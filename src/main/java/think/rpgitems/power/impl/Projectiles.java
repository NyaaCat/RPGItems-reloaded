package think.rpgitems.power.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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

/**
 * Power projectile.
 *
 * <p>Launches projectile of type {@link #projectileType} with {@link #gravity} when right clicked.
 * If use {@link #isCone} mode, {@link #amount} of projectiles will randomly distributed in the cone
 * with angle {@link #range} centered with player's direction.
 */
@SuppressWarnings("WeakerAccess")
@Meta(
        defaultTrigger = "RIGHT_CLICK",
        generalInterface = {
            PowerLeftClick.class,
            PowerRightClick.class,
            PowerPlain.class,
            PowerSneak.class,
            PowerLivingEntity.class,
            PowerSprint.class,
            PowerHurt.class,
            PowerHit.class,
            PowerHitTaken.class,
            PowerBowShoot.class,
            PowerBeamHit.class,
            PowerLocation.class
        },
        implClass = Projectiles.Impl.class)
public class Projectiles extends BasePower {
    /** Z_axis. */
    private static final Vector z_axis = new Vector(0, 0, 1);
    /** X_axis. */
    private static final Vector x_axis = new Vector(1, 0, 0);
    /** Y_axis. */
    private static final Vector y_axis = new Vector(0, 1, 0);

    @Property(order = 1)
    public boolean isCone = false;

    @Property public boolean gravity = true;

    @Property(order = 3)
    public int range = 15;

    @Property(order = 4)
    public int amount = 5;

    @Property(order = 5)
    public double speed = 1;

    @Property public int burstCount = 1;
    @Property public int burstInterval = 1;
    @Property public boolean setFireballDirection = false;
    @Property public Double yield = 0d;
    @Property public Boolean isIncendiary = false;

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

    @Property public boolean suppressArrow = false;
    @Property public boolean applyForce = false;
    @Property public boolean requireHurtByEntity = true;

    @Property public FiringLocation firingLocation = FiringLocation.SELF;

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

    @Property public double initialRotation = 0;

    @Property public double firingRange = 64;

    @Property public boolean castOff = false;

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
        SELF,
        TARGET;
    }

    private final Cache<UUID, Integer> burstTask =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .concurrencyLevel(2)
                    .build();

    public Cache<UUID, Integer> getBurstTask() {
        return burstTask;
    }

    @Override
    public void init(ConfigurationSection section) {
        super.init(section);
        if (getYield() != null && getYield() == -1) {
            yield = null;
        }
    }

    public Double getYield() {
        return yield;
    }

    /** Amount of projectiles */
    public int getAmount() {
        return amount;
    }

    /** Burst count of one shoot */
    public int getBurstCount() {
        return burstCount;
    }

    /** Interval between bursts */
    public int getBurstInterval() {
        return burstInterval;
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
        return I18n.formatDefault(
                isCone() ? "power.projectile.cone" : "power.projectile.display",
                getProjectileType(getProjectileType()),
                0);
    }

    /** Whether launch projectiles in cone */
    public boolean isCone() {
        return isCone;
    }

    public static String getProjectileType(Class<? extends Projectile> projectileType) {
        if (projectileType == WitherSkull.class) return "skull";
        else if (projectileType == Fireball.class) return "fireball";
        else if (projectileType == SmallFireball.class) return "smallfireball";
        else if (projectileType == Arrow.class) return "arrow";
        else if (projectileType == LlamaSpit.class) return "llamaspit";
        else if (projectileType == ShulkerBullet.class) return "shulkerbullet";
        else if (projectileType == DragonFireball.class) return "dragonfireball";
        else if (projectileType == Trident.class) return "trident";
        else return "snowball";
    }

    /** Type of projectiles */
    public Class<? extends Projectile> getProjectileType() {
        return projectileType;
    }

    /** Range will projectiles spread, in degree */
    public int getRange() {
        return range;
    }

    /** Speed of projectiles */
    public double getSpeed() {
        return speed;
    }

    public boolean isApplyForce() {
        return applyForce;
    }

    /** Whether the projectile have gravity */
    public boolean isGravity() {
        return gravity;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    /** Whether to set Fireball' direction so it won't curve */
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
            Class<? extends org.bukkit.entity.Projectile> projectileType =
                    (Class<? extends org.bukkit.entity.Projectile>) pt;
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

    public static class Impl
            implements PowerRightClick<Projectiles>,
                    PowerLeftClick<Projectiles>,
                    PowerSneak<Projectiles>,
                    PowerSprint<Projectiles>,
                    PowerHitTaken<Projectiles>,
                    PowerHit<Projectiles>,
                    PowerLivingEntity<Projectiles>,
                    PowerPlain<Projectiles>,
                    PowerBowShoot<Projectiles>,
                    PowerHurt<Projectiles> {

        @Override
        public PowerResult<Double> takeHit(
                Projectiles power,
                Player target,
                ItemStack stack,
                double damage,
                EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Projectiles power, Player player, ItemStack stack) {
            return fire(power, player, stack, 1);
        }

        public PowerResult<Void> fire(
                Projectiles power, Player player, ItemStack stack, float speedFactor) {
            CastUtils.CastLocation castLocation = null;
            if (power.getFiringLocation().equals(FiringLocation.TARGET)) {
                castLocation =
                        CastUtils.rayTrace(
                                player,
                                player.getEyeLocation(),
                                player.getEyeLocation().getDirection(),
                                power.getFiringRange());
            }
            fire(power, player, player, stack, speedFactor, castLocation);
            UUID uuid = player.getUniqueId();
            Cache<UUID, Integer> burstTask = power.getBurstTask();
            if (power.getBurstCount() > 1) {
                Integer prev = burstTask.getIfPresent(uuid);
                if (prev != null) {
                    Bukkit.getScheduler().cancelTask(prev);
                }
                CastUtils.CastLocation finalCastLocation = castLocation;
                BukkitTask bukkitTask =
                        (new BukkitRunnable() {
                                    int count = power.getBurstCount() - 1;

                                    @Override
                                    public void run() {
                                        if (player.getInventory()
                                                .getItemInMainHand()
                                                .equals(stack)) {
                                            CastUtils.CastLocation castLocation1 =
                                                    finalCastLocation;
                                            if (!power.isCastOff()) {
                                                castLocation1 =
                                                        CastUtils.rayTrace(
                                                                player,
                                                                player.getEyeLocation(),
                                                                player.getEyeLocation()
                                                                        .getDirection(),
                                                                power.getFiringRange());
                                            }
                                            burstTask.put(uuid, this.getTaskId());
                                            if (count-- > 0 && player.isOnline()) {
                                                fire(
                                                        power,
                                                        player,
                                                        player,
                                                        stack,
                                                        speedFactor,
                                                        castLocation1);
                                                return;
                                            }
                                        }
                                        burstTask.invalidate(uuid);
                                        this.cancel();
                                    }
                                })
                                .runTaskTimer(
                                        RPGItems.plugin,
                                        power.getBurstInterval(),
                                        power.getBurstInterval());
                burstTask.put(uuid, bukkitTask.getTaskId());
            }
            return PowerResult.ok();
        }

        @Override
        public Class<? extends Projectiles> getPowerClass() {
            return Projectiles.class;
        }

        private RoundedConeInfo generateConeInfo(
                double cone,
                RangedDoubleValue firingR,
                RangedDoubleValue firingTheta,
                RangedDoubleValue firingPhi,
                double initialRotation) {
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

        private void fire(
                Projectiles power,
                Player player,
                LivingEntity source,
                ItemStack stack,
                float speedFactor,
                CastUtils.CastLocation castLocation) {

            for (int i = 0; i < (power.isCone() ? power.getAmount() : 1); i++) {
                RoundedConeInfo roundedConeInfo =
                        generateConeInfo(
                                power.isCone() ? power.getRange() : 0,
                                power.getFiringR(),
                                power.getFiringTheta(),
                                power.getFiringPhi(),
                                power.getInitialRotation());
                if (power.getFiringLocation().equals(FiringLocation.TARGET)
                        && castLocation != null) {
                    Location targetLocation = castLocation.getTargetLocation();
                    Location fireLocation =
                            CastUtils.parseFiringLocation(
                                    targetLocation,
                                    y_axis,
                                    player.getEyeLocation(),
                                    roundedConeInfo);
                    World world = player.getWorld();
                    ArmorStand spawn =
                            world.spawn(
                                    fireLocation,
                                    ArmorStand.class,
                                    armorStand -> {
                                        armorStand.setVisible(false);
                                        armorStand.setInvulnerable(true);
                                        armorStand.setSmall(true);
                                        armorStand.setMarker(true);
                                        armorStand.setCollidable(false);
                                        Location fireLocation1 = fireLocation.clone();
                                        fireLocation1.setDirection(
                                                targetLocation
                                                        .toVector()
                                                        .subtract(fireLocation.toVector()));
                                        armorStand.setRotation(
                                                fireLocation1.getYaw(), fireLocation1.getPitch());
                                        armorStand.addScoreboardTag("casted_projectile_source");
                                    });
                    (new BukkitRunnable() {
                                @Override
                                public void run() {
                                    spawn.remove();
                                }
                            })
                            .runTaskLater(RPGItems.plugin, 1);
                    source = spawn;
                }
                fire(power, player, source, stack, roundedConeInfo, speedFactor);
            }
        }

        private void fire(
                Projectiles power,
                Player player,
                LivingEntity source,
                ItemStack stack,
                RoundedConeInfo roundedConeInfo,
                float speedFactor) {
            Vector direction1 = source.getEyeLocation().getDirection();
            Vector v = CastUtils.makeCone(source.getEyeLocation(), direction1, roundedConeInfo);
            Events.registerRPGProjectile(power.getItem(), stack, player, source);
            org.bukkit.entity.Projectile projectile =
                    source.launchProjectile(
                            power.getProjectileType(),
                            v.clone().normalize().multiply(power.getSpeed() * speedFactor));
            if (projectile instanceof AbstractArrow) {
                ((AbstractArrow) projectile).setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                projectile.addScoreboardTag("rgi_projectile");
            }
            projectile.setShooter(player);
            handleProjectile(power, v, projectile);
        }

        @SuppressWarnings("deprecation")
        private void handleProjectile(
                Projectiles power, Vector v, org.bukkit.entity.Projectile projectile) {
            projectile.setPersistent(false);
            projectile.setGravity(power.isGravity());
            if (projectile instanceof Explosive) {
                if (power.getYield() != null) {
                    ((Explosive) projectile).setYield(power.getYield().floatValue());
                }
                if (power.getIncendiary() != null) {
                    ((Explosive) projectile).setIsIncendiary(power.getIncendiary());
                }
            }
            if (projectile instanceof Fireball && power.isSetFireballDirection()) {
                ((Fireball) projectile)
                        .setDirection(v.clone().normalize().multiply(power.getSpeed()));
            }
            if (Arrow.class.isAssignableFrom(power.getProjectileType())) {
                Events.autoRemoveProjectile(projectile.getEntityId());
                ((Arrow) projectile).setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            }
            if (!power.isGravity()) {
                (new BukkitRunnable() {
                            @Override
                            public void run() {
                                projectile.remove();
                            }
                        })
                        .runTaskLater(RPGItems.plugin, 80);
            }
        }

        @Override
        public PowerResult<Void> hurt(
                Projectiles power, Player target, ItemStack stack, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> rightClick(
                Projectiles power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> leftClick(
                Projectiles power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Double> hit(
                Projectiles power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                EntityDamageByEntityEvent event) {
            return fire(power, player, stack).with(damage);
        }

        @Override
        public PowerResult<Void> sneak(
                Projectiles power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sprint(
                Projectiles power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(
                Projectiles power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                Double value) {
            CastUtils.CastLocation castLocation = null;
            if (power.getFiringLocation().equals(FiringLocation.TARGET)) {
                castLocation =
                        CastUtils.rayTrace(
                                entity,
                                entity.getEyeLocation(),
                                entity.getEyeLocation().getDirection(),
                                power.getFiringRange());
            }
            fire(power, player, entity, stack, 1, castLocation);
            UUID uuid = player.getUniqueId();
            Cache<UUID, Integer> burstTask = power.getBurstTask();
            if (power.getBurstCount() > 1) {
                Integer prev = burstTask.getIfPresent(uuid);
                if (prev != null) {
                    Bukkit.getScheduler().cancelTask(prev);
                }
                CastUtils.CastLocation finalCastLocation = castLocation;
                BukkitTask bukkitTask =
                        (new BukkitRunnable() {
                                    int count = power.getBurstCount() - 1;

                                    @Override
                                    public void run() {
                                        if (player.getInventory()
                                                .getItemInMainHand()
                                                .equals(stack)) {
                                            CastUtils.CastLocation castLocation1 =
                                                    finalCastLocation;
                                            if (!power.isCastOff()) {
                                                castLocation1 =
                                                        CastUtils.rayTrace(
                                                                entity,
                                                                entity.getEyeLocation(),
                                                                entity.getEyeLocation()
                                                                        .getDirection(),
                                                                power.getFiringRange());
                                            }
                                            burstTask.put(uuid, this.getTaskId());
                                            if (count-- > 0) {
                                                fire(
                                                        power,
                                                        player,
                                                        entity,
                                                        stack,
                                                        1,
                                                        finalCastLocation);
                                                return;
                                            }
                                        }
                                        burstTask.invalidate(uuid);
                                        this.cancel();
                                    }
                                })
                                .runTaskTimer(
                                        RPGItems.plugin,
                                        power.getBurstInterval(),
                                        power.getBurstInterval());
                burstTask.put(uuid, bukkitTask.getTaskId());
            }
            return PowerResult.ok();
        }

        @Override
        public PowerResult<Float> bowShoot(
                Projectiles power, Player player, ItemStack itemStack, EntityShootBowEvent e) {
            if (power.isSuppressArrow()) {
                e.setCancelled(true);
            }
            return fire(power, player, itemStack, power.isApplyForce() ? e.getForce() : 1)
                    .with(power.isSuppressArrow() ? -1 : e.getForce());
        }
    }
}
