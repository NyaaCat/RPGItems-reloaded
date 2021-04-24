package think.rpgitems.power.cond;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

@Meta(marker = true, withConditions = true)
public class XorCondition extends BaseCondition<Void> {

    @Property(order = 0, required = true)
    public String id;

    @Property public boolean init = false;

    @Property public boolean isStatic = true;

    @Property public boolean isCritical = false;

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
    public PowerResult<Void> check(
            Player player, ItemStack stack, Map<PropertyHolder, PowerResult<?>> context) {
        Set<String> conditions = new HashSet<>(getConditions());
        boolean ans = init;
        for (Map.Entry<PropertyHolder, PowerResult<?>> entry : context.entrySet()) {
            PropertyHolder power = entry.getKey();
            if (power instanceof Condition && conditions.contains(((Condition<?>) power).id())) {
                conditions.remove(((Condition<?>) power).id());
                ans = ans ^ entry.getValue().isOK();
            }
        }
        if (!isStatic) {
            List<Condition<?>> powerConditions = getItem().getConditions();
            for (Condition<?> condition : powerConditions) {
                if (!conditions.contains(condition.id())) continue;
                assert !condition.isStatic();
                PowerResult<?> result = condition.check(player, stack, context);
                ans = ans ^ result.isOK();
            }
        }
        return ans ? PowerResult.ok() : PowerResult.fail();
    }

    @Override
    public String getName() {
        return "xorcondition";
    }

    @Override
    public String displayText() {
        return null;
    }
}
