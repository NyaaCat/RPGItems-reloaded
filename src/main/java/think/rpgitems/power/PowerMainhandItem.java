package think.rpgitems.power;

import javax.annotation.CheckReturnValue;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/** Triggers when player swap mainhand item to offhand */
public interface PowerMainhandItem<P extends Power> extends Pimpl<P> {

  /**
   * Calls when {@code player} swap mainhand item to offhand
   *
   * @param player Player
   * @param stack Item that triggered this power
   * @param event Event that triggered this power
   * @return PowerResult with proposed event continuation
   */
  @CheckReturnValue
  PowerResult<Boolean> swapToOffhand(
      P power, Player player, ItemStack stack, PlayerSwapHandItemsEvent event);

  /**
   * Calls when {@code player} place item to offhand in inventory
   *
   * @param player Player
   * @param stack Item that triggered this power
   * @param event Event that triggered this power
   * @return PowerResult with proposed event continuation
   */
  @CheckReturnValue
  default PowerResult<Boolean> placeOffhand(
      P power, Player player, ItemStack stack, InventoryClickEvent event) {
    return PowerResult.noop();
  }
}
