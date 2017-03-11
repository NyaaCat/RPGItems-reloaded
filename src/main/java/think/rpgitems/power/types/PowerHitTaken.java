package think.rpgitems.power.types;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public interface PowerHitTaken extends Power {
    /**
     * @param target player been hit
     * @param i      item that triggered this power
     * @param ev     damage event
     * @return new damage value, if nothing change, return a negative number.
     */
    double takeHit(Player target, ItemStack i, EntityDamageEvent ev);
}
