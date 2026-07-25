package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 值为"物品列表"的组件通用实现，如 charged_projectiles / bundle_contents / container。
 * 列表元素不是原始 ItemStack 数据，而是 rpgitem id（同 {@link UseRemainderHandler}）。
 * <p>
 * decode/encode 只处理 id 列表本身，不在这一步查 {@link ItemManager}——物品加载顺序
 * 不保证被引用的 rpgitem 已经注册，过早解析会把空列表缓存进 {@code components} 字段
 * （表现为最终物品上的组件是空的，而不是完全没有这个组件）。真正按 id 查物品、组装
 * ItemStack 列表放在 {@link #apply}，它在每次生成实际物品（{@code updateItem}）时才跑，
 * 此时所有物品必然已加载完毕。
 */
public record ItemStackListHandler<C, B>(
        DataComponentType type, String yamlKey,
        Supplier<B> newBuilder, BiConsumer<B, ItemStack> add, Function<B, C> build
) implements ComponentHandler<List<String>> {

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        List<String> ids = parent.getStringList(yamlKey);
        return ids.isEmpty() ? null : List.copyOf(ids);
    }

    @Override
    public void encode(List<String> value, ConfigurationSection config) {
        config.set(yamlKey, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void apply(ItemStack item, Object value) {
        List<String> ids = (List<String>) value;
        B builder = newBuilder.get();
        for (String id : ids) {
            RPGItem rpgItem = ItemManager.getItemByName(id);
            if (rpgItem != null) {
                add.accept(builder, rpgItem.toItemStack());
            }
        }
        DataComponentType.Valued<C> valued = (DataComponentType.Valued<C>) type;
        item.setData(valued, build.apply(builder));
    }
}
