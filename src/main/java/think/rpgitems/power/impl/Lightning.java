package think.rpgitems.power.impl;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.data.Context;
import think.rpgitems.power.*;

import java.util.Random;

/**
 * Power lightning.
 * <p>
 * The lightning power will strike the hit target with lightning with a chance of 1/{@link #chance}.
 * </p>
 */
@Meta(defaultTrigger = {"HIT", "PROJECTILE_HIT"}, generalInterface = PowerLocation.class, implClass = Lightning.Impl.class)
public class Lightning extends BasePower {
    @Property(order = 0)
    public int chance = 20;

    private Random random = new Random();

    @Override
    public String getName() {
        return "lightning";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.lightning", (int) ((1d / (double) getChance()) * 100d));
    }

    /**
     * Chance of triggering this power
     */
    public int getChance() {
        return chance;
    }

    public Random getRandom() {
        return random;
    }

    public static class Impl implements PowerHit<Lightning>, PowerProjectileHit<Lightning>, PowerLocation<Lightning> {

        @Override
        public PowerResult<Double> hit(Lightning power, Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            Location location = entity.getLocation();
            return fire(power, player, stack, location).with(damage);
        }

        @Override
        public PowerResult<Void> fire(Lightning power, Player player, ItemStack stack, Location location) {
            if (power.getRandom().nextInt(power.getChance()) == 0) {
                location.getWorld().strikeLightning(location);
                Context.instance().putExpiringSeconds(player.getUniqueId(), "lightning.location", location, 3);
                return PowerResult.ok();
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> projectileHit(Lightning power, Player player, ItemStack stack, ProjectileHitEvent event) {
            Projectile hit = event.getEntity();
            Location location = hit.getLocation();
            return fire(power, player, stack, location);
        }

        @Override
        public Class<? extends Lightning> getPowerClass() {
            return Lightning.class;
        }
    }
}
