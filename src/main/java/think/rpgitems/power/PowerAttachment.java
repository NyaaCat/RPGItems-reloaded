package think.rpgitems.power;

import javax.annotation.CheckReturnValue;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.RPGItem;

public interface PowerAttachment<P extends Power> extends Pimpl<P> {
  @CheckReturnValue
  PowerResult<Void> attachment(
      P power,
      Player player,
      ItemStack stack,
      RPGItem originItem,
      Event originEvent,
      ItemStack originStack);
}
