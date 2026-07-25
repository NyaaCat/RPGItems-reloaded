package think.rpgitems.utils.component;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Color;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.intellij.lang.annotations.Subst;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 各 handler 共用的解析/序列化小工具。
 *
 * <p>分两组：
 * <ul>
 *   <li>ConfigurationSection 读写：{@link #stringOrList}、{@link #setStringOrList}、
 *       {@link #setDamageTypeSet}</li>
 *   <li>getMapList 风格的原始 Map 读取：{@link #mapInt}、{@link #mapFloat}、
 *       {@link #mapString}、{@link #mapStringOrList}
 *       —— SnakeYAML 反序列化出的数值可能是 Integer / Double / Long，
 *       必须经 Number 转换，不能直接强转</li>
 * </ul>
 */
@SuppressWarnings("PatternValidation")
public final class ComponentYamlUtil {

    private ComponentYamlUtil() {
    }

    // ---------- ConfigurationSection 读写 ----------

    /**
     * 读取一个"既可能是单个字符串、也可能是字符串列表"的字段，统一返回 List。
     * 无配置返回空列表。
     */
    public static List<String> stringOrList(ConfigurationSection s, String path) {
        if (s.isString(path)) {
            String v = s.getString(path);
            return v == null ? List.of() : List.of(v);
        }
        if (s.isList(path)) {
            return s.getStringList(path);
        }
        return List.of();
    }

    /** 序列化的对称操作：单元素写成字符串，多元素写成列表。 */
    public static void setStringOrList(ConfigurationSection config, String path, List<String> values) {
        Object v = singleOrList(values);
        if (v != null) {
            config.set(path, v);
        }
    }

    // ---------- getMapList 风格的原始 Map 读取 ----------

    public static int mapInt(Map<?, ?> map, String key, int def) {
        return map.get(key) instanceof Number n ? n.intValue() : def;
    }

    public static float mapFloat(Map<?, ?> map, String key, float def) {
        return map.get(key) instanceof Number n ? n.floatValue() : def;
    }

    public static boolean mapBoolean(Map<?, ?> map, String key, boolean def) {
        return map.get(key) instanceof Boolean b ? b : def;
    }

    public static String mapString(Map<?, ?> map, String key, String def) {
        return map.get(key) instanceof String s ? s : def;
    }

    /** {@link #stringOrList} 的 Map 版本。 */
    public static List<String> mapStringOrList(Map<?, ?> map, String key) {
        Object v = map.get(key);
        if (v instanceof String s) {
            return List.of(s);
        }
        if (v instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object o : list) {
                if (o instanceof String s) {
                    result.add(s);
                }
            }
            return result;
        }
        return List.of();
    }

    // ---------- DamageType 集合（含 #tag 支持）----------

    /**
     * 由 key 字符串列表构建 DamageType 的 RegistryKeySet。
     * 与原版语义一致：要么是单个 "#tag"，要么是纯 ID 列表，两者不允许混用。
     */
    public static RegistryKeySet<DamageType> damageTypeSet(List<String> keys) {
        if (keys.size() == 1 && keys.getFirst().startsWith("#")) {
            Registry<DamageType> registry = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.DAMAGE_TYPE);
            @Subst("is_projectile") String name = keys.getFirst().substring(1);
            TagKey<DamageType> tagKey = TagKey.create(RegistryKey.DAMAGE_TYPE, Key.key(name));
            if (!registry.hasTag(tagKey)) {
                throw new IllegalArgumentException("Unknown damage type tag: " + keys.getFirst());
            }
            return registry.getTag(tagKey);
        }

        List<TypedKey<DamageType>> typedKeys = new ArrayList<>(keys.size());
        for (String k : keys) {
            if (k.startsWith("#")) {
                throw new IllegalArgumentException(
                        "Damage type tag cannot be mixed with plain damage types: " + k);
            }
            typedKeys.add(TypedKey.create(RegistryKey.DAMAGE_TYPE, Key.key(k)));
        }
        return RegistrySet.keySet(RegistryKey.DAMAGE_TYPE, typedKeys);
    }

    /**
     * {@link #damageTypeSet} 的对称操作，返回可直接放进 YAML 的对象：
     * Tag → "#key" 字符串；单元素 → 字符串；多元素 → 列表；空集 → null。
     * 既可用于 config.set，也可用于 getMapList 风格的 Map value。
     */
    public static Object damageTypeSetToYaml(RegistryKeySet<DamageType> set) {
        if (set instanceof Tag<DamageType> tag) {
            return "#" + tag.tagKey().key().asString();
        }
        return singleOrList(set.values().stream().map(k -> k.key().asString()).toList());
    }

    /** 把 DamageType 集合写到 ConfigurationSection 的 path 处，空集不写。 */
    public static void setDamageTypeSet(ConfigurationSection config, String path, RegistryKeySet<DamageType> set) {
        Object v = damageTypeSetToYaml(set);
        if (v != null) {
            config.set(path, v);
        }
    }

    // ---------- 颜色 ----------

    /**
     * 解析 "r,g,b" 或单个整数形式的颜色字符串。
     * dyed_color 与 potion_contents.custom_color 共用。
     */
    public static Color parseColor(String colorValue) {
        if (colorValue == null) {
            return null;
        }
        if (colorValue.contains(",")) {
            String[] parts = colorValue.split(",");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid RGB format, expected 3 values: " + colorValue);
            }
            try {
                return Color.fromRGB(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim())
                );
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid RGB format: " + colorValue);
            }
        }
        try {
            return Color.fromRGB(Integer.parseInt(colorValue.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid color value: " + colorValue);
        }
    }

    /** parseColor 的对称操作，输出 "r,g,b"。 */
    public static String colorToString(Color color) {
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }

    // ---------- 内部 ----------

    private static Object singleOrList(List<String> values) {
        if (values.isEmpty()) {
            return null;
        }
        return values.size() == 1 ? values.getFirst() : values;
    }
}
