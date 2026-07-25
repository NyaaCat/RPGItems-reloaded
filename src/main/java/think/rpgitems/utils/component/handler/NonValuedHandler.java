package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ComponentStatus;

/**
 * 无值组件（glider: true / intangible_projectile: true 这类）的通用实现。
 * 不需要每个组件单独建类，在 ComponentRegistry 里传入类型和 key 实例化即可。
 * encode / apply 永远不会被调用（NON_VALUED 由 ComponentUtilV2 统一处理），
 * 所以留空实现。
 */
public final class NonValuedHandler implements ComponentHandler<Void> {

    private final DataComponentType.NonValued type;
    private final String yamlKey;

    public NonValuedHandler(DataComponentType.NonValued type, String yamlKey) {
        this.type = type;
        this.yamlKey = yamlKey;
    }

    @Override
    public DataComponentType type() {
        return type;
    }

    @Override
    public String yamlKey() {
        return yamlKey;
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        return parent.getBoolean(yamlKey) ? ComponentStatus.NON_VALUED : null;
    }

    @Override
    public void encode(Void value, ConfigurationSection config) {
        // NON_VALUED 由 ComponentUtilV2 统一序列化为 "key: true"，不会走到这里
    }

    @Override
    public void apply(ItemStack item, Object value) {
        // NON_VALUED 由 ComponentUtilV2 统一 setData(NonValued)，不会走到这里
    }
}
