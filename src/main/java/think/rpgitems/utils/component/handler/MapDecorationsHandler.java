package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapDecorations;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.map.MapCursor;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/**
 * map_decorations:
 *   home:
 *     type: target_point
 *     x: 12.5
 *     z: -4.0
 *     rotation: 45
 */
@SuppressWarnings("PatternValidation")
public class MapDecorationsHandler implements ComponentHandler<MapDecorations> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.MAP_DECORATIONS;
    }

    @Override
    public String yamlKey() {
        return "map_decorations";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null || section.getKeys(false).isEmpty()) {
            return null;
        }
        MapDecorations.Builder builder = MapDecorations.mapDecorations();
        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            String typeValue = entry == null ? null : entry.getString("type");
            if (typeValue == null) {
                continue;
            }
            MapCursor.Type cursorType = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.MAP_DECORATION_TYPE).get(Key.key(typeValue));
            if (cursorType == null) {
                continue;
            }
            double x = entry.getDouble("x", 0);
            double z = entry.getDouble("z", 0);
            float rotation = (float) entry.getDouble("rotation", 0);
            builder.put(id, MapDecorations.decorationEntry(cursorType, x, z, rotation));
        }
        return builder;
    }

    @Override
    public void encode(MapDecorations value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        for (var entry : value.decorations().entrySet()) {
            ConfigurationSection entrySection = section.createSection(entry.getKey());
            MapDecorations.DecorationEntry decoration = entry.getValue();
            entrySection.set("type", decoration.type().key().asString());
            entrySection.set("x", decoration.x());
            entrySection.set("z", decoration.z());
            entrySection.set("rotation", decoration.rotation());
        }
    }
}
