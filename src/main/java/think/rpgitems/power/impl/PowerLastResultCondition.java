package think.rpgitems.power.impl;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@PowerMeta(marker = true, implClass = PowerLastResultCondition.class)
public class PowerLastResultCondition extends BasePower implements PowerCondition<Map.Entry<Power, PowerResult>> {

    @Property(order = 0, required = true)
    public String id;

    @Property
    public boolean isCritical = false;

    @Property
    public Set<TriggerResult> okResults = Collections.singleton(TriggerResult.OK);

    @Property
    public boolean failOnNoResult = true;

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isCritical() {
        return isCritical;
    }

    @Override
    public PowerResult<Map.Entry<Power, PowerResult>> check(Player player, ItemStack stack, Map<Power, PowerResult> context) {
        Optional<Map.Entry<Power, PowerResult>> last = context.entrySet().stream().skip(context.size() - 1).findFirst();
        return last
                       .map(r -> okResults.contains(r.getValue().result()) ? PowerResult.ok() : PowerResult.fail())
                       .orElse(failOnNoResult ? PowerResult.fail() : PowerResult.ok())
                       .with(last.orElse(null));
    }

    @Override
    public Set<String> getConditions() {
        return Collections.emptySet();
    }

    @Override
    public String getName() {
        return "lastresultcondition";
    }

    @Override
    public String displayText() {
        return null;
    }

    @Override
    public Power getPower() {
        return this;
    }
}
