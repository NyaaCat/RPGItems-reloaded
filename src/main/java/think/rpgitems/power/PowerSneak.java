package think.rpgitems.power;

import javax.annotation.CheckReturnValue;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

/** Triggers when player sneaks */
public interface PowerSneak<P extends Power> extends Pimpl<P> {
  /**
   * Calls when {@code player} using {@code stack} sneaks
   *
   * @param player Player
   * @param stack Item that triggered this power
   * @param event Event that triggered this power
   * @return PowerResult
   */
  @CheckReturnValue
  PowerResult<Void> sneak(P power, Player player, ItemStack stack, PlayerToggleSneakEvent event);
}
