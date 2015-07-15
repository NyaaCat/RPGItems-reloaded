package think.rpgitems.power.types;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface PowerHit extends Power {
    public void hit(Player player, LivingEntity e, double damage);
}
