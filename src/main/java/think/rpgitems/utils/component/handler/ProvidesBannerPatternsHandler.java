package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ComponentYamlUtil;

import java.util.List;

/**
 * provides_banner_patterns: "globe"
 * provides_banner_patterns: [globe, creeper]
 * 与 damage_resistance 不同，横幅图案没有 tag/普通值互斥的原版限制，
 * 因此这里不做 tag 支持，直接按普通 key 列表处理。
 */
@SuppressWarnings("PatternValidation")
public class ProvidesBannerPatternsHandler implements ComponentHandler<RegistryKeySet<PatternType>> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.PROVIDES_BANNER_PATTERNS;
    }

    @Override
    public String yamlKey() {
        return "provides_banner_patterns";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        List<String> patterns = ComponentYamlUtil.stringOrList(parent, yamlKey());
        if (patterns.isEmpty()) {
            return null;
        }
        List<TypedKey<PatternType>> typedKeys = patterns.stream()
                .map(p -> TypedKey.create(RegistryKey.BANNER_PATTERN, Key.key(p)))
                .toList();
        return RegistrySet.keySet(RegistryKey.BANNER_PATTERN, typedKeys);
    }

    @Override
    public void encode(RegistryKeySet<PatternType> value, ConfigurationSection config) {
        ComponentYamlUtil.setStringOrList(config, yamlKey(),
                value.values().stream().map(k -> k.key().asString()).toList());
    }
}
