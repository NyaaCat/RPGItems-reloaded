package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when player swap offhand item to main hand
 */
public interface PowerSwapToOffhand extends Power {

    /**
     * Calls when {@code player} swap mainhand item to offhand
     *
     * @param player Player
     * @param stack  Item that triggered this power
     */
    @CheckReturnValue
    PowerResult<Boolean> swapToOffhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event);

    /**
     * Calls when {@code player} place item to offhand in inventory
     *
     * @param player Player
     * @param stack  Item that triggered this power
     */
    @CheckReturnValue
    default PowerResult<Boolean> placeOffhand(Player player, ItemStack stack, InventoryClickEvent event) {
        return PowerResult.noop();
    }
}