package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.FoodProperties;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class FoodHandler implements ComponentHandler<FoodProperties> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.FOOD;
    }

    @Override
    public String yamlKey() {
        return "food";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        return FoodProperties.food()
                .canAlwaysEat(section.getBoolean("can_always_eat", false))
                .nutrition(Math.max(section.getInt("nutrition"), 0))
                .saturation((float) section.getDouble("saturation", 0.0));
    }

    @Override
    public void encode(FoodProperties value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("nutrition", value.nutrition());
        section.set("saturation", value.saturation());
        section.set("can_always_eat", value.canAlwaysEat());
    }
}
