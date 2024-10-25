package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when player's exp changes
 */
public interface PowerExpChange extends Pimpl {
    /**
     * Calls when {@code player} using {@code stack} and changes exp
     *
     * @param player Player
     * @param stack  Item that triggered this power
     * @param event  Event that triggered this power
     * @return PowerResult with proposed damage
     */
    @CheckReturnValue
    PowerResult<Void> expChange(Player player, ItemStack stack, PlayerExpChangeEvent event);
}
