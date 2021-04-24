package think.rpgitems.power;

import javax.annotation.CheckReturnValue;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

/** Triggers when RPG Projectile hits */
public interface PowerProjectileHit<P extends Power> extends Pimpl<P> {
    /**
     * Calls when {@code player} using {@code stack} has launched a projectile {@code arrow} and it
     * hit something
     *
     * @param player Player
     * @param stack Item that triggered this power
     * @param event Event that triggered this power
     * @return PowerResult
     */
    @CheckReturnValue
    PowerResult<Void> projectileHit(
            P power, Player player, ItemStack stack, ProjectileHitEvent event);
}
