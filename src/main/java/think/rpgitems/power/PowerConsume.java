package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

public interface PowerConsume extends Pimpl {
    @CheckReturnValue
    PowerResult<Void> consume(Player player, ItemStack stack, PlayerItemConsumeEvent event);
}
