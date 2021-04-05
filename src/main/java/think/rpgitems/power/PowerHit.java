package think.rpgitems.power;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when hit some LivingEntity
 */
public interface PowerHit<P extends Power> extends Pimpl<P> {
    /**
     * Calls when {@code player} using {@code stack} hits an {@code entity} with {@code damage}
     *
     * @param player Player
     * @param stack  ItemStack of this RPGItem
     * @param entity LivingEntity being hit
     * @param damage Damage of this event
     * @param event  Event that triggered this power
     * @return PowerResult with proposed damage
     */
    @CheckReturnValue
    PowerResult<Double> hit(P power, Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event);
}
