package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseEffects;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class UseEffectsHandler implements ComponentHandler<UseEffects> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.USE_EFFECTS;
    }

    @Override
    public String yamlKey() {
        return "use_effects";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        float speedMultiplier = (float) section.getDouble("speed_multiplier", 0.2);
        if (speedMultiplier < 0 || speedMultiplier > 1) {
            throw new IllegalArgumentException("Use effects speed multiplier must be between 0 and 1"
                    + (rpgItem == null ? "" : ":" + rpgItem.getName()));
        }
        return UseEffects.useEffects()
                .canSprint(section.getBoolean("can_sprint", false))
                .interactVibrations(section.getBoolean("interact_vibrations", true))
                .speedMultiplier(speedMultiplier);
    }

    @Override
    public void encode(UseEffects value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("can_sprint", value.canSprint());
        section.set("interact_vibrations", value.interactVibrations());
        section.set("speed_multiplier", value.speedMultiplier());
    }
}
