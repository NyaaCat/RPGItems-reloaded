package think.rpgitems.power.types;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Triggers when right click
 */
public interface PowerRightClick extends Power {
    /**
     * Calls when {@code player} using {@code item} right clicks {@code clicked}
     *
     * @param player  Player
     * @param item    Item that triggered this power
     * @param clicked Block clicked
     */
    void rightClick(Player player, ItemStack item, Block clicked);
}
