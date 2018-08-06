package think.rpgitems.power;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Triggers when left click
 */
public interface PowerLeftClick extends Power {
    /**
     * Calls when {@code player} using {@code stack} left clicks {@code clicked}
     *
     * @param player  Player
     * @param stack   Item that triggered this power
     * @param clicked Block clicked
     */
    void leftClick(Player player, ItemStack stack, Block clicked);
}
