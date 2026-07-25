package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemRarity;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/**
 * rarity: epic
 * 最简单的一类组件：单个标量值，无 builder。apply 用接口默认实现即可。
 */
public class RarityHandler implements ComponentHandler<ItemRarity> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.RARITY;
    }

    @Override
    public String yamlKey() {
        return "rarity";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        String value = parent.getString(yamlKey());
        return value == null ? null : ItemRarity.valueOf(value.toUpperCase());
    }

    @Override
    public void encode(ItemRarity value, ConfigurationSection config) {
        config.set(yamlKey(), value.name().toLowerCase());
    }
}
