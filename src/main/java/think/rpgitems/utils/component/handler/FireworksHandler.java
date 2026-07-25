package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Fireworks;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ComponentYamlUtil;

import java.util.List;
import java.util.Map;

/**
 * fireworks:
 *   flight_duration: 2
 *   explosions:
 *   - type: star
 *     colors: ["255,0,0"]
 */
public class FireworksHandler implements ComponentHandler<Fireworks> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.FIREWORKS;
    }

    @Override
    public String yamlKey() {
        return "fireworks";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        Fireworks.Builder builder = Fireworks.fireworks()
                .flightDuration(section.getInt("flight_duration", 1));
        for (Map<?, ?> explosion : section.getMapList("explosions")) {
            String typeName = ComponentYamlUtil.mapString(explosion, "type", "ball");
            List<String> colors = ComponentYamlUtil.mapStringOrList(explosion, "colors");
            List<String> fadeColors = ComponentYamlUtil.mapStringOrList(explosion, "fade_colors");
            boolean trail = ComponentYamlUtil.mapBoolean(explosion, "trail", false);
            boolean flicker = ComponentYamlUtil.mapBoolean(explosion, "flicker", false);
            builder.addEffect(FireworkExplosionHandler.buildEffect(typeName, colors, fadeColors, trail, flicker));
        }
        return builder;
    }

    @Override
    public void encode(Fireworks value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("flight_duration", value.flightDuration());
        List<Map<String, Object>> explosions = value.effects().stream()
                .map(FireworkExplosionHandler::effectToMap)
                .toList();
        section.set("explosions", explosions);
    }
}
