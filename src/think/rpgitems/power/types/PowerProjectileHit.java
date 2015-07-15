package think.rpgitems.power.types;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public interface PowerProjectileHit extends Power {
    public void projectileHit(Player player, Projectile arrow);
}
