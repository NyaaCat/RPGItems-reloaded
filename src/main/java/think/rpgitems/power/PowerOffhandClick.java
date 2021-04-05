package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when player clicks an item in offhand
 */
public interface PowerOffhandClick<P extends Power> extends Pimpl<P> {
    /**
     * Calls when {@code player} using {@code stack} in offhand clicks
     *
     * @param player Player
     * @param stack  Item that triggered this power
     * @param event  Event that triggered this power
     * @return PowerResult with proposed damage
     */
    @CheckReturnValue
    PowerResult<Void> offhandClick(P power, Player player, ItemStack stack, PlayerInteractEvent event);
}