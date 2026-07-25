package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BannerPatternLayers;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ComponentYamlUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * banner_patterns 组件。列表格式（getMapList），层级顺序即列表顺序：
 * <pre>
 * banner_patterns:
 * - color: black
 *   pattern: base
 * - color: yellow
 *   pattern: creeper
 * </pre>
 * 旧的 layer_1/layer_2 命名子段格式不再支持
 * （命名子段本身并不保证顺序语义，列表才是横幅层的正确表达）。
 */
public class BannerPatternsHandler implements ComponentHandler<BannerPatternLayers> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.BANNER_PATTERNS;
    }

    @Override
    public String yamlKey() {
        return "banner_patterns";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        List<Map<?, ?>> layers = parent.getMapList(yamlKey());
        if (layers.isEmpty()) {
            return null;
        }
        BannerPatternLayers.Builder builder = BannerPatternLayers.bannerPatternLayers();
        for (Map<?, ?> layer : layers) {
            String color = ComponentYamlUtil.mapString(layer, "color", null);
            @Subst("base") String pattern = ComponentYamlUtil.mapString(layer, "pattern", null);
            if (color == null || pattern == null) {
                continue;
            }
            PatternType patternType = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.BANNER_PATTERN).get(Key.key(pattern));
            if (patternType != null) {
                builder.add(new Pattern(DyeColor.valueOf(color.toUpperCase()), patternType));
            }
        }
        return builder;
    }

    @Override
    public void encode(BannerPatternLayers value, ConfigurationSection config) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Pattern pattern : value.patterns()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("color", pattern.getColor().name().toLowerCase());
            map.put("pattern", RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.BANNER_PATTERN)
                    .getKey(pattern.getPattern()).asString());
            list.add(map);
        }
        config.set(yamlKey(), list);
    }
}
