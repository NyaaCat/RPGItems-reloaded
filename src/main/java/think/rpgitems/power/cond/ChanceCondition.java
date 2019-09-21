package think.rpgitems.power.cond;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Meta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;
import think.rpgitems.power.PropertyHolder;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Meta(marker = true)
public class ChanceCondition extends BaseCondition<Void> {

    @Property(order = 0, required = true)
    public String id;

    @Property
    public boolean isStatic = false;

    @Property
    public boolean isCritical = false;

    @Property
    public double chancePercentage;

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
    public PowerResult<Void> check(Player player, ItemStack stack, Map<PropertyHolder, PowerResult<?>> context) {
        if (ThreadLocalRandom.current().nextDouble(0, 100) > chancePercentage) return PowerResult.fail();
        return PowerResult.ok();
    }

    @Override
    public Set<String> getConditions() {
        return Collections.emptySet();
    }

    @Override
    public String getName() {
        return "chancecondition";
    }

    @Override
    public String displayText() {
        return null;
    }
}
