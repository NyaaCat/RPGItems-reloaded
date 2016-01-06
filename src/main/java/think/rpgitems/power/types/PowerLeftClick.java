package think.rpgitems.power.types;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public interface PowerLeftClick extends Power {
    public void leftClick(Player player, Block clicked);
}
