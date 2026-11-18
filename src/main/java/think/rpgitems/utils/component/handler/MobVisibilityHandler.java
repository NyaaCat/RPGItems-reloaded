package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MobVisibility;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.set.RegistrySet;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.jspecify.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ComponentYamlUtil;

public class MobVisibilityHandler implements ComponentHandler<MobVisibility> {
    @Override
    public DataComponentType type() {
        return DataComponentTypes.MOB_VISIBILITY;
    }

    @Override
    public String yamlKey() {
        return "mob_visibility";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        float visibility = (float) Math.clamp(section.getDouble("visibility", 1.0), 0.0, 10.0);
        return MobVisibility.mobVisibility(ComponentYamlUtil.registryKeySet(RegistryKey.ENTITY_TYPE, ComponentYamlUtil.stringOrList(section, "targeting_entity_types")), visibility);
    }

    @Override
    public void encode(MobVisibility value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        ComponentYamlUtil.setSetToYaml(section, "targeting_entity_types", value.targetingEntityTypes());
        section.set("visibility", value.visibility());
    }
}
