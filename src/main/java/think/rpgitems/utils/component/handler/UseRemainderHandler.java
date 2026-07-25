package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseRemainder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/**
 * use_remainder: "rpgitem-id"
 * <p>
 * id 到 ItemStack 的解析放在 {@link #apply} 而不是 {@link #decode}——物品加载顺序不保证
 * 被引用的 rpgitem 已经注册，在 decode 阶段解析会因为顺序问题查不到而静默失效。
 * apply 在每次生成实际物品时才跑，此时所有物品必然已加载完毕。
 */
public class UseRemainderHandler implements ComponentHandler<String> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.USE_REMAINDER;
    }

    @Override
    public String yamlKey() {
        return "use_remainder";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        return parent.isString(yamlKey()) ? parent.getString(yamlKey()) : null;
    }

    @Override
    public void encode(String value, ConfigurationSection config) {
        config.set(yamlKey(), value);
    }

    @Override
    public void apply(ItemStack item, Object value) {
        RPGItem rpgItem = ItemManager.getItemByName((String) value);
        if (rpgItem == null) {
            return;
        }
        item.setData(DataComponentTypes.USE_REMAINDER, UseRemainder.useRemainder(rpgItem.toItemStack()));
    }
}
