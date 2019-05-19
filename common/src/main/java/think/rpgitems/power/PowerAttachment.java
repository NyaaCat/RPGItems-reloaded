package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.RPGItem;

import javax.annotation.CheckReturnValue;

public interface PowerAttachment extends Power {
    @CheckReturnValue
    PowerResult<Void> attachment(Player player, ItemStack stack, RPGItem originItem, Event originEvent, ItemStack originStack);
}
