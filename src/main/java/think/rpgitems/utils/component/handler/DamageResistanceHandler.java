package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DamageResistant;
import io.papermc.paper.registry.set.RegistryKeySet;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.damage.DamageType;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ComponentYamlUtil;

import java.util.List;

/**
 * damage_resistance: "in_fire"        单个
 * damage_resistance: "#is_fire"       tag
 * damage_resistance: [in_fire, ...]   列表
 */
public class DamageResistanceHandler implements ComponentHandler<DamageResistant> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.DAMAGE_RESISTANT;
    }

    @Override
    public String yamlKey() {
        return "damage_resistance";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        List<String> types = ComponentYamlUtil.stringOrList(parent, yamlKey());
        if (types.isEmpty()) {
            return null;
        }
        return DamageResistant.damageResistant(ComponentYamlUtil.damageTypeSet(types));
    }

    @Override
    public void encode(DamageResistant value, ConfigurationSection config) {
        RegistryKeySet<DamageType> types = value.types();
        Object v = ComponentYamlUtil.damageTypeSetToYaml(types);
        if (v != null) {
            config.set(yamlKey(), v);
        }
    }
}
