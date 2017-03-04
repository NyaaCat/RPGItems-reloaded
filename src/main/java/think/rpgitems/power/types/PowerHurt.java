package think.rpgitems.power.types;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public interface PowerHurt extends Power {
    /**
     * @param target player been hit
     * @param i item that triggered this power
     * @param ev damage event
     * @return new damage value, if nothing change, return a negative number.
     */
    void hurt(Player target, ItemStack i, EntityDamageEvent ev);
}
