package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class RepairCostHandler implements ComponentHandler<Integer> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.REPAIR_COST;
    }

    @Override
    public String yamlKey() {
        return "repair_cost";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        if (!parent.isInt(yamlKey())) {
            return null;
        }
        int cost = parent.getInt(yamlKey());
        if (cost < 0) {
            throw new IllegalArgumentException("Repair cost must be greater than or equal to 0"
                    + (rpgItem == null ? "" : ":" + rpgItem.getName()));
        }
        return cost;
    }

    @Override
    public void encode(Integer value, ConfigurationSection config) {
        config.set(yamlKey(), value);
    }
}
