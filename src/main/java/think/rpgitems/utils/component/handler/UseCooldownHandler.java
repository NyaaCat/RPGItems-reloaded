package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

@SuppressWarnings("PatternValidation")
public class UseCooldownHandler implements ComponentHandler<UseCooldown> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.USE_COOLDOWN;
    }

    @Override
    public String yamlKey() {
        return "use_cooldown";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        double raw = section.getDouble("seconds");
        float seconds = raw <= 0 ? 1.0E-20f : (float) raw;
        UseCooldown.Builder builder = UseCooldown.useCooldown(seconds);
        String cooldownGroup = section.getString("cooldown_group");
        if (cooldownGroup != null) {
            builder.cooldownGroup(Key.key(cooldownGroup));
        }
        return builder;
    }

    @Override
    public void encode(UseCooldown value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("seconds", value.seconds());
        if (value.cooldownGroup() != null) {
            section.set("cooldown_group", value.cooldownGroup().asString());
        }
    }
}
