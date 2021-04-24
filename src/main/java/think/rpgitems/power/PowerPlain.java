package think.rpgitems.power;

import javax.annotation.CheckReturnValue;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Plain triggers for powers that do not requires a event */
public interface PowerPlain<P extends Power> extends Pimpl<P> {
  /**
   * Simply trigger this power
   *
   * @param player Player
   * @param stack Item that triggered this power
   * @return PowerResult
   */
  @CheckReturnValue
  PowerResult<Void> fire(P power, Player player, ItemStack stack);
}
