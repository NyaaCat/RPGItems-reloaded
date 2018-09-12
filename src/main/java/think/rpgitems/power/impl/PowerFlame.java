package think.rpgitems.power.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.PowerHit;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;

/**
 * Power flame.
 * <p>
 * The flame power will set the target on fire on hit in {@link #burnTime} ticks.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(immutableTrigger = true)
public class PowerFlame extends BasePower implements PowerHit {

    /**
     * Duration of the fire, in ticks
     */
    @Property(order = 0)
    public int burnTime = 20;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        entity.setFireTicks(burnTime);
        return PowerResult.ok(damage);
    }

    @Override
    public String displayText() {
        return I18n.format("power.flame", (double) burnTime / 20d);
    }

    @Override
    public String getName() {
        return "flame";
    }
}
