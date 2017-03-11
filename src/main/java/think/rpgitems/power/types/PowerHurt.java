package think.rpgitems.power.types;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public interface PowerHurt extends Power {
    /**
     * @param target player been hit
     * @param i      item that triggered this power
     * @param ev     damage event
     */
    void hurt(Player target, ItemStack i, EntityDamageEvent ev);
}
