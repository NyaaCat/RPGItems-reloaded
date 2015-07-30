package think.rpgitems.api.power;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface IPowerBeenHit extends IPower {
    /**
     * Invoked when player hit.
     *
     * @param player player been hit
     * @param enemy  livingEntity made the hit
     * @param damage damage value made
     * @return power triggered or not
     */
    Boolean onHit(Player player, LivingEntity enemy, Double damage);
}
