package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class MinimumAttackChargeHandler implements ComponentHandler<Float> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.MINIMUM_ATTACK_CHARGE;
    }

    @Override
    public String yamlKey() {
        return "minimum_attack_charge";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        if (!parent.isDouble(yamlKey()) && !parent.isInt(yamlKey())) {
            return null;
        }
        float charge = (float) parent.getDouble(yamlKey());
        if (charge < 0 || charge > 1) {
            throw new IllegalArgumentException("Minimum attack charge must be between 0 and 1"
                    + (rpgItem == null ? "" : ":" + rpgItem.getName()));
        }
        return charge;
    }

    @Override
    public void encode(Float value, ConfigurationSection config) {
        config.set(yamlKey(), value);
    }
}
