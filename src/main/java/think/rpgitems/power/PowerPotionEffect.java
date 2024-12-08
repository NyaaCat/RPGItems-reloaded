package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when player get effected
 */
public interface PowerPotionEffect extends Pimpl {
    /**
     * Calls when {@code player} using {@code stack} and effected
     *
     * @param player Player
     * @param stack  Item that triggered this power
     * @param event  Event that triggered this power
     * @return PowerResult with proposed damage
     */
    @CheckReturnValue
    PowerResult<Void> potionEffect(Player player, ItemStack stack, EntityPotionEffectEvent event);
}
