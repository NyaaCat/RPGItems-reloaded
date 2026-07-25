package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemArmorTrim;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

@SuppressWarnings("PatternValidation")
public class TrimHandler implements ComponentHandler<ItemArmorTrim> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.TRIM;
    }

    @Override
    public String yamlKey() {
        return "trim";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        String materialKey = section.getString("material");
        String patternKey = section.getString("pattern");
        if (materialKey == null || patternKey == null) {
            return null;
        }
        TrimMaterial material = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.TRIM_MATERIAL).get(Key.key(materialKey));
        TrimPattern pattern = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.TRIM_PATTERN).get(Key.key(patternKey));
        if (material == null || pattern == null) {
            return null;
        }
        return ItemArmorTrim.itemArmorTrim(new ArmorTrim(material, pattern));
    }

    @Override
    public void encode(ItemArmorTrim value, ConfigurationSection config) {
        ArmorTrim armorTrim = value.armorTrim();
        ConfigurationSection section = config.createSection(yamlKey());
        Key materialKey = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.TRIM_MATERIAL).getKey(armorTrim.getMaterial());
        if (materialKey != null) {
            section.set("material", materialKey.asString());
        }
        Key patternKey = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.TRIM_PATTERN).getKey(armorTrim.getPattern());
        if (patternKey != null) {
            section.set("pattern", patternKey.asString());
        }
    }
}
