package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.util.TriState;
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
import java.util.Objects;

/**
 * tool 组件。规则列表（getMapList）形式的复杂组件迁移示例，
 * decode / encode 内容基本就是原来两个大方法里对应分支的原样搬运。
 */
@SuppressWarnings({"unchecked"})
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
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        Tool.Builder builder = Tool.tool();

        builder.canDestroyBlocksInCreative(section.getBoolean("can_destroy_blocks_in_creative", true));
        builder.damagePerBlock(Math.max(0, section.getInt("damage_per_block", 1)));
        builder.defaultMiningSpeed(Math.max(0.0f, (float) section.getDouble("default_mining_speed", 1.0)));

        if (section.contains("rules")) {
            List<Tool.Rule> rules = new ArrayList<>();
            for (Map<?, ?> ruleData : section.getMapList("rules")) {
                RegistryKeySet<BlockType> blockKeySet = parseBlocks(ruleData.get("blocks"));
                if (blockKeySet == null) {
                    continue;
                }
                Float speed = ruleData.containsKey("speed") ? ((Number) ruleData.get("speed")).floatValue() : null;
                TriState correctForDrops = TriState.NOT_SET;
                if (ruleData.containsKey("correct_for_drops")) {
                    correctForDrops = TriState.byBoolean((boolean) ruleData.get("correct_for_drops"));
                }
                rules.add(Tool.rule(blockKeySet, speed, correctForDrops));
            }
            builder.addRules(rules);
        }
        return builder;
    }

    private static @Nullable RegistryKeySet<BlockType> parseBlocks(Object blocks) {
        List<String> blockStrings;
        if (blocks instanceof String blockString) {
            blockStrings = List.of(blockString);
        } else if (blocks instanceof List<?> list) {
            blockStrings = (List<String>) list;
        } else {
            return null;
        }
        List<BlockType> types = blockStrings.stream()
                .map(block -> {
                    @Subst("stone") String b = block;
                    return RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK).get(Key.key(b));
                })
                .filter(Objects::nonNull)
                .toList();
        if (types.isEmpty()) {
            return null;
        }
        return RegistrySet.keySetFromValues(RegistryKey.BLOCK, types);
    }

    @Override
    public void encode(Tool value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());

        section.set("can_destroy_blocks_in_creative", value.canDestroyBlocksInCreative());
        section.set("damage_per_block", value.damagePerBlock());
        section.set("default_mining_speed", value.defaultMiningSpeed());

        List<Tool.Rule> rules = value.rules();
        if (!rules.isEmpty()) {
            List<Map<String, Object>> rulesList = new ArrayList<>();
            for (Tool.Rule rule : rules) {
                Map<String, Object> ruleMap = new HashMap<>();
                ruleMap.put("blocks", rule.blocks().values().stream()
                        .map(k -> k.key().asString())
                        .toList());
                if (rule.speed() != null) {
                    ruleMap.put("speed", rule.speed());
                }
                if (rule.correctForDrops() != TriState.NOT_SET) {
                    ruleMap.put("correct_for_drops", rule.correctForDrops().toBoolean());
                }
                rulesList.add(ruleMap);
            }
            section.set("rules", rulesList);
        }
    }
}
