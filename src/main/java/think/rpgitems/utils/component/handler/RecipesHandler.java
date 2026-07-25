package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

import java.util.List;

/**
 * recipes:
 * - "minecraft:diamond_sword"
 */
@SuppressWarnings("PatternValidation")
public class RecipesHandler implements ComponentHandler<List<Key>> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.RECIPES;
    }

    @Override
    public String yamlKey() {
        return "recipes";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        List<String> keys = parent.getStringList(yamlKey());
        if (keys.isEmpty()) {
            return null;
        }
        return keys.stream().map(Key::key).toList();
    }

    @Override
    public void encode(List<Key> value, ConfigurationSection config) {
        config.set(yamlKey(), value.stream().map(Key::asString).toList());
    }
}
