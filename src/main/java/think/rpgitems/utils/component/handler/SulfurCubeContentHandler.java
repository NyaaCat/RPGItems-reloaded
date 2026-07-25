package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.SulfurCubeContent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

/**
 * sulfur_cube_content: "rpgitem-id"（同 {@link UseRemainderHandler}，值是 rpgitem id 而非原始物品数据）
 * <p>
 * 和 {@link ItemStackListHandler} 一样，id 到 ItemStack 的解析放在 {@link #apply}
 * 而不是 {@link #decode}，避免被引用的 rpgitem 因加载顺序还没注册导致解析失败。
 */
public class SulfurCubeContentHandler implements ComponentHandler<String> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.SULFUR_CUBE_CONTENT;
    }

    @Override
    public String yamlKey() {
        return "sulfur_cube_content";
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
        item.setData(DataComponentTypes.SULFUR_CUBE_CONTENT, SulfurCubeContent.sulfurCubeContent(rpgItem.toItemStack()));
    }
}
