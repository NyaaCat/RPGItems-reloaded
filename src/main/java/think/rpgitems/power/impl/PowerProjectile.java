package think.rpgitems.power.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static think.rpgitems.utils.PowerUtils.checkCooldown;

/**
 * Power projectile.
 * <p>
 * Launches projectile of type {@link #projectileType} with {@link #gravity} when right clicked.
 * If use {@link #isCone} mode, {@link #amount} of projectiles will randomly distributed in the cone
 * with angle {@link #range} centered with player's direction.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(immutableTrigger = true)
public class PowerProjectile extends BasePower implements PowerRightClick {
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

    private static Cache<UUID, Integer> burstCounter = CacheBuilder.newBuilder().expireAfterAccess(3, TimeUnit.SECONDS).concurrencyLevel(2).build();
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
     * burst count of one shoot
     */
    public int burstCount = 1;
    /**
     * Interval between bursts
     */
    public int burstInterval = 1;
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
            "dragonfireball"
    })
    @Deserializer(ProjectileType.class)
    @Serializer(ProjectileType.class)
    @Property(order = 2, required = true)
    public Class<? extends Projectile> projectileType = Snowball.class;

    /**
     * Check if the type is acceptable
     *
     * @param str Type name
     * @return If acceptable
     */
    public boolean acceptableType(String str) {
        return !(isCone && str.equalsIgnoreCase("fireball"));
    }

    @Override
    public String getName() {
        return "projectile";
    }

    @Override
    public String displayText() {
        return I18n.format(isCone ? "power.projectile.cone" : "power.projectile.display", getProjectileType(projectileType), (double) cooldown / 20d);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        fire(player);
        if (burstCount > 1) {
            burstCounter.put(player.getUniqueId(), burstCount - 1);
            (new BukkitRunnable() {
                @Override
                public void run() {
                    Integer i;
                    if (player.getInventory().getItemInMainHand().equals(stack) && (i = burstCounter.getIfPresent(player.getUniqueId())) != null) {
                        if (i > 0) {
                            fire(player);
                            burstCounter.put(player.getUniqueId(), i - 1);
                            return;
                        }
                    }
                    burstCounter.invalidate(player.getUniqueId());
                    this.cancel();
                }
            }).runTaskTimer(RPGItems.plugin, 1, burstInterval);
        }
        return PowerResult.ok();
    }

    @SuppressWarnings("deprecation")
    private void fire(Player player) {
        if (!isCone) {
            Projectile projectile = player.launchProjectile(projectileType, player.getEyeLocation().getDirection().multiply(speed));
            projectile.setPersistent(false);
            Events.rpgProjectiles.put(projectile.getEntityId(), getItem().getUID());
            projectile.setGravity(gravity);
            if (projectileType == Arrow.class) {
                ((Arrow) projectile).setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                Events.removeArrows.add(projectile.getEntityId());
            }
            if (!gravity) {
                (new BukkitRunnable() {
                    @Override
                    public void run() {
                        projectile.remove();
                    }
                }).runTaskLater(RPGItems.plugin, 80);
            }
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
                projectile.setPersistent(false);
                Events.rpgProjectiles.put(projectile.getEntityId(), getItem().getUID());
                projectile.setGravity(gravity);
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
        }
    }

    static class ProjectileType implements Getter, Setter{
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
        public Class<? extends Projectile> set(String type) {
            switch (type) {
                case "skull":
                    return WitherSkull.class;
                case "fireball":
                    return Fireball.class;
                case "smallfireball":
                    return SmallFireball.class;
                case "arrow":
                    return Arrow.class;
                case "llamaspit":
                    return LlamaSpit.class;
                case "shulkerbullet":
                    return ShulkerBullet.class;
                case "dragonfireball":
                    return DragonFireball.class;
                default:
                    return Snowball.class;
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
        else
            return "snowball";
    }
}
