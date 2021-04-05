package think.rpgitems.power;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public interface PowerEntity<P extends Power> extends Pimpl<P> {
    PowerResult<Void> fire(P power, Player player, ItemStack stack, Entity entity, @Nullable Double value);
}
