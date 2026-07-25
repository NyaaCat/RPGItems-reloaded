package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapId;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/** map_id: 0 */
public class MapIdHandler implements ComponentHandler<MapId> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.MAP_ID;
    }

    @Override
    public String yamlKey() {
        return "map_id";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        if (!parent.isInt(yamlKey())) {
            return null;
        }
        return MapId.mapId(parent.getInt(yamlKey()));
    }

    @Override
    public void encode(MapId value, ConfigurationSection config) {
        config.set(yamlKey(), value.id());
    }
}
