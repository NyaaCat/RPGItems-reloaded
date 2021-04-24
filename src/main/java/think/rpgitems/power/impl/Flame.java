package think.rpgitems.power.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

/**
 * Power flame.
 *
 * <p>The flame power will set the target on fire on hit in {@link #burntime} ticks.
 */
@SuppressWarnings("WeakerAccess")
@Meta(
        defaultTrigger = "HIT",
        generalInterface = PowerLivingEntity.class,
        implClass = Flame.Impl.class)
public class Flame extends BasePower {

    @Property(order = 0)
    public int burntime = 20;

    @Override
    public String getName() {
        return "flame";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.flame", (double) getBurntime() / 20d);
    }

    /** Duration of the fire, in ticks */
    public int getBurntime() {
        return burntime;
    }

    public static class Impl implements PowerHit<Flame>, PowerLivingEntity<Flame> {

        @Override
        public PowerResult<Double> hit(
                Flame power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                EntityDamageByEntityEvent event) {
            return fire(power, player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Void> fire(
                Flame power, Player player, ItemStack stack, LivingEntity entity, Double value) {
            entity.setFireTicks(power.getBurntime());
            return PowerResult.ok();
        }

        @Override
        public Class<? extends Flame> getPowerClass() {
            return Flame.class;
        }
    }
}
