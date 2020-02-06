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
@Meta(defaultTrigger = "HIT", generalInterface = PowerLivingEntity.class, implClass = Flame.Impl.class)
public class Flame extends BasePower {

    @Property(order = 0)
    public int burntime = 20;
    @Property
    public int cost = 0;

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

    @Override
    public String displayText() {
        return I18n.formatDefault("power.flame", (double) getBurntime() / 20d);
    }

    /**
     * Duration of the fire, in ticks
     */
    public int getBurntime() {
        return burntime;
    }

    public class Impl implements PowerHit, PowerLivingEntity {

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, Double value) {
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            entity.setFireTicks(getBurntime());
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return Flame.this;
        }
    }
}
