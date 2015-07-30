package think.rpgitems.api.power;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Powers triggered when player hit a entity
 */
public interface IPowerHit extends IPower {
    /**
     * Invoked when player hit an entity
     *
     * @param player the player made the hit
     * @param target the entity been hit
     * @param damage the damage value made
     * @return power triggered or not
     */
    Boolean hit(Player player, LivingEntity target, Double damage);
}
