package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.MusicInstrument;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

import java.util.Objects;

/**
 * instrument: "minecraft:ponder_goat_horn"   注册表已有乐器，直接引用
 * instrument:                                自定义乐器
 *   description: "<red>test"
 *   sound_event: { sound_id: "...", range: 5 }
 *   range: 4
 *   use_duration: 0.01
 * <p>
 * 注：MusicInstrument 不是 DataComponentBuilder，接口默认的 apply 实现
 * 会走"非 Builder"分支直接 setData，不需要覆写。
 */
@SuppressWarnings("PatternValidation")
public class InstrumentHandler implements ComponentHandler<MusicInstrument> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.INSTRUMENT;
    }

    @Override
    public String yamlKey() {
        return "instrument";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section != null) {
            String descriptionStr = section.getString("description", "");
            Component description = MiniMessage.miniMessage().deserialize(descriptionStr);
            ConfigurationSection soundEvent = section.getConfigurationSection("sound_event");
            float soundRange = soundEvent != null ? (float) soundEvent.getDouble("range", 1.0) : 1f;
            @Subst("item.goat_horn.sound.0")
            String soundKey = soundEvent != null
                    ? soundEvent.getString("sound_id", "item.goat_horn.sound.0")
                    : "item.goat_horn.sound.0";
            float range = (float) section.getDouble("range", 1.0);
            float useDuration = (float) section.getDouble("use_duration", 1.0);
            return MusicInstrument.create(factory -> factory.empty()
                    .description(description)
                    .duration(useDuration)
                    .range(range)
                    .soundEvent(sf -> sf.empty().fixedRange(soundRange).location(Key.key(soundKey))));
        }
        if (parent.isString(yamlKey())) {
            return RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.INSTRUMENT)
                    .get(Key.key(Objects.requireNonNull(parent.getString(yamlKey()))));
        }
        return null;
    }

    @Override
    public void encode(MusicInstrument value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("description", MiniMessage.miniMessage().serialize(value.description()));
        section.set("sound_event.range", value.getRange());
        section.set("sound_event.sound_id", Objects.requireNonNull(Registry.SOUNDS.getKey(value.getSound())).asString());
        section.set("range", value.getRange());
        section.set("use_duration", value.getDuration());
    }
}
