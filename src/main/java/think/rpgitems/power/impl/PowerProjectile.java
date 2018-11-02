package think.rpgitems.power.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
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
@PowerMeta(defaultTrigger = "RIGHT_CLICK")
public class PowerProjectile extends BasePower implements PowerRightClick, PowerLeftClick, PowerPlain {
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

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Whether launch projectiles in cone
     */
    @Property(order = 1)
    public boolean isCone = false;
    /**
     * Whether the projectile have gravity
     */
    @Property
    public boolean gravity = true;
    /**
     * Range will projectiles spread, in degree
     */
    @Property(order = 3)
    public int range = 15;
    /**
     * Amount of projectiles
     */
    @Property(order = 4)
    public int amount = 5;
    /**
     * Speed of projectiles
     */
    @Property(order = 5)
    public double speed = 1;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 1;
    /**
     * Burst count of one shoot
     */
    @Property
    public int burstCount = 1;
    /**
     * Interval between bursts
     */
    @Property
    public int burstInterval = 1;
    /**
     * Whether to set Fireball' direction so it won't curve
     */
    @Property
    public boolean setFireballDirection = false;

    @Property
    public Double yield = null;

    @Property
    public Boolean isIncendiary = null;
    /**
     * Type of projectiles
     */
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
    public Class<? extends Projectile> projectileType = Snowball.class;

    @Override
    public String getName() {
        return "projectile";
    }

    @Override
    public String displayText() {
        return I18n.format(isCone ? "power.projectile.cone" : "power.projectile.display", getProjectileType(projectileType), (double) cooldown / 20d);
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
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        fire(player);
        UUID uuid = player.getUniqueId();
        if (burstCount > 1) {
            Integer prev = burstTask.getIfPresent(uuid);
            if (prev != null) {
                Bukkit.getScheduler().cancelTask(prev);
            }
            BukkitTask bukkitTask = (new BukkitRunnable() {
                int count = burstCount - 1;

                @Override
                public void run() {
                    if (player.getInventory().getItemInMainHand().equals(stack)) {
                        burstTask.put(uuid, this.getTaskId());
                        if (count-- > 0) {
                            fire(player);
                            return;
                        }
                    }
                    burstTask.invalidate(uuid);
                    this.cancel();
                }
            }).runTaskTimer(RPGItems.plugin, burstInterval, burstInterval);
            burstTask.put(uuid, bukkitTask.getTaskId());
        }
        return PowerResult.ok();
    }

    private void fire(Player player) {
        if (!isCone) {
            Vector v = player.getEyeLocation().getDirection().multiply(speed);
            Projectile projectile = player.launchProjectile(projectileType, v);
            handleProjectile(v, projectile);
        } else {
            Vector loc = player.getEyeLocation().getDirection();
            range = Math.abs(range) % 360;
            double phi = range / 180f * Math.PI;
            Vector a, b;
            Vector ax1 = loc.getCrossProduct(z_axis);
            if (ax1.length() < 0.01) {
                a = x_axis.clone();
                b = y_axis.clone();
            } else {
                a = ax1.normalize();
                b = loc.getCrossProduct(a).normalize();
            }
            for (int i = 0; i < amount; i++) {
                double z = range == 0 ? 1 : ThreadLocalRandom.current().nextDouble(Math.cos(phi), 1);
                double det = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
                double theta = Math.acos(z);
                Vector v = a.clone().multiply(Math.cos(det)).add(b.clone().multiply(Math.sin(det))).multiply(Math.sin(theta)).add(loc.clone().multiply(Math.cos(theta)));
                Projectile projectile = player.launchProjectile(projectileType, v.normalize().multiply(speed));
                handleProjectile(v, projectile);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void handleProjectile(Vector v, Projectile projectile) {
        projectile.setPersistent(false);
        Events.rpgProjectiles.put(projectile.getEntityId(), getItem().getUID());
        projectile.setGravity(gravity);
        if (projectile instanceof Explosive) {
            if (yield != null) {
                ((Explosive) projectile).setYield(yield.floatValue());
            }
            if (isIncendiary != null) {
                ((Explosive) projectile).setIsIncendiary(isIncendiary);
            }
        }
        if (projectile instanceof Fireball && setFireballDirection) {
            ((Fireball) projectile).setDirection(v.clone().normalize().multiply(speed));
        }
        if (projectileType == Arrow.class) {
            Events.removeArrows.add(projectile.getEntityId());
            ((Arrow) projectile).setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        }
        if (!gravity) {
            (new BukkitRunnable() {
                @Override
                public void run() {
                    projectile.remove();
                }
            }).runTaskLater(RPGItems.plugin, 80);
        }
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

    @Override
    public void init(ConfigurationSection section) {
        super.init(section);
        if (yield != null && yield == -1) {
            yield = null;
        }
    }
}
