package think.rpgitems.power;

import javax.annotation.CheckReturnValue;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

/** Triggers when being hurt */
public interface PowerHurt<P extends Power> extends Pimpl<P> {
  /**
   * Calls when {@code target} using {@code stack} being hurt in {@code event}
   *
   * @param target Player being hurt
   * @param stack Item that triggered this power
   * @param event Event that triggered this power
   * @return PowerResult
   */
  @CheckReturnValue
  PowerResult<Void> hurt(P power, Player target, ItemStack stack, EntityDamageEvent event);
}
