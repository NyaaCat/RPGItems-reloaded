package think.rpgitems.power.types;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PowerLeftClick extends Power {
    public void leftClick(Player player, ItemStack i, Block clicked);
}
