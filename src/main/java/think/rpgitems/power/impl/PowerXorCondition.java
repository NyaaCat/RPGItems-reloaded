package think.rpgitems.power.impl;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@PowerMeta(marker = true, withConditions = true, implClass = PowerXorCondition.class)
public class PowerXorCondition extends BasePower implements PowerCondition<Void> {

    @Property(order = 0, required = true)
    public String id;

    @Property
    public boolean init = false;

    @Property
    public boolean isStatic = true;

    @Property
    public boolean isCritical = false;

    @Override
    public String id() {
        return id;
    }

    @Override
    public Power getPower() {
        return this;
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
    public PowerResult<Void> check(Player player, ItemStack stack, Map<Power, PowerResult> context) {
        Set<String> conditions = new HashSet<>(getConditions());
        boolean ans = init;
        for (Map.Entry<Power, PowerResult> entry : context.entrySet()) {
            Power power = entry.getKey();
            if (power instanceof PowerCondition && conditions.contains(((PowerCondition) power).id())) {
                conditions.remove(((PowerCondition) power).id());
                ans = ans ^ entry.getValue().isOK();
            }
        }
        if (!isStatic) {
            List<PowerCondition> powerConditions = getItem().getPower(PowerCondition.class, true);
            for (PowerCondition powerCondition : powerConditions) {
                if (!conditions.contains(powerCondition.id())) continue;
                assert !powerCondition.isStatic();
                PowerResult result = powerCondition.check(player, stack, context);
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
