package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Repairable;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemType;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ComponentYamlUtil;

import java.util.List;
import java.util.Objects;

public class RepairableHandler implements ComponentHandler<Repairable> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.REPAIRABLE;
    }

    @Override
    public String yamlKey() {
        return "repairable";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        List<String> items = ComponentYamlUtil.stringOrList(section, "items");
        if (items.isEmpty()) {
            return null;
        }
        List<ItemType> types = items.stream()
                .map(i -> {
                    @Subst("air") String item = i;
                    return RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).get(Key.key(item));
                })
                .filter(Objects::nonNull)
                .toList();
        if (types.isEmpty()) {
            return null;
        }
        RegistryKeySet<ItemType> keySet = RegistrySet.keySetFromValues(RegistryKey.ITEM, types);
        return Repairable.repairable(keySet);
    }

    @Override
    public void encode(Repairable value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        ComponentYamlUtil.setStringOrList(section, "items",
                value.types().values().stream().map(k -> k.key().asString()).toList());
    }
}
