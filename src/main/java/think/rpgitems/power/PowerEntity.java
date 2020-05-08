package think.rpgitems.power;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public interface PowerEntity extends Pimpl {
    PowerResult<Void> fire(Player player, ItemStack stack, Entity entity, @Nullable Double value);
}
