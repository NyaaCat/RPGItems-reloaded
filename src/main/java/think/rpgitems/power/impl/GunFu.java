package think.rpgitems.power.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Context;
import think.rpgitems.power.*;

import java.util.List;
import java.util.stream.Collectors;

import static think.rpgitems.power.PowerResult.noop;
import static think.rpgitems.power.PowerResult.ok;
import static think.rpgitems.power.Utils.*;

/**
 * Power shulker bullet.
 * <p>
 * Launches shulker bullet when right clicked.
 * Target nearest entity
 * </p>
 */
@Meta(immutableTrigger = true, withSelectors = true, implClass = GunFu.Impl.class)
public class GunFu extends BasePower {

    @Property(order = 1)
    public double distance = 20;
    @Property
    public double viewAngle = 30;

    @Property
    public double initVelFactor = 0.5;

    @Property
    public double velFactor = 0.05;

    @Property
    public double forceFactor = 1.5;

    @Property
    public int maxTicks = 200;

    @Property
    public int delay = 0;

    @Override
    public void init(ConfigurationSection s) {
        super.init(s);
    }

    public int getDelay() {
        return delay;
    }

    /**
     * Range of target finding
     */
    public double getDistance() {
        return distance;
    }

    public double getForceFactor() {
        return forceFactor;
    }

    public double getInitVelFactor() {
        return initVelFactor;
    }

    public int getMaxTicks() {
        return maxTicks;
    }

    @Override
    public String getName() {
        return "gunfu";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.gunfu", (0) / 20d);
    }

    public double getVelFactor() {
        return velFactor;
    }

    /**
     * View angle of target finding
     */
    public double getViewAngle() {
        return viewAngle;
    }

    public static class Impl implements PowerProjectileLaunch<GunFu>, PowerBowShoot<GunFu> {

        @Override
        public PowerResult<Float> bowShoot(GunFu power, Player player, ItemStack stack, EntityShootBowEvent event) {
            if (event.getProjectile() instanceof Projectile) {
                return run(power, player, (Projectile) event.getProjectile(), event.getForce());
            }
            return noop();
        }

        @Override
        public Class<? extends GunFu> getPowerClass() {
            return GunFu.class;
        }

        public PowerResult<Float> run(GunFu power, Player player, Projectile projectile, float force) {
            List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(power, player.getEyeLocation(), player, power.getDistance(), 0), player.getLocation().toVector(), power.getViewAngle(), player.getLocation().getDirection()).stream().filter(player::hasLineOfSight).collect(Collectors.toList());
            projectile.setVelocity(projectile.getVelocity().multiply(power.getInitVelFactor()));
            if (!entities.isEmpty()) {
                LivingEntity target = entities.get(0);
                Context.instance().putExpiringSeconds(player.getUniqueId(), "gunfu.target", target, 3);
                new BukkitRunnable() {

                    private int ticks = power.getMaxTicks();

                    @Override
                    public void run() {
                        if (!target.isValid() || projectile.isDead() || !projectile.isValid() || ticks-- <= 0) {
                            cancel();
                            return;
                        }
                        Vector origVel = projectile.getVelocity();
                        double v = origVel.length();
                        Vector rel = target.getEyeLocation().toVector().subtract(projectile.getLocation().toVector()).normalize().multiply(v);
                        double velFac = power.getVelFactor() * (power.getForceFactor() - force);
                        rel.multiply(velFac).add(origVel.multiply(1 - velFac));
                        projectile.setVelocity(rel);
                        if (projectile instanceof Fireball) {
                            ((Fireball) projectile).setDirection(rel.normalize());
                        }
                    }
                }.runTaskTimer(RPGItems.plugin, power.getDelay(), 0);
            }
            return ok(force * (float) power.getInitVelFactor());
        }

        @Override
        public PowerResult<Void> projectileLaunch(GunFu power, Player player, ItemStack stack, ProjectileLaunchEvent event) {
            Projectile projectile = event.getEntity();
            return run(power, player, projectile, 1).with(null);
        }
    }
}
