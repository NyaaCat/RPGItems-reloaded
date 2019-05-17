package cat.nyaa.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityLoadCrossbowEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerResult;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when loading crossbow
 */
public interface PowerCrossbowLoad extends Power {
    /**
     * Calls when {@code target} loading crossbow with {@code arrow}
     *
     * @param target Player been hit
     * @param stack  Item that triggered this power
     * @param arrow  Arrow of itemstack
     * @param event  Event that triggered this power
     *
     * @return PowerResult with proposed damage
     */
    @CheckReturnValue
    PowerResult<Void> crossbowLoad(Player target, ItemStack stack, ItemStack arrow, EntityLoadCrossbowEvent event);
}