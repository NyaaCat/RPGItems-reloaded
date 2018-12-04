package think.rpgitems.power.impl;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@PowerMeta(marker = true)
public class PowerDurabilityCondition extends BasePower implements PowerCondition<Void> {

    @Property(order = 0, required = true)
    public String id;

    @Property
    public boolean isStatic = false;

    @Property
    public boolean isCritical = false;

    @Property
    public int durabilityMin = Integer.MIN_VALUE;

    @Property
    public int durabilityMax = Integer.MAX_VALUE;

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
    public PowerResult<Void> check(Player player, ItemStack stack, Map<Power, PowerResult> context) {
        int durability = getItem().getDurability(stack);
        if ((durabilityMax > 0 && durability > durabilityMax) || durability < durabilityMin) return PowerResult.fail();
        return PowerResult.ok();
    }

    @Override
    public String getName() {
        return "durabilitycondition";
    }

    @Override
    public String displayText() {
        return null;
    }

    @Override
    public Set<String> getConditions() {
        return Collections.emptySet();
    }
}
