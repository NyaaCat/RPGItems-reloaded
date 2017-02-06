package think.rpgitems.power.types;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PowerTick extends Power {
    public void tick(Player player, ItemStack i);
}
