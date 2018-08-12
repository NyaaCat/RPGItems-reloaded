package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
/**
 * Triggers when player sprints
 */
public interface PowerSprint extends Power {
    /**
     * Calls when {@code player} using {@code stack} sprints
     *
     * @param player  Player
     * @param stack   Item that triggered this power
     */
    void sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event);
}