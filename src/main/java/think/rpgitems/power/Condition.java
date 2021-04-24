package think.rpgitems.power;

import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface Condition<T> extends PropertyHolder, TagHolder {

  String id();

  boolean isStatic();

  boolean isCritical();

  PowerResult<T> check(Player player, ItemStack stack, Map<PropertyHolder, PowerResult<?>> context);

  Set<String> getConditions();

  String displayText();
}
