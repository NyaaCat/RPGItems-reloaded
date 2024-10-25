package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when player mends an item by enchantment
 */
public interface PowerMending extends Pimpl {
    /**
     * Calls when {@code player} using {@code stack} and mending
     *
     * @param player Player
     * @param stack  Item that triggered this power
     * @param event  Event that triggered this power
     * @return PowerResult with proposed damage
     */
    @CheckReturnValue
    PowerResult<Void> mending(Player player, ItemStack stack, PlayerItemMendEvent event);
}
