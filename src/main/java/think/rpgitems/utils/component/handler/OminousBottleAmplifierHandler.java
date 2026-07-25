package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.OminousBottleAmplifier;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/**
 * ominous_bottle_amplifier: 1
 * 修复原实现的 bug：原代码 decode 存的是 OminousBottleAmplifier，
 * apply 转型成 OminousBottleAmplifier，但 encode 那边误判成 Integer，
 * 导致保存出来的 YAML 永远不会写这个字段。这里统一用 OminousBottleAmplifier。
 */
public class OminousBottleAmplifierHandler implements ComponentHandler<OminousBottleAmplifier> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER;
    }

    @Override
    public String yamlKey() {
        return "ominous_bottle_amplifier";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        if (!parent.isInt(yamlKey())) {
            return null;
        }
        int amplifier = parent.getInt(yamlKey());
        if (amplifier < 0 || amplifier > 4) {
            throw new IllegalArgumentException("Ominous bottle amplifier must be between 0 and 4"
                    + (rpgItem == null ? "" : ":" + rpgItem.getName()));
        }
        return OminousBottleAmplifier.amplifier(amplifier);
    }

    @Override
    public void encode(OminousBottleAmplifier value, ConfigurationSection config) {
        config.set(yamlKey(), value.amplifier());
    }
}
