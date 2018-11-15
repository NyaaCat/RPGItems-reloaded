package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when right click
 */
public interface PowerRightClick extends Power {
    /**
     * Calls when {@code player} using {@code stack} right clicks {@code clicked}
     *
     * @param player  Player
     * @param stack   Item that triggered this power
     * @param event   Event that triggered this power
     *
     * @return PowerResult
     */
    @CheckReturnValue
    PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event);
}
