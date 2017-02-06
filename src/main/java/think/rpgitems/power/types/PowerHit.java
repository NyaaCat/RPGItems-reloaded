package think.rpgitems.power.types;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PowerHit extends Power {
    public void hit(Player player, ItemStack i, LivingEntity e, double damage);
}
