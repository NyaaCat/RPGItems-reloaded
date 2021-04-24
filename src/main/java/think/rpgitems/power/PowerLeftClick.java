package think.rpgitems.power;

import javax.annotation.CheckReturnValue;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/** Triggers when left click */
public interface PowerLeftClick<P extends Power> extends Pimpl<P> {
  /**
   * Calls when {@code player} using {@code stack} left clicks {@code clicked}
   *
   * @param player Player
   * @param stack Item that triggered this power
   * @param event Event that triggered this power
   * @return PowerResult
   */
  @CheckReturnValue
  PowerResult<Void> leftClick(P power, Player player, ItemStack stack, PlayerInteractEvent event);
}
