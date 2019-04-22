package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.CheckReturnValue;

public interface PowerBowShoot extends Power {

    @CheckReturnValue
    PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event);
}
