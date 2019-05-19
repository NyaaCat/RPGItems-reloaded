package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

/**
 * Triggers when RPG Projectile hits
 */
public interface PowerProjectileHit extends Power {
    /**
     * Calls when {@code player} using {@code stack} has launched a projectile {@code arrow} and it hit something
     *
     * @param player Player
     * @param stack  Item that triggered this power
     * @param event  Event that triggered this power
     *
     * @return PowerResult
     */
    @CheckReturnValue
    PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event);
}
