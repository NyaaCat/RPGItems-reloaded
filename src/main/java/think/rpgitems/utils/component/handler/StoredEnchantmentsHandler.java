package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/**
 * stored_enchantments:
 *   sharpness: 5
 *   smite: 5
 */
@SuppressWarnings("PatternValidation")
public class StoredEnchantmentsHandler implements ComponentHandler<ItemEnchantments> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.STORED_ENCHANTMENTS;
    }

    @Override
    public String yamlKey() {
        return "stored_enchantments";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null || section.getKeys(false).isEmpty()) {
            return null;
        }
        ItemEnchantments.Builder builder = ItemEnchantments.itemEnchantments();
        for (String enc : section.getKeys(false)) {
            Enchantment enchantment = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT).get(Key.key(enc));
            if (enchantment != null) {
                builder.add(enchantment, section.getInt(enc, 1));
            }
        }
        return builder;
    }

    @Override
    public void encode(ItemEnchantments value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        for (var entry : value.enchantments().entrySet()) {
            section.set(entry.getKey().getKey().asString(), entry.getValue());
        }
    }
}
