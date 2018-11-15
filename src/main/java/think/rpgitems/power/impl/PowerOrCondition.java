package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Pair;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@PowerMeta(marker = true, withConditions = true)
public class PowerOrCondition extends BasePower implements PowerCondition<Map.Entry<Power, PowerResult>> {

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
    public PowerResult<Map.Entry<Power, PowerResult>> check(Player player, ItemStack stack, Map<Power, PowerResult> context) {
        Set<String> conditions = new HashSet<>(getConditions());
        for (Map.Entry<Power, PowerResult> entry : context.entrySet()) {
            Power power = entry.getKey();
            if (power instanceof PowerCondition && conditions.contains(((PowerCondition) power).id())) {
                conditions.remove(((PowerCondition) power).id());
                if (entry.getValue().isOK()) return PowerResult.ok().with(entry);
            }
        }
        if (!isStatic) {
            List<PowerCondition> powerConditions = getItem().getPower(PowerCondition.class, true);
            for (PowerCondition powerCondition : powerConditions) {
                if (!conditions.contains(powerCondition.id())) continue;
                assert !powerCondition.isStatic();
                PowerResult result = powerCondition.check(player, stack, context);
                if (result.isOK()) return PowerResult.ok().with(Pair.of(powerCondition, result));
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
