package think.rpgitems.power;

import javax.annotation.CheckReturnValue;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Triggers per tick */
public interface PowerTick<P extends Power> extends Pimpl<P> {
    /**
     * Calls per tick with {@code player} using {@code stack}
     *
     * @param player Player
     * @param stack Item that triggered this power
     * @return PowerResult
     */
    @CheckReturnValue
    PowerResult<Void> tick(P power, Player player, ItemStack stack);
}
