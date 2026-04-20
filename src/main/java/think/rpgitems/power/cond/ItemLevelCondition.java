package think.rpgitems.power.cond;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.AcceptedValue;
import think.rpgitems.power.Meta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;
import think.rpgitems.power.PropertyHolder;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Meta(marker = true)
public class ItemLevelCondition extends BaseCondition<Void> {

    @Property(order = 0, required = true)
    public String id;

    @Property
    public boolean isStatic = true;

    @Property
    public boolean isCritical = false;

    @Property(required = true)
    public int level = 1;

    @Property
    @AcceptedValue({"eq", "gt", "egt", "lt", "elt"})
    public String operation = "eq";

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
        int itemLevel = getItem().getItemLevel(stack);
        String op = operation == null ? "eq" : operation.toLowerCase(Locale.ROOT);
        return switch (op) {
            case "eq" -> itemLevel == level ? PowerResult.ok() : PowerResult.fail();
            case "gt" -> itemLevel > level ? PowerResult.ok() : PowerResult.fail();
            case "egt" -> itemLevel >= level ? PowerResult.ok() : PowerResult.fail();
            case "lt" -> itemLevel < level ? PowerResult.ok() : PowerResult.fail();
            case "elt" -> itemLevel <= level ? PowerResult.ok() : PowerResult.fail();
            default -> PowerResult.fail();
        };
    }

    @Override
    public Set<String> getConditions() {
        return Collections.emptySet();
    }

    @Override
    public String getName() {
        return "itemlevelcondition";
    }

    @Override
    public String displayText() {
        return null;
    }
}
