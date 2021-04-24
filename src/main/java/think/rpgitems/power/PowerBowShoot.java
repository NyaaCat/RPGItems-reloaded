package think.rpgitems.power;

import javax.annotation.CheckReturnValue;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

public interface PowerBowShoot<P extends Power> extends Pimpl<P> {

    @CheckReturnValue
    PowerResult<Float> bowShoot(P power, Player player, ItemStack stack, EntityShootBowEvent event);
}
