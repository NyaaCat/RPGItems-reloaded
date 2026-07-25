package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class BreakSoundHandler implements ComponentHandler<Key> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.BREAK_SOUND;
    }

    @Override
    public String yamlKey() {
        return "break_sound";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        @Subst("entity.item.break") String sound = parent.getString(yamlKey(), "minecraft:entity.item.break");
        if (!parent.contains(yamlKey())) {
            return null;
        }
        return Key.key(sound);
    }

    @Override
    public void encode(Key value, ConfigurationSection config) {
        config.set(yamlKey(), value.asString());
    }
}
