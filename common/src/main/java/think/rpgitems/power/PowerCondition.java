package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public interface PowerCondition<T> extends Power {

    String id();

    boolean isStatic();

    boolean isCritical();

    PowerResult<T> check(Player player, ItemStack stack, Map<Power, PowerResult> context);
}
