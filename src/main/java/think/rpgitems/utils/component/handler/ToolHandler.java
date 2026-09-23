package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.util.TriState;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.configuration.ConfigurationSection;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tool 组件。
 */
public class ToolHandler implements ComponentHandler<Tool> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.TOOL;
    }

    @Override
    public String yamlKey() {
        return "tool";
    }

    @Override
    public @Nullable Object decode(
            ConfigurationSection parent,
            @Nullable RPGItem rpgItem
    ) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }

        Tool.Builder builder = Tool.tool();

        builder.canDestroyBlocksInCreative(
                section.getBoolean("can_destroy_blocks_in_creative", true)
        );

        builder.damagePerBlock(
                Math.max(0, section.getInt("damage_per_block", 1))
        );

        builder.defaultMiningSpeed(
                Math.max(
                        0.0f,
                        (float) section.getDouble("default_mining_speed", 1.0)
                )
        );

        if (section.contains("rules")) {
            List<Tool.Rule> rules = new ArrayList<>();

            for (Map<?, ?> ruleData : section.getMapList("rules")) {
                RegistryKeySet<BlockType> blockKeySet =
                        parseBlocks(ruleData.get("blocks"));

                if (blockKeySet == null) {
                    continue;
                }

                Float speed = null;
                if (ruleData.containsKey("speed")) {
                    Object speedValue = ruleData.get("speed");
                    if (speedValue instanceof Number number) {
                        speed = number.floatValue();
                    }
                }

                TriState correctForDrops = TriState.NOT_SET;

                if (ruleData.containsKey("correct_for_drops")) {
                    Object value = ruleData.get("correct_for_drops");

                    if (value instanceof Boolean bool) {
                        correctForDrops = TriState.byBoolean(bool);
                    }
                }

                rules.add(Tool.rule(
                        blockKeySet,
                        speed,
                        correctForDrops
                ));
            }

            builder.addRules(rules);
        }

        return builder;
    }

    private static @Nullable RegistryKeySet<BlockType> parseBlocks(
            Object blocks
    ) {
        List<String> blockStrings;

        if (blocks instanceof String blockString) {
            blockStrings = List.of(blockString);
        } else if (blocks instanceof List<?> list) {
            blockStrings = new ArrayList<>(list.size());

            for (Object value : list) {
                if (!(value instanceof String string)) {
                    return null;
                }

                blockStrings.add(string);
            }
        } else {
            return null;
        }

        if (blockStrings.isEmpty()) {
            return null;
        }

        /*
         * 单独使用 #xxx：
         *
         * blocks: "#minecraft:mineable/pickaxe"
         *
         * 解析成 Tag。
         */
        if (blockStrings.size() == 1
                && blockStrings.getFirst().startsWith("#")) {

            Registry<BlockType> registry =
                    RegistryAccess.registryAccess()
                            .getRegistry(RegistryKey.BLOCK);

            @Subst("minecraft:mineable/pickaxe")
            String name = blockStrings.getFirst().substring(1);

            TagKey<BlockType> tagKey =
                    TagKey.create(
                            RegistryKey.BLOCK,
                            Key.key(name)
                    );

            if (!registry.hasTag(tagKey)) {
                throw new IllegalArgumentException(
                        "Unknown block tag: " + blockStrings.getFirst()
                );
            }

            return registry.getTag(tagKey);
        }

        /*
         * Tag 不能和普通 Block 混合：
         *
         * blocks:
         *   - "#minecraft:mineable/pickaxe"
         *   - "minecraft:stone"
         */
        List<TypedKey<BlockType>> typedKeys =
                new ArrayList<>(blockStrings.size());

        for (String block : blockStrings) {
            if (block.startsWith("#")) {
                throw new IllegalArgumentException(
                        "Block tag cannot be mixed with plain blocks: " + block
                );
            }

            typedKeys.add(
                    TypedKey.create(
                            RegistryKey.BLOCK,
                            Key.key(block)
                    )
            );
        }

        return RegistrySet.keySet(
                RegistryKey.BLOCK,
                typedKeys
        );
    }

    @Override
    public void encode(
            Tool value,
            ConfigurationSection config
    ) {
        ConfigurationSection section =
                config.createSection(yamlKey());

        section.set(
                "can_destroy_blocks_in_creative",
                value.canDestroyBlocksInCreative()
        );

        section.set(
                "damage_per_block",
                value.damagePerBlock()
        );

        section.set(
                "default_mining_speed",
                value.defaultMiningSpeed()
        );

        List<Tool.Rule> rules = value.rules();

        if (rules.isEmpty()) {
            return;
        }

        List<Map<String, Object>> rulesList =
                new ArrayList<>(rules.size());

        for (Tool.Rule rule : rules) {
            Map<String, Object> ruleMap = new HashMap<>();

            RegistryKeySet<BlockType> blocks = rule.blocks();

            /*
             * 如果是 Tag：
             *
             * #minecraft:mineable/pickaxe
             *
             * 如果是普通 RegistryKeySet：
             *
             * [minecraft:stone, minecraft:dirt]
             */
            if (blocks instanceof Tag<?> tag) {
                ruleMap.put(
                        "blocks",
                        "#" + tag.tagKey().key().asString()
                );
            } else {
                ruleMap.put(
                        "blocks",
                        blocks.values().stream()
                                .map(key -> key.key().asString())
                                .toList()
                );
            }

            if (rule.speed() != null) {
                ruleMap.put(
                        "speed",
                        rule.speed()
                );
            }

            if (rule.correctForDrops() != TriState.NOT_SET) {
                ruleMap.put(
                        "correct_for_drops",
                        rule.correctForDrops().toBoolean()
                );
            }

            rulesList.add(ruleMap);
        }

        section.set("rules", rulesList);
    }
}