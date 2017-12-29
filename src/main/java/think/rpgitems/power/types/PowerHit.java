package think.rpgitems.power.types;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Triggers when hit some LivingEntity
 */
public interface PowerHit extends Power,PowerDelayable {
    /**
     * Calls when {@code player} using {@code stack} hits an {@code entity} with {@code damage}
     *
     * @param player Player
     * @param stack  ItemStack of this RPGItem
     * @param entity LivingEntity being hit
     * @param damage Damage of this event
     */
    void hit(Player player, ItemStack stack, LivingEntity entity, double damage);
}
