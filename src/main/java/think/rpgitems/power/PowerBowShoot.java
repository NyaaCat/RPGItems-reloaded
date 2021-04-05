package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

public interface PowerBowShoot<P extends Power> extends Pimpl<P> {

    @CheckReturnValue
    PowerResult<Float> bowShoot(P power, Player player, ItemStack stack, EntityShootBowEvent event);
}
