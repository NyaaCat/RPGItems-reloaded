package think.rpgitems.power;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

public interface PowerLocation extends Pimpl {

    /**
     * A trigger that fire a power with an location
     *
     * @param player   Player
     * @param stack    Item that triggered this power
     * @param location Location that involved in this trigger
     * @return PowerResult
     */
    @CheckReturnValue
    PowerResult<Void> fire(Player player, ItemStack stack, Location location);
}
