package think.rpgitems.power;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Triggers when right click
 */
public interface PowerRightClick extends Power {
    /**
     * Calls when {@code player} using {@code stack} right clicks {@code clicked}
     *
     * @param player  Player
     * @param stack   Item that triggered this power
     * @param clicked Block clicked
     */
    void rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event);
}
