package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/**
 * 简单枚举类组件的通用实现，如 axolotl_variant: blue（对应 Axolotl.Variant）。
 * 在 ComponentRegistry 里传入类型、key 和枚举 class 即可实例化，无需单独建类。
 */
public record EnumHandler<T extends Enum<T>>(DataComponentType type, String yamlKey,
                                              Class<T> enumClass) implements ComponentHandler<T> {

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        String value = parent.getString(yamlKey);
        return value == null ? null : Enum.valueOf(enumClass, value.toUpperCase());
    }

    @Override
    public void encode(T value, ConfigurationSection config) {
        config.set(yamlKey, value.name().toLowerCase());
    }
}
