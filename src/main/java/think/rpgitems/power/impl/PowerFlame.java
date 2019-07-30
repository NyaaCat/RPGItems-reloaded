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
@PowerMeta(immutableTrigger = true, generalInterface = PowerLivingEntity.class, implClass = PowerFlame.Impl.class)
public class PowerFlame extends BasePower {

    @Property(order = 0)
    private int burntime = 20;
    @Property
    private int cost = 0;

    public class Impl implements PowerHit, PowerLivingEntity {

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, Double value) {
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            entity.setFireTicks(getBurntime());
            return PowerResult.ok();
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }


        @Override
        public Power getPower() {
            return PowerFlame.this;
        }
    }


    @Override
    public String displayText() {
        return I18n.format("power.flame", (double) getBurntime() / 20d);
    }

    /**
     * Duration of the fire, in ticks
     */
    public int getBurntime() {
        return burntime;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    @Override
    public String getName() {
        return "flame";
    }
}
