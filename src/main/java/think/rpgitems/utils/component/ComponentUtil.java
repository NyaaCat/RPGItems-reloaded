package think.rpgitems.utils.component;

import io.papermc.paper.datacomponent.DataComponentBuilder;
import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组件系统的三个入口，全部是纯分发逻辑：
 * <ul>
 *   <li>{@link #getComponents} 替代旧 ComponentUtil.getComponents（YAML → 内存）</li>
 *   <li>{@link #toConfigSection} 替代旧 ComponentUtil.toConfigSection（内存 → YAML）</li>
 *   <li>{@link #applyComponents} 替代 RPGItem 里那段一百多分支的 setData if-else 链（内存 → 物品）</li>
 * </ul>
 * 数据结构保持 {@code List<Map<DataComponentType, Object>>} 不变，
 * RPGItem.setComponents / getComponents 无需改动。
 */
public final class ComponentUtil {

    private ComponentUtil() {
    }

    public static List<Map<DataComponentType, Object>> getComponents(ConfigurationSection s) {
        return getComponents(s, null);
    }

    /** YAML → 内存。未注册的 key 静默跳过（与旧行为一致：未知 key 会落入 switch default）。 */
    public static List<Map<DataComponentType, Object>> getComponents(ConfigurationSection s, @Nullable RPGItem rpgItem) {
        List<Map<DataComponentType, Object>> components = new ArrayList<>();
        for (String key : s.getKeys(false)) {
            ComponentHandler<?> handler = ComponentRegistry.byYamlKey(key);
            if (handler == null) {
                continue;
            }

            Object value;
            ConfigurationSection section = s.getConfigurationSection(key);
            if (section != null && section.getBoolean("unset")) {
                // unset 判定统一在这里做，handler.decode 不需要各自处理
                value = ComponentStatus.UNSET;
            } else {
                value = handler.decode(s, rpgItem);
            }
            if (value == null) {
                continue;
            }

            Map<DataComponentType, Object> componentMap = new HashMap<>();
            componentMap.put(handler.type(), value);
            components.add(componentMap);
        }
        return components;
    }

    /** 内存 → YAML。 */
    @SuppressWarnings("unchecked")
    public static void toConfigSection(List<Map<DataComponentType, Object>> componentMaps, ConfigurationSection config) {
        for (Map<DataComponentType, Object> componentMap : componentMaps) {
            for (Map.Entry<DataComponentType, Object> entry : componentMap.entrySet()) {
                ComponentHandler<Object> handler = (ComponentHandler<Object>) ComponentRegistry.byType(entry.getKey());
                if (handler == null) {
                    continue;
                }
                Object value = entry.getValue();
                if (value == ComponentStatus.UNSET) {
                    config.createSection(handler.yamlKey()).set("unset", true);
                } else if (value == ComponentStatus.NON_VALUED) {
                    config.set(handler.yamlKey(), true);
                } else {
                    if (!(value instanceof Enum<?>) && value instanceof DataComponentBuilder<?> builder) {
                        value = builder.build();
                    }
                    handler.encode(value, config);
                }
            }
        }
    }

    /** 内存 → 物品。 */
    public static void applyComponents(ItemStack item, @Nullable List<Map<DataComponentType, Object>> componentMaps) {
        if (componentMaps == null) {
            return;
        }
        for (Map<DataComponentType, Object> componentMap : componentMaps) {
            for (Map.Entry<DataComponentType, Object> entry : componentMap.entrySet()) {
                DataComponentType key = entry.getKey();
                Object value = entry.getValue();
                if (value == ComponentStatus.UNSET) {
                    item.unsetData(key);
                } else if (value == ComponentStatus.NON_VALUED) {
                    item.setData((DataComponentType.NonValued) key);
                } else {
                    ComponentHandler<?> handler = ComponentRegistry.byType(key);
                    if (handler != null) {
                        handler.apply(item, value);
                    }
                }
            }
        }
    }
}
