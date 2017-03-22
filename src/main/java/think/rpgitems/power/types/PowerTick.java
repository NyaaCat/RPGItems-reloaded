package think.rpgitems.power.types;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Triggers per tick
 */
public interface PowerTick extends Power {
    /**
     * Calls per tick with {@code player} using {@code item}
     *
     * @param player  Player
     * @param item    Item that triggered this power
     */
    void tick(Player player, ItemStack item);
}
