package think.rpgitems.api.power;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

/**
 * Powers triggered when player hit a entity
 */
public interface IPowerProjectileHit extends IPower {
    /**
     * Invoked when player hit an entity use projectile
     *
     * @param player     the player made the hit
     * @param target     the entity been hit
     * @param damage     the damage value made
     * @param projectile the projectile
     * @return power triggered or not
     */
    Boolean projectileHit(Player player, LivingEntity target, Double damage, Projectile projectile);
}
