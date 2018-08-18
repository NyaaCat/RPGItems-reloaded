package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when being hurt
 */
public interface PowerHurt extends Power {
    /**
     * Calls when {@code target} using {@code stack} being hurt in {@code event}
     *
     * @param target Player being hurt
     * @param stack  Item that triggered this power
     * @param event  Damage event
     */
    @CheckReturnValue
    PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event);
}
