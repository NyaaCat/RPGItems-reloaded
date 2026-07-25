package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/**
 * base_color: "white"
 * 注：原实现里当 base_color 是子段但不含 unset:true 时会静默丢弃，
 * 这里只支持标量写法（子段形式只用于 unset）。
 */
public class BaseColorHandler implements ComponentHandler<DyeColor> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.BASE_COLOR;
    }

    @Override
    public String yamlKey() {
        return "base_color";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        String value = parent.getString(yamlKey());
        return value == null ? null : DyeColor.valueOf(value.toUpperCase());
    }

    @Override
    public void encode(DyeColor value, ConfigurationSection config) {
        config.set(yamlKey(), value.name().toLowerCase());
    }
}
