package think.rpgitems.power.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

/**
 * Power flame.
 * <p>
 * The flame power will set the target on fire on hit in {@link #burntime} ticks.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(immutableTrigger = true, generalInterface = PowerLivingEntity.class)
public class PowerFlame extends BasePower implements PowerHit, PowerLivingEntity {

    /**
     * Duration of the fire, in ticks
     */
    @Property(order = 0)
    public int burntime = 20;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        return fire(player, stack, entity, damage).with(damage);
    }

    @Override
    public String displayText() {
        return I18n.format("power.flame", (double) burntime / 20d);
    }

    @Override
    public String getName() {
        return "flame";
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, double value) {
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        entity.setFireTicks(burntime);
        return PowerResult.ok();
    }
}
