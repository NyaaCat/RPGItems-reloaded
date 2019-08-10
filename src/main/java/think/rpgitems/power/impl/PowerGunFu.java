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
@PowerMeta(immutableTrigger = true, withSelectors = true, implClass = PowerGunFu.Impl.class)
public class PowerGunFu extends BasePower {

    @Property(order = 0, alias = "cooldownTime")
    private long cooldown = 0;
    @Property
    private int cost = 0;
    @Property(order = 1)
    private double distance = 20;
    @Property
    private double viewAngle = 30;

    @Property
    private double initVelFactor = 0.5;

    @Property
    private double velFactor = 0.05;

    @Property
    private double forceFactor = 1.5;

    @Property
    private int maxTicks = 200;

    @Property
    private int delay = 0;

    @Override
    public void init(ConfigurationSection s) {
        super.init(s);
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
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
        return I18n.format("power.gunfu", (double) getCooldown() / 20d);
    }

    /**
     * Cooldown time of this power
     */
    public long getCooldown() {
        return cooldown;
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

    public class Impl implements PowerProjectileLaunch, PowerBowShoot {

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            if (event.getProjectile() instanceof Projectile) {
                return run(player, (Projectile) event.getProjectile(), event.getForce());
            }
            return noop();
        }

        @Override
        public Power getPower() {
            return PowerGunFu.this;
        }

        public PowerResult<Float> run(Player player, Projectile projectile, float force) {
            List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(getPower(), player.getEyeLocation(), player, getDistance(), 0), player.getLocation().toVector(), getViewAngle(), player.getLocation().getDirection()).stream().filter(player::hasLineOfSight).collect(Collectors.toList());
            projectile.setVelocity(projectile.getVelocity().multiply(getInitVelFactor()));
            if (!entities.isEmpty()) {
                LivingEntity target = entities.get(0);
                Context.instance().putExpiringSeconds(player.getUniqueId(), "gunfu.target", target, 3);
                new BukkitRunnable() {

                    private int ticks = getMaxTicks();

                    @Override
                    public void run() {
                        if (!target.isValid() || projectile.isDead() || !projectile.isValid() || ticks-- <= 0) {
                            cancel();
                            return;
                        }
                        Vector origVel = projectile.getVelocity();
                        double v = origVel.length();
                        Vector rel = target.getEyeLocation().toVector().subtract(projectile.getLocation().toVector()).normalize().multiply(v);
                        double velFac = getVelFactor() * (getForceFactor() - force);
                        rel.multiply(velFac).add(origVel.multiply(1 - velFac));
                        projectile.setVelocity(rel);
                        if (projectile instanceof Fireball) {
                            ((Fireball) projectile).setDirection(rel.normalize());
                        }
                    }
                }.runTaskTimer(RPGItems.plugin, getDelay(), 0);
            }
            return ok(force * (float) getInitVelFactor());
        }

        @Override
        @SuppressWarnings("deprecation")
        public PowerResult<Void> projectileLaunch(Player player, ItemStack stack, ProjectileLaunchEvent event) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            Projectile projectile = event.getEntity();
            return run(player, projectile, 1).with(null);
        }
    }
}
