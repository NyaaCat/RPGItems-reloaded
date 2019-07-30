package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface PowerCondition<T> extends Power,Pimpl {

    String id();

    boolean isStatic();

    boolean isCritical();

    PowerResult<T> check(Player player, ItemStack stack, Map<Power, PowerResult> context);

    @Override
    default Set<Trigger> getTriggers() {
        return Collections.emptySet();
    }
}
