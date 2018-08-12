package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Triggers when player sneaks
 */
public interface PowerSneak extends Power {
    /**
     * Calls when {@code player} using {@code stack} sneaks
     *
     * @param player  Player
     * @param stack   Item that triggered this power
     */
    void sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event);
}
