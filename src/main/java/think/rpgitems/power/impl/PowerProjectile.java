package think.rpgitems.power.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
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
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = PowerProjectile.Impl.class)
public class PowerProjectile extends BasePower {
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

    private Cache<UUID, Integer> burstTask = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).concurrencyLevel(2).build();

    @Property(order = 0)
    private long cooldown = 0;
    @Property(order = 1)
    private boolean isCone = false;
    @Property
    private boolean gravity = true;
    @Property(order = 3)
    private int range = 15;
    @Property(order = 4)
    private int amount = 5;
    @Property(order = 5)
    private double speed = 1;
    @Property
    private int cost = 0;
    @Property
    private int burstCount = 1;
    @Property
    private int burstInterval = 1;
    @Property
    private boolean setFireballDirection = false;

    @Property
    private Double yield = null;

    @Property
    private Boolean isIncendiary = null;
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
    private Class<? extends Projectile> projectileType = Snowball.class;

    @Property
    private boolean suppressArrow = false;

    @Property
    private boolean applyForce = false;
    @Property
    private boolean requireHurtByEntity = true;

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
        return I18n.format(isCone() ? "power.projectile.cone" : "power.projectile.display", getProjectileType(getProjectileType()), (double) getCooldown() / 20d);
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
    public long getCooldown() {
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
            Class<? extends Projectile> projectileType = (Class<? extends Projectile>) pt;
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
            fire(player, stack, player.getEyeLocation().getDirection(), speedFactor);
            UUID uuid = player.getUniqueId();
            if (getBurstCount() > 1) {
                Integer prev = burstTask.getIfPresent(uuid);
                if (prev != null) {
                    Bukkit.getScheduler().cancelTask(prev);
                }
                BukkitTask bukkitTask = (new BukkitRunnable() {
                    int count = getBurstCount() - 1;

                    @Override
                    public void run() {
                        if (player.getInventory().getItemInMainHand().equals(stack)) {
                            burstTask.put(uuid, this.getTaskId());
                            if (count-- > 0) {
                                fire(player, stack, player.getEyeLocation().getDirection(), speedFactor);
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
            return PowerProjectile.this;
        }

        private void fire(Player player, ItemStack stack, Vector direction, float speedFactor) {
            if (!isCone()) {
                Vector v = direction.multiply(getSpeed() * speedFactor);
                Events.registerRPGProjectile(getPower().getItem(), stack, player);
                Projectile projectile = player.launchProjectile(getProjectileType(), v);
                handleProjectile(v, projectile);
            } else {
                range = Math.abs(getRange()) % 360;
                double phi = getRange() / 180f * Math.PI;
                Vector a, b;
                Vector ax1 = direction.getCrossProduct(z_axis);
                if (ax1.length() < 0.01) {
                    a = x_axis.clone();
                    b = y_axis.clone();
                } else {
                    a = ax1.normalize();
                    b = direction.getCrossProduct(a).normalize();
                }
                for (int i = 0; i < getAmount(); i++) {
                    double z = getRange() == 0 ? 1 : ThreadLocalRandom.current().nextDouble(Math.cos(phi), 1);
                    double det = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
                    double theta = Math.acos(z);
                    Vector v = a.clone().multiply(Math.cos(det)).add(b.clone().multiply(Math.sin(det))).multiply(Math.sin(theta)).add(direction.clone().multiply(Math.cos(theta)));
                    Events.registerRPGProjectile(getPower().getItem(), stack, player);
                    Projectile projectile = player.launchProjectile(getProjectileType(), v.normalize().multiply(getSpeed() * speedFactor));
                    handleProjectile(v, projectile);
                }
            }
        }

        @SuppressWarnings("deprecation")
        private void handleProjectile(Vector v, Projectile projectile) {
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
            Vector direction = player.getEyeLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
            fire(player, stack, direction, 1);
            UUID uuid = player.getUniqueId();
            if (getBurstCount() > 1) {
                Integer prev = burstTask.getIfPresent(uuid);
                if (prev != null) {
                    Bukkit.getScheduler().cancelTask(prev);
                }
                BukkitTask bukkitTask = (new BukkitRunnable() {
                    int count = getBurstCount() - 1;

                    @Override
                    public void run() {
                        if (player.getInventory().getItemInMainHand().equals(stack)) {
                            burstTask.put(uuid, this.getTaskId());
                            if (count-- > 0) {
                                fire(player, stack, direction, 1);
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
