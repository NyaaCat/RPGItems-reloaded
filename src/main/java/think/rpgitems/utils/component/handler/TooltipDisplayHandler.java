package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * tooltip_display:
 *   hide_tooltip: true
 *   hidden_components: [trim, rarity]
 * <p>
 * 修复原实现的 bug：原代码 hide_tooltip 和 hidden_components 写在 if/else 里，
 * 二选一生效；实际上二者是组件里两个独立字段，应该能同时设置。
 */
@SuppressWarnings("PatternValidation")
public class TooltipDisplayHandler implements ComponentHandler<TooltipDisplay> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.TOOLTIP_DISPLAY;
    }

    @Override
    public String yamlKey() {
        return "tooltip_display";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        TooltipDisplay.Builder builder = TooltipDisplay.tooltipDisplay();
        builder.hideTooltip(section.getBoolean("hide_tooltip", false));

        List<String> hiddenComponents = section.getStringList("hidden_components");
        if (!hiddenComponents.isEmpty()) {
            Set<DataComponentType> hiddenTypes = hiddenComponents.stream()
                    .map(String::toLowerCase)
                    .map(s -> Registry.DATA_COMPONENT_TYPE.get(Key.key(s)))
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));
            builder.hiddenComponents(hiddenTypes);
        }
        return builder;
    }

    @Override
    public void encode(TooltipDisplay value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("hide_tooltip", value.hideTooltip());
        Set<DataComponentType> hiddenComponents = value.hiddenComponents();
        if (!hiddenComponents.isEmpty()) {
            section.set("hidden_components", hiddenComponents.stream()
                    .map(t -> t.getKey().value())
                    .collect(Collectors.toList()));
        }
    }
}
