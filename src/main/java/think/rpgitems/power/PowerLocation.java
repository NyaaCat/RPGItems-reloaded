package think.rpgitems.power;

import javax.annotation.CheckReturnValue;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PowerLocation<P extends Power> extends Pimpl<P> {

    /**
     * A trigger that fire a power with an location
     *
     * @param player Player
     * @param stack Item that triggered this power
     * @param location Location that involved in this trigger
     * @return PowerResult
     */
    @CheckReturnValue
    PowerResult<Void> fire(P power, Player player, ItemStack stack, Location location);
}
