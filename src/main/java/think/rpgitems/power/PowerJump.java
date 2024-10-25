package think.rpgitems.power;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when player sprints
 */
public interface PowerJump extends Pimpl {
    /**
     * Calls when {@code player} using {@code stack} jumps
     *
     * @param player Player
     * @param stack  Item that triggered this power
     * @param event  Event that triggered this power
     * @return PowerResult
     */
    @CheckReturnValue
    PowerResult<Void> jump(Player player, ItemStack stack, PlayerJumpEvent event);
}