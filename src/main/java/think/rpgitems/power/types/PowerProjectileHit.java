package think.rpgitems.power.types;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;

public interface PowerProjectileHit extends Power {
    public void projectileHit(Player player, ItemStack i, Projectile arrow);
}
