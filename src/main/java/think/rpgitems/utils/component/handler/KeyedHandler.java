package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Keyed;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/**
 * 简单 registry 类组件的通用实现，如 damage_type: minecraft:in_fire（对应 Registry.DAMAGE_TYPE）。
 * 在 ComponentRegistry 里传入类型、key 和 RegistryKey 即可实例化，无需单独建类。
 */
@SuppressWarnings("PatternValidation")
public record KeyedHandler<T extends Keyed>(DataComponentType type, String yamlKey,
                                             RegistryKey<T> registryKey) implements ComponentHandler<T> {

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        String value = parent.getString(yamlKey);
        return value == null ? null : RegistryAccess.registryAccess().getRegistry(registryKey).get(Key.key(value));
    }

    @Override
    public void encode(T value, ConfigurationSection config) {
        config.set(yamlKey, value.key().asString());
    }
}
