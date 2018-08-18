package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when being hit
 */
public interface PowerHitTaken extends Power {
    /**
     * Calls when {@code target} using {@code stack} being hit in {@code event}
     *
     * @param target Player been hit
     * @param stack  Item that triggered this power
     * @param event  Damage event
     * @return New damage value, if nothing change, return a negative number.
     */
    @CheckReturnValue
    PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event);
}
