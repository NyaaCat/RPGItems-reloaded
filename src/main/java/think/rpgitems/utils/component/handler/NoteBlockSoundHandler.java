package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/** note_block_sound: "minecraft:block.note_block.harp" */
public class NoteBlockSoundHandler implements ComponentHandler<Key> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.NOTE_BLOCK_SOUND;
    }

    @Override
    public String yamlKey() {
        return "note_block_sound";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        @Subst("block.note_block.harp") String sound = parent.getString(yamlKey());
        return sound == null ? null : Key.key(sound);
    }

    @Override
    public void encode(Key value, ConfigurationSection config) {
        config.set(yamlKey(), value.asString());
    }
}
