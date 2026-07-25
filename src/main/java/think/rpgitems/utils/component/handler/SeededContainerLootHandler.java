package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.SeededContainerLoot;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/**
 * container_loot:
 *   loot_table: "minecraft:chests/simple_dungeon"
 *   seed: 0
 */
@SuppressWarnings("PatternValidation")
public class SeededContainerLootHandler implements ComponentHandler<SeededContainerLoot> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.CONTAINER_LOOT;
    }

    @Override
    public String yamlKey() {
        return "container_loot";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        String lootTable = section.getString("loot_table");
        if (lootTable == null) {
            return null;
        }
        return SeededContainerLoot.seededContainerLoot(Key.key(lootTable))
                .seed(section.getLong("seed", 0));
    }

    @Override
    public void encode(SeededContainerLoot value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("loot_table", value.lootTable().asString());
        section.set("seed", value.seed());
    }
}
