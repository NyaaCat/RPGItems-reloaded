package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class PotionDurationScaleHandler implements ComponentHandler<Float> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.POTION_DURATION_SCALE;
    }

    @Override
    public String yamlKey() {
        return "potion_duration_scale";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        if (!parent.isDouble(yamlKey()) && !parent.isInt(yamlKey())) {
            return null;
        }
        double scale = parent.getDouble(yamlKey());
        if (scale < 0) {
            throw new IllegalArgumentException("Potion duration scale must be greater than 0"
                    + (rpgItem == null ? "" : ":" + rpgItem.getName()));
        }
        return (float) scale;
    }

    @Override
    public void encode(Float value, ConfigurationSection config) {
        config.set(yamlKey(), value);
    }
}
