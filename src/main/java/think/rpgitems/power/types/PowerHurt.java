package think.rpgitems.power.types;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Triggers when being hurt
 */
public interface PowerHurt extends Power {
    /**
     * Calls when {@code target} using {@code item} being hurt in {@code event}
     *
     * @param target Player being hurt
     * @param item   Item that triggered this power
     * @param event  Damage event
     */
    void hurt(Player target, ItemStack item, EntityDamageEvent event);
}
