package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class MaxDamageHandler implements ComponentHandler<Integer> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.MAX_DAMAGE;
    }

    @Override
    public String yamlKey() {
        return "max_damage";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        if (!parent.isInt(yamlKey())) {
            return null;
        }
        if (parent.getInt("max_stack_size") > 1) {
            throw new IllegalArgumentException("Item cannot be both damageable and stackable"
                    + (rpgItem == null ? "" : ":" + rpgItem.getName()));
        }
        int value = parent.getInt(yamlKey());
        return value > 0 ? value : null;
    }

    @Override
    public void encode(Integer value, ConfigurationSection config) {
        config.set(yamlKey(), value);
    }
}
