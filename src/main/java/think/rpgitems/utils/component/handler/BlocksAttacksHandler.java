package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import io.papermc.paper.datacomponent.item.blocksattacks.ItemDamageFunction;
import io.papermc.paper.registry.set.RegistryKeySet;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.damage.DamageType;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ComponentYamlUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * blocks_attacks 组件。damage_reductions 为列表格式（getMapList）：
 * <pre>
 * blocks_attacks:
 *   block_delay_seconds: 0
 *   bypassed_by: "#is_explosion"
 *   damage_reductions:
 *   - base: 1
 *     factor: 1
 *     horizontal_blocking_angle: 30
 *     type: "#is_fall"
 *   - base: 0.5
 *     factor: 1
 *     type:
 *     - on_fire
 *     - in_fire
 *   item_damage:
 *     base: 10
 *     factor: 10
 *     threshold: 10
 * </pre>
 * bypassed_by 与 type 支持单个 "#tag" 或纯 ID 列表（不可混用），
 * 序列化时 tag 会原样写回 "#tag" 形式。
 */
public class BlocksAttacksHandler implements ComponentHandler<BlocksAttacks> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.BLOCKS_ATTACKS;
    }

    @Override
    public String yamlKey() {
        return "blocks_attacks";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        BlocksAttacks.Builder builder = BlocksAttacks.blocksAttacks();

        builder.blockDelaySeconds((float) section.getDouble("block_delay_seconds", 0));
        @Subst("item.shield.break") String sound = section.getString("block_sound", "minecraft:item.shield.break");
        builder.blockSound(Key.key(sound));
        @Subst("item.shield.break") String disableSound = section.getString("disable_sound", "minecraft:item.shield.break");
        builder.disableSound(Key.key(disableSound));
        builder.disableCooldownScale((float) section.getDouble("disable_cooldown_scale", 1.0));

        ConfigurationSection itemDamageSection = section.getConfigurationSection("item_damage");
        if (itemDamageSection != null) {
            builder.itemDamage(ItemDamageFunction.itemDamageFunction()
                    .base(itemDamageSection.getInt("base", 1))
                    .factor(itemDamageSection.getInt("factor", 1))
                    .threshold(itemDamageSection.getInt("threshold", 1))
                    .build());
        }

        List<String> bypassedBy = ComponentYamlUtil.stringOrList(section, "bypassed_by");
        if (!bypassedBy.isEmpty()) {
            builder.bypassedBy(ComponentYamlUtil.damageTypeSet(bypassedBy));
        }

        List<DamageReduction> reductions = new ArrayList<>();
        for (Map<?, ?> reductionData : section.getMapList("damage_reductions")) {
            DamageReduction.Builder reduction = DamageReduction.damageReduction()
                    .base(ComponentYamlUtil.mapFloat(reductionData, "base", 1))
                    .factor(ComponentYamlUtil.mapFloat(reductionData, "factor", 1))
                    .horizontalBlockingAngle(
                            ComponentYamlUtil.mapFloat(reductionData, "horizontal_blocking_angle", 90));
            List<String> types = ComponentYamlUtil.mapStringOrList(reductionData, "type");
            if (!types.isEmpty()) {
                reduction.type(ComponentYamlUtil.damageTypeSet(types));
            }
            reductions.add(reduction.build());
        }
        if (!reductions.isEmpty()) {
            builder.damageReductions(reductions);
        }
        return builder;
    }

    @Override
    public void encode(BlocksAttacks value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());

        section.set("block_delay_seconds", value.blockDelaySeconds());
        section.set("disable_cooldown_scale", value.disableCooldownScale());
        if (value.blockSound() != null) {
            section.set("block_sound", value.blockSound().asString());
        }
        if (value.disableSound() != null) {
            section.set("disable_sound", value.disableSound().asString());
        }

        RegistryKeySet<DamageType> bypassedBy = value.bypassedBy();
        if (bypassedBy != null) {
            ComponentYamlUtil.setDamageTypeSet(section, "bypassed_by", bypassedBy);
        }

        ItemDamageFunction itemDamage = value.itemDamage();
        ConfigurationSection itemDamageSection = section.createSection("item_damage");
        itemDamageSection.set("base", itemDamage.base());
        itemDamageSection.set("factor", itemDamage.factor());
        itemDamageSection.set("threshold", itemDamage.threshold());

        List<DamageReduction> reductions = value.damageReductions();
        if (!reductions.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>(reductions.size());
            for (DamageReduction reduction : reductions) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("base", reduction.base());
                map.put("factor", reduction.factor());
                map.put("horizontal_blocking_angle", reduction.horizontalBlockingAngle());
                RegistryKeySet<DamageType> typeSet = reduction.type();
                if (typeSet != null) {
                    Object types = ComponentYamlUtil.damageTypeSetToYaml(typeSet);
                    if (types != null) {
                        map.put("type", types);
                    }
                }
                list.add(map);
            }
            section.set("damage_reductions", list);
        }
    }
}
