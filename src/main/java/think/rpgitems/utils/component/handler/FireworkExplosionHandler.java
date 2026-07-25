package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ComponentYamlUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * firework_explosion:
 *   type: star
 *   colors: ["255,0,0"]
 *   fade_colors: ["0,0,255"]
 *   trail: true
 *   flicker: false
 * <p>
 * {@link #buildEffect} / {@link #effectToMap} 也被 {@link FireworksHandler} 复用，
 * 因为 fireworks 组件里的 explosions 就是同一种结构的列表。
 * {@link FireworkEffect.Builder} 不是 {@link io.papermc.paper.datacomponent.DataComponentBuilder}，
 * 所以这里必须手动 build，不能借助 ComponentUtil 的自动 build。
 */
public class FireworkExplosionHandler implements ComponentHandler<FireworkEffect> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.FIREWORK_EXPLOSION;
    }

    @Override
    public String yamlKey() {
        return "firework_explosion";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        return buildEffect(section.getString("type", "ball"), section.getStringList("colors"),
                section.getStringList("fade_colors"), section.getBoolean("trail", false),
                section.getBoolean("flicker", false));
    }

    @Override
    public void encode(FireworkEffect value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        effectToMap(value).forEach(section::set);
    }

    static FireworkEffect buildEffect(String typeName, List<String> colors, List<String> fadeColors,
                                       boolean trail, boolean flicker) {
        FireworkEffect.Builder builder = FireworkEffect.builder()
                .with(FireworkEffect.Type.valueOf(typeName.toUpperCase()))
                .trail(trail)
                .flicker(flicker);
        for (String color : colors) {
            builder.withColor(ComponentYamlUtil.parseColor(color));
        }
        for (String color : fadeColors) {
            builder.withFade(ComponentYamlUtil.parseColor(color));
        }
        return builder.build();
    }

    static Map<String, Object> effectToMap(FireworkEffect value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", value.getType().name().toLowerCase());
        map.put("trail", value.hasTrail());
        map.put("flicker", value.hasFlicker());
        map.put("colors", value.getColors().stream().map(ComponentYamlUtil::colorToString).toList());
        map.put("fade_colors", value.getFadeColors().stream().map(ComponentYamlUtil::colorToString).toList());
        return map;
    }
}
