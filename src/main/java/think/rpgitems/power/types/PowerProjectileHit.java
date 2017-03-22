package think.rpgitems.power.types;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;

/**
 * Triggers when RPG Projectile hits
 */
public interface PowerProjectileHit extends Power {
    /**
     * Calls when {@code player} using {@code item} has launched a projectile {@code arrow} and it hit something
     *
     * @param player  Player
     * @param item    Item that triggered this power
     * @param arrow   Projectile
     */
    void projectileHit(Player player, ItemStack item, Projectile arrow);
}
