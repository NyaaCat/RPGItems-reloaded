package think.rpgitems.power;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.papermc.paper.event.entity.EntityLandEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when player lands
 */
public interface PowerLand extends Pimpl {
    /**
     * Calls when {@code player} using {@code stack} lands
     *
     * @param player Player
     * @param stack  Item that triggered this power
     * @param event  Event that triggered this power
     * @return PowerResult
     */
    @CheckReturnValue
    PowerResult<Void> land(Player player, ItemStack stack, EntityLandEvent event);
}