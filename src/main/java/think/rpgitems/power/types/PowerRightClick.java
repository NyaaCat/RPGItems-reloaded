package think.rpgitems.power.types;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Triggers when right click
 */
public interface PowerRightClick extends IPower {
    /**
     * Calls when {@code player} using {@code stack} right clicks {@code clicked}
     *
     * @param player  Player
     * @param stack   Item that triggered this power
     * @param clicked Block clicked
     */
    void rightClick(Player player, ItemStack stack, Block clicked);
}
