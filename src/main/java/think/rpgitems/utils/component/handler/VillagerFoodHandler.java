package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.VillagerFood;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class VillagerFoodHandler implements ComponentHandler<VillagerFood> {
    @Override
    public DataComponentType type() {
        return DataComponentTypes.VILLAGER_FOOD;
    }

    @Override
    public String yamlKey() {
        return "villager_food";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        int nutrition = Math.max(0, section.getInt("nutrition", 0));
        return VillagerFood.villagerFood(nutrition);
    }

    @Override
    public void encode(VillagerFood value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("nutrition", value.nutrition());
    }
}
