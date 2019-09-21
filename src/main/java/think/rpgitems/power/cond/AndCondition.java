package think.rpgitems.power.cond;

import cat.nyaa.nyaacore.Pair;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Meta(marker = true, withConditions = true)
public class AndCondition extends BaseCondition<Map.Entry<Condition<?>, PowerResult<?>>> {

    @Property(order = 0, required = true)
    public String id;

    @Property
    public boolean isStatic = true;

    @Property
    public boolean isCritical = false;

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public boolean isCritical() {
        return isCritical;
    }

    @Override
    public PowerResult<Map.Entry<Condition<?>, PowerResult<?>>> check(Player player, ItemStack stack, Map<PropertyHolder, PowerResult<?>> context) {
        Set<String> conditions = new HashSet<>(getConditions());
        for (Map.Entry<PropertyHolder, PowerResult<?>> entry : context.entrySet()) {
            PropertyHolder cond = entry.getKey();
            if (cond instanceof Condition && conditions.contains(((Condition<?>) cond).id())) {
                conditions.remove(((Condition<?>) cond).id());
                if (!entry.getValue().isOK()) return PowerResult.fail(Pair.of((Condition<?>) cond, entry.getValue()));
            }
        }
        if (!isStatic) {
            List<Condition<?>> powerConditions = getItem().getConditions();
            for (Condition<?> condition : powerConditions) {
                if (!conditions.contains(condition.id())) continue;
                if (condition.isStatic()) throw new IllegalStateException();
                PowerResult<?> result = condition.check(player, stack, context);
                if (!result.isOK()) return PowerResult.fail(Pair.of(condition, result));
            }
        }
        return PowerResult.ok(null);
    }

    @Override
    public String getName() {
        return "andcondition";
    }
}
