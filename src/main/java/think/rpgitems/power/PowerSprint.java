package think.rpgitems.power;

import javax.annotation.CheckReturnValue;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;

/** Triggers when player sprints */
public interface PowerSprint<P extends Power> extends Pimpl<P> {
  /**
   * Calls when {@code player} using {@code stack} sprints
   *
   * @param player Player
   * @param stack Item that triggered this power
   * @param event Event that triggered this power
   * @return PowerResult
   */
  @CheckReturnValue
  PowerResult<Void> sprint(P power, Player player, ItemStack stack, PlayerToggleSprintEvent event);
}
