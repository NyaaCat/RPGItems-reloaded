package think.rpgitems.power.types;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Triggers per tick
 */
public interface PowerTick extends IPower {
    /**
     * Calls per tick with {@code player} using {@code stack}
     *
     * @param player Player
     * @param stack  Item that triggered this power
     */
    void tick(Player player, ItemStack stack);
}
