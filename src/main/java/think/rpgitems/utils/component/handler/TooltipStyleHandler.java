package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

@SuppressWarnings("PatternValidation")
public class TooltipStyleHandler implements ComponentHandler<Key> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.TOOLTIP_STYLE;
    }

    @Override
    public String yamlKey() {
        return "tooltip_style";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        String value = parent.getString(yamlKey());
        return value == null ? null : Key.key(value);
    }

    @Override
    public void encode(Key value, ConfigurationSection config) {
        config.set(yamlKey(), value.toString());
    }
}
