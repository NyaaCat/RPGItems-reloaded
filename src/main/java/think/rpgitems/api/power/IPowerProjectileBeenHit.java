package think.rpgitems.api.power;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public interface IPowerProjectileBeenHit extends IPower {
    /**
     * Invoked when player hit.
     *
     * @param player     player been hit
     * @param enemy      livingEntity made the hit
     * @param damage     damage value made
     * @param projectile projectile hit the player
     * @return power triggered or not
     */
    Boolean onHit(Player player, LivingEntity enemy, Double damage, Projectile projectile);
}
