package think.rpgitems.power.cond;

import cat.nyaa.nyaacore.Pair;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;
import think.rpgitems.power.impl.BasePower;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Meta(marker = true, withConditions = true)
public class OrCondition extends BasePower implements Condition<Map.Entry<PropertyHolder, PowerResult>> {

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

    @SuppressWarnings("unchecked")
    @Override
    public PowerResult<Map.Entry<PropertyHolder, PowerResult>> check(Player player, ItemStack stack, Map<PropertyHolder, PowerResult> context) {
        Set<String> conditions = new HashSet<>(getConditions());
        for (Map.Entry<PropertyHolder, PowerResult> entry : context.entrySet()) {
            PropertyHolder power = entry.getKey();
            if (power instanceof Condition && conditions.contains(((Condition) power).id())) {
                conditions.remove(((Condition) power).id());
                if (entry.getValue().isOK()) return PowerResult.ok(entry);
            }
        }
        if (!isStatic) {
            List<Condition> powerConditions = getItem().getConditions();
            for (Condition condition : powerConditions) {
                if (!conditions.contains(condition.id())) continue;
                assert !condition.isStatic();
                PowerResult result = condition.check(player, stack, context);
                if (result.isOK()) return PowerResult.ok(Pair.of(condition, result));
            }
        }
        return PowerResult.fail();
    }

    @Override
    public String getName() {
        return "orcondition";
    }

    @Override
    public String displayText() {
        return null;
    }
}
