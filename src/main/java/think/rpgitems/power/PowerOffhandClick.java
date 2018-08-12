package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Triggers when player clicks an item in offhand
 */
public interface PowerOffhandClick extends Power {
    /**
     * Calls when {@code player} using {@code stack} in offhand clicks
     *
     * @param player Player
     * @param stack  Item that triggered this power
     */
    void offhandClick(Player player, ItemStack stack, PlayerInteractEvent event);
}