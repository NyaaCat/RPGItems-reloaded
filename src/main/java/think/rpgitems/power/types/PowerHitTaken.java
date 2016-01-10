package think.rpgitems.power.types;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface PowerHitTaken extends Power {
    /**
     * @param target player been hit
     * @param damager who made the damage
     * @param damage damage value
     * @return new damage value, if nothing change, return a negative number.
     */
    double takeHit(Player target, Entity damager, double damage);
}
