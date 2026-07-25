package think.rpgitems.utils.component;

import io.papermc.paper.datacomponent.DataComponentBuilder;
import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;

/**
 * 单个数据组件的编解码器。每个组件（rarity / tool / consumable ...）对应一个实现类，
 * 三处逻辑（YAML 解析、YAML 序列化、应用到物品）集中在同一个类里。
 *
 * <p>约定：
 * <ul>
 *   <li>{@code unset: true} 与 NON_VALUED（glider: true 这类无值组件）由
 *       {@link ComponentUtil} 统一处理，<b>不会</b>传入 {@link #encode} / {@link #apply}。</li>
 *   <li>{@link #decode} 返回 {@code null} 表示该 key 下没有可用配置，调用方会跳过。</li>
 *   <li>{@link #decode} 可以返回 Builder（延迟 build），也可以返回最终值；
 *       {@link ComponentUtil} 在 encode 前会自动 build，默认 {@link #apply} 也会自动 build。</li>
 * </ul>
 *
 * @param <T> 组件的最终值类型（build 之后的类型），如 {@code Tool}、{@code ItemRarity}
 */
public interface ComponentHandler<T> {

    /** 该 handler 负责的组件类型，如 {@code DataComponentTypes.RARITY}。 */
    DataComponentType type();

    /** 该组件在 YAML 中的 key，全小写，如 {@code "attack_range"}。 */
    String yamlKey();

    /**
     * 从配置解析出组件值。
     *
     * @param parent  components 段本身（即包含 {@link #yamlKey()} 的那一层）
     * @param rpgItem 所属物品，可能为 null，仅用于报错信息等
     * @return Builder、最终值、{@link ComponentStatus#NON_VALUED}，或 null（表示无配置）
     */
    @Nullable
    Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem);

    /**
     * 把组件值写回配置。value 保证已经 build 过（不会是 Builder），
     * 也保证不是 UNSET / NON_VALUED。
     *
     * @param value  组件最终值
     * @param config components 段本身，实现内自行 {@code config.set(yamlKey(), ...)} 或 createSection
     */
    void encode(T value, ConfigurationSection config);

    /**
     * 把组件值应用到物品上。默认实现直接 setData，Builder 会自动 build，
     * 绝大多数 handler 不需要覆写。
     */
    @SuppressWarnings("unchecked")
    default void apply(ItemStack item, Object value) {
        DataComponentType.Valued<T> valued = (DataComponentType.Valued<T>) type();
        if (!(value instanceof Enum<?>) && value instanceof DataComponentBuilder<?> builder) {
            item.setData(valued, ((DataComponentBuilder<T>) builder).build());
        } else {
            item.setData(valued, (T) value);
        }
    }
}
