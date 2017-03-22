package think.rpgitems.power.types;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Triggers when hit some LivingEntity
 */
public interface PowerHit extends Power {
    /**
     * Calls when {@code player} using {@code item} hits an {@code entity} with {@code damage}
     *
     * @param player Player
     * @param item   ItemStack of this RPGItem
     * @param entity LivingEntity being hit
     * @param damage Damage of this event
     */
    void hit(Player player, ItemStack item, LivingEntity entity, double damage);
}
