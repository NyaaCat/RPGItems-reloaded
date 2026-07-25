package think.rpgitems.utils.component.handler;

import io.papermc.paper.block.BlockPredicate;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.item.ItemAdventurePredicate;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import org.bukkit.block.BlockType;
import org.bukkit.configuration.ConfigurationSection;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * can_place_on / can_break 共用实现（两者结构完全一致，只是组件类型和 key 不同）。
 * <pre>
 * can_place_on:
 * - grass_block
 * - stone
 * </pre>
 */
public record ItemAdventurePredicateHandler(DataComponentType type,
                                            String yamlKey) implements ComponentHandler<ItemAdventurePredicate> {

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        List<String> blockTypes = parent.getStringList(yamlKey);
        if (blockTypes.isEmpty()) {
            return null;
        }
        List<BlockType> blocks = new ArrayList<>();
        for (@Subst("stone") String block : blockTypes) {
            BlockType blockType = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.BLOCK).get(Key.key(block));
            if (blockType != null) {
                blocks.add(blockType);
            }
        }
        if (blocks.isEmpty()) {
            return null;
        }
        RegistryKeySet<BlockType> blockKeySet = RegistrySet.keySetFromValues(RegistryKey.BLOCK, blocks);
        return ItemAdventurePredicate.itemAdventurePredicate()
                .addPredicate(BlockPredicate.predicate().blocks(blockKeySet).build());
    }

    @Override
    public void encode(ItemAdventurePredicate value, ConfigurationSection config) {
        List<String> blockTypes = value.predicates().stream()
                .flatMap(p -> {
                    RegistryKeySet<BlockType> blocks = p.blocks();
                    return blocks == null ? Stream.<BlockType>empty()
                            : blocks.values().stream();
                })
                .filter(Objects::nonNull)
                .map(block -> block.key().asString())
                .collect(Collectors.toList());
        config.set(yamlKey, blockTypes);
    }
}
