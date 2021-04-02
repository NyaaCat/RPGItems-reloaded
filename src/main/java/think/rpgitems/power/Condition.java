package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;

public interface Condition<T> extends PropertyHolder, TagHolder {

    String id();

    boolean isStatic();

    boolean isCritical();

    PowerResult<T> check(Player player, ItemStack stack, Map<PropertyHolder, PowerResult<?>> context);

    Set<String> getConditions();

    String displayText();
}
