package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.SwingAnimation;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class SwingAnimationHandler implements ComponentHandler<SwingAnimation> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.SWING_ANIMATION;
    }

    @Override
    public String yamlKey() {
        return "swing_animation";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        return SwingAnimation.swingAnimation()
                .type(SwingAnimation.Animation.valueOf(section.getString("type", "whack").toUpperCase()))
                .duration(section.getInt("duration", 6));
    }

    @Override
    public void encode(SwingAnimation value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("type", value.type().toString().toUpperCase());
        section.set("duration", value.duration());
    }
}
