package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ComponentYamlUtil;

/** dyed_color: "255,255,255" */
public class DyedColorHandler implements ComponentHandler<DyedItemColor> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.DYED_COLOR;
    }

    @Override
    public String yamlKey() {
        return "dyed_color";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        String value = parent.getString(yamlKey());
        if (value == null) {
            return null;
        }
        return DyedItemColor.dyedItemColor().color(ComponentYamlUtil.parseColor(value));
    }

    @Override
    public void encode(DyedItemColor value, ConfigurationSection config) {
        Color color = value.color();
        config.set(yamlKey(), ComponentYamlUtil.colorToString(color));
    }
}
