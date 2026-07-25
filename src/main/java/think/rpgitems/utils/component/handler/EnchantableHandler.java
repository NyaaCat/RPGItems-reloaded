package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Enchantable;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/**
 * enchantable: 5
 * 修复原实现的 bug：原代码把 "读取普通整数值" 的分支也包在
 * {@code if (s.isConfigurationSection(key))} 里面，导致 enchantable 写成
 * 普通整数（唯一合法写法）时永远不会被解析到。unset 由调度器统一处理，
 * 这里只需要处理普通整数。
 */
public class EnchantableHandler implements ComponentHandler<Enchantable> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.ENCHANTABLE;
    }

    @Override
    public String yamlKey() {
        return "enchantable";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        if (!parent.isInt(yamlKey())) {
            return null;
        }
        int value = parent.getInt(yamlKey());
        if (value < 1) {
            throw new IllegalArgumentException("enchantable value must be >= 1"
                    + (rpgItem == null ? "" : ":" + rpgItem.getName()));
        }
        return Enchantable.enchantable(value);
    }

    @Override
    public void encode(Enchantable value, ConfigurationSection config) {
        config.set(yamlKey(), value.value());
    }
}
