package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface Condition<T> extends PropertyHolder {

    String id();

    boolean isStatic();

    boolean isCritical();

    PowerResult<T> check(Player player, ItemStack stack, Map<PropertyHolder, PowerResult> context);
}
