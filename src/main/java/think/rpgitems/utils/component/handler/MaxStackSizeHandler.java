package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class MaxStackSizeHandler implements ComponentHandler<Integer> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.MAX_STACK_SIZE;
    }

    @Override
    public String yamlKey() {
        return "max_stack_size";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        if (!parent.isInt(yamlKey())) {
            return null;
        }
        if (parent.getInt(yamlKey(), 1) > 1 && parent.isInt("max_damage")) {
            throw new IllegalArgumentException("Item cannot be both damageable and stackable"
                    + (rpgItem == null ? "" : ":" + rpgItem.getName()));
        }
        int value = parent.getInt(yamlKey());
        if (value < 1 || value > 99) {
            throw new IllegalArgumentException("Max stack size should be between 1 and 99"
                    + (rpgItem == null ? "" : ":" + rpgItem.getName()));
        }
        return value;
    }

    @Override
    public void encode(Integer value, ConfigurationSection config) {
        config.set(yamlKey(), value);
    }
}
