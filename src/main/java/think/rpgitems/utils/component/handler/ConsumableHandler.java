package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ConsumeEffectYaml;

/**
 * consumable 组件。effects 部分复用 ConsumeEffectYaml，
 * 与 DeathProtectionHandler 共享同一套解析/序列化逻辑。
 */
public class ConsumableHandler implements ComponentHandler<Consumable> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.CONSUMABLE;
    }

    @Override
    public String yamlKey() {
        return "consumable";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        Consumable.Builder builder = Consumable.consumable();

        builder.animation(ItemUseAnimation.valueOf(section.getString("animation", "eat").toUpperCase()));
        builder.consumeSeconds((float) section.getDouble("consume_seconds", 1.6));
        builder.hasConsumeParticles(section.getBoolean("has_consume_particles", true));
        @Subst("entity.generic.eat") String sound = section.getString("sound", "minecraft:entity.generic.eat");
        builder.sound(Key.key(sound));

        builder.addEffects(ConsumeEffectYaml.parse(section, "on_consume_effects"));
        return builder;
    }

    @Override
    public void encode(Consumable value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("animation", value.animation().name());
        section.set("consume_seconds", value.consumeSeconds());
        section.set("has_consume_particles", value.hasConsumeParticles());
        section.set("sound", value.sound().asString());

        ConsumeEffectYaml.serialize(value.consumeEffects(), section, "on_consume_effects");
    }
}
