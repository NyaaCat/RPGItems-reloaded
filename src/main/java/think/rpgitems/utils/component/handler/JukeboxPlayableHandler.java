package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.JukeboxPlayable;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.JukeboxSong;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/** jukebox_playable: minecraft:pigstep */
@SuppressWarnings("PatternValidation")
public class JukeboxPlayableHandler implements ComponentHandler<JukeboxPlayable> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.JUKEBOX_PLAYABLE;
    }

    @Override
    public String yamlKey() {
        return "jukebox_playable";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        if (!parent.isString(yamlKey())) {
            return null;
        }
        JukeboxSong song = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.JUKEBOX_SONG)
                .get(Key.key(parent.getString(yamlKey(), "cat")));
        if (song == null) {
            song = JukeboxSong.CAT;
        }
        return JukeboxPlayable.jukeboxPlayable(song);
    }

    @Override
    public void encode(JukeboxPlayable value, ConfigurationSection config) {
        config.set(yamlKey(), value.jukeboxSong().getKey().asString());
    }
}
