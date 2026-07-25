package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/** enchantment_glint_override: true */
public class EnchantmentGlintOverrideHandler implements ComponentHandler<Boolean> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE;
    }

    @Override
    public String yamlKey() {
        return "enchantment_glint_override";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        return parent.isBoolean(yamlKey()) ? parent.getBoolean(yamlKey()) : null;
    }

    @Override
    public void encode(Boolean value, ConfigurationSection config) {
        config.set(yamlKey(), value);
    }
}
