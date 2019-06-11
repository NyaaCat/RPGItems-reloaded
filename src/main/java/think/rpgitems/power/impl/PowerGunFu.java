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
@PowerMeta(immutableTrigger = true, withSelectors = true)
public class PowerGunFu extends BasePower implements PowerProjectileLaunch, PowerBowShoot {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;
    /**
     * Range of target finding
     */
    @Property(order = 1)
    public double distance = 20;
    /**
     * View angle of target finding
     */
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
        cooldown = s.getLong("cooldownTime");
        super.init(s);
    }

    @Override
    public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
        if (!checkCooldown(this, player, cooldown, true, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        if (event.getProjectile() instanceof Projectile) {
            return run(player, (Projectile) event.getProjectile(), event.getForce());
        }
        return noop();
    }

    @Override
    public String getName() {
        return "gunfu";
    }

    @Override
    public String displayText() {
        return I18n.format("power.gunfu", (double) cooldown / 20d);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PowerResult<Void> projectileLaunch(Player player, ItemStack stack, ProjectileLaunchEvent event) {
        if (!checkCooldown(this, player, cooldown, true, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        Projectile projectile = event.getEntity();
        return run(player, projectile, 1).with(null);
    }

    public PowerResult<Float> run(Player player, Projectile projectile, float force) {
        List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(this, player.getEyeLocation(), player, distance, 0), player.getLocation().toVector(), viewAngle, player.getLocation().getDirection()).stream().filter(player::hasLineOfSight).collect(Collectors.toList());
        projectile.setVelocity(projectile.getVelocity().multiply(initVelFactor));
        if (!entities.isEmpty()) {
            LivingEntity target = entities.get(0);
            Context.instance().putExpiringSeconds(player.getUniqueId(), "gunfu.target", target, 3);
            new BukkitRunnable() {

                private int ticks = maxTicks;

                @Override
                public void run() {
                    if (!target.isValid() || projectile.isDead() || !projectile.isValid() || ticks-- <= 0) {
                        cancel();
                        return;
                    }
                    Vector origVel = projectile.getVelocity();
                    double v = origVel.length();
                    Vector rel = target.getEyeLocation().toVector().subtract(projectile.getLocation().toVector()).normalize().multiply(v);
                    double velFac = velFactor * (forceFactor - force);
                    rel.multiply(velFac).add(origVel.multiply(1 - velFac));
                    projectile.setVelocity(rel);
                    if (projectile instanceof Fireball) {
                        ((Fireball) projectile).setDirection(rel.normalize());
                    }
                }
            }.runTaskTimer(RPGItems.plugin, delay, 0);
        }
        return ok(force * (float) initVelFactor);
    }
}
