package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when player is sneaking
 */
public interface PowerSneaking extends Pimpl {
    /**
     * Calls per tick when {@code player} using {@code stack} sneaking
     *
     * @param player Player
     * @param stack  Item that triggered this power
     * @return PowerResult
     */
    @CheckReturnValue
    PowerResult<Void> sneaking(Player player, ItemStack stack);
}
