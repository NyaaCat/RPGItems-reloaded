package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when player sprints
 */
public interface PowerSprint extends Power {
    /**
     * Calls when {@code player} using {@code stack} sprints
     *
     * @param player Player
     * @param stack  Item that triggered this power
     * @param event  Event that triggered this power
     *
     * @return PowerResult
     */
    @CheckReturnValue
    PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event);
}