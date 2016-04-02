package think.rpgitems.power.types;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public interface PowerRightClick extends Power {
    public void rightClick(Player player, Block clicked);
}
