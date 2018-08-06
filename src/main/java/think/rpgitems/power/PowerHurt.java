package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

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
    void hurt(Player target, ItemStack stack, EntityDamageEvent event);
}
