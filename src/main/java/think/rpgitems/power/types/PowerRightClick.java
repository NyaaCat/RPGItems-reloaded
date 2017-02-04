package think.rpgitems.power.types;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PowerRightClick extends Power {
    public void rightClick(Player player, ItemStack i, Block clicked);
}
