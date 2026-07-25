package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapItemColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ComponentYamlUtil;

/** map_color: "255,0,0" */
public class MapColorHandler implements ComponentHandler<MapItemColor> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.MAP_COLOR;
    }

    @Override
    public String yamlKey() {
        return "map_color";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        String value = parent.getString(yamlKey());
        if (value == null) {
            return null;
        }
        return MapItemColor.mapItemColor().color(ComponentYamlUtil.parseColor(value));
    }

    @Override
    public void encode(MapItemColor value, ConfigurationSection config) {
        config.set(yamlKey(), ComponentYamlUtil.colorToString(value.color()));
    }
}
