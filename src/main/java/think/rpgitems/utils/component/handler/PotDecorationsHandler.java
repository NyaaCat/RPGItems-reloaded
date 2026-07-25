package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotDecorations;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/**
 * pot_decorations:
 *   back: brick
 *   left: skeleton_skull
 *   right: creeper_head
 *   front: brick
 */
@SuppressWarnings("PatternValidation")
public class PotDecorationsHandler implements ComponentHandler<PotDecorations> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.POT_DECORATIONS;
    }

    @Override
    public String yamlKey() {
        return "pot_decorations";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        return PotDecorations.potDecorations()
                .back(resolve(section.getString("back")))
                .left(resolve(section.getString("left")))
                .right(resolve(section.getString("right")))
                .front(resolve(section.getString("front")));
    }

    private static @Nullable ItemType resolve(@Nullable String value) {
        if (value == null) {
            return null;
        }
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).get(Key.key(value));
    }

    @Override
    public void encode(PotDecorations value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        setIfPresent(section, "back", value.back());
        setIfPresent(section, "left", value.left());
        setIfPresent(section, "right", value.right());
        setIfPresent(section, "front", value.front());
    }

    private static void setIfPresent(ConfigurationSection section, String key, @Nullable ItemType type) {
        if (type != null) {
            section.set(key, type.key().asString());
        }
    }
}
