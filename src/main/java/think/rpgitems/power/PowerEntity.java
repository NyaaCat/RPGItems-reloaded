package think.rpgitems.power;

import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PowerEntity<P extends Power> extends Pimpl<P> {
    PowerResult<Void> fire(
            P power, Player player, ItemStack stack, Entity entity, @Nullable Double value);
}
