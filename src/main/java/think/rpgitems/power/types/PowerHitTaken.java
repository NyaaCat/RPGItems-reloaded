package think.rpgitems.power.types;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface PowerHitTaken extends Power {
    public void takeHit(Player target, LivingEntity damager, double damage);
}
