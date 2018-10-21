package think.rpgitems.power.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import java.util.Random;

/**
 * Power lightning.
 * <p>
 * The lightning power will strike the hit target with lightning with a chance of 1/{@link #chance}.
 * </p>
 */
@PowerMeta(defaultTrigger = {"HIT", "PROJECTILE_HIT"})
public class PowerLightning extends BasePower implements PowerHit, PowerProjectileHit {
    /**
     * Chance of triggering this power
     */
    @Property(order = 0)
    public int chance = 20;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    private Random random = new Random();

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (random.nextInt(chance) == 0) {
            if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
            entity.getWorld().strikeLightning(entity.getLocation());
            return PowerResult.ok(damage);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
        if (random.nextInt(chance) == 0) {
            if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
            Projectile hit = event.getEntity();
            hit.getWorld().strikeLightning(hit.getLocation());
            return PowerResult.ok();
        }
        return PowerResult.noop();
    }

    @Override
    public String displayText() {
        return I18n.format("power.lightning", (int) ((1d / (double) chance) * 100d));
    }

    @Override
    public String getName() {
        return "lightning";
    }
}
