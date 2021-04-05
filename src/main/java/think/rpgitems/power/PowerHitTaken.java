package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when being hit
 */
public interface PowerHitTaken<P extends Power> extends Pimpl<P> {
    /**
     * Calls when {@code target} using {@code stack} being hit in {@code event}
     *
     * @param target Player been hit
     * @param stack  Item that triggered this power
     * @param damage Damage of this event
     * @param event  Event that triggered this power
     * @return PowerResult with proposed damage
     */
    @CheckReturnValue
    PowerResult<Double> takeHit(P power, Player target, ItemStack stack, double damage, EntityDamageEvent event);
}
