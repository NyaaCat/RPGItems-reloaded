package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DeathProtection;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ConsumeEffectYaml;

/**
 * death_protection 组件。
 * 原实现里这一段和 consumable 的 effects 解析是整段复制的，
 * 现在两边都只剩对 ConsumeEffectYaml 的一行调用。
 */
public class DeathProtectionHandler implements ComponentHandler<DeathProtection> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.DEATH_PROTECTION;
    }

    @Override
    public String yamlKey() {
        return "death_protection";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        DeathProtection.Builder builder = DeathProtection.deathProtection();
        builder.addEffects(ConsumeEffectYaml.parse(section, "death_effects"));
        return builder;
    }

    @Override
    public void encode(DeathProtection value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        ConsumeEffectYaml.serialize(value.deathEffects(), section, "death_effects");
    }
}
