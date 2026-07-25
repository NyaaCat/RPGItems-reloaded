package think.rpgitems.utils.component;

import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.intellij.lang.annotations.Subst;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * on_consume_effects / death_effects 的解析与序列化，
 * ConsumableHandler 和 DeathProtectionHandler 共用。
 *
 * <p>YAML 格式为列表（getMapList），与原版数据包的写法一致：
 * <pre>
 * on_consume_effects:
 * - type: apply_effects
 *   probability: 0.8
 *   effects:
 *   - id: speed
 *     amplifier: 1
 *     duration: 100
 * - type: clear_all_effects
 * - type: teleport_randomly
 *   diameter: 20.0
 * </pre>
 * 注意 probability 在 apply_effects 这一层（一组效果共享一个概率），
 * 与原版组件结构一致；旧的 effect_1/effect_2 命名子段格式不再支持。
 */
@SuppressWarnings("PatternValidation")
public final class ConsumeEffectYaml {

    private ConsumeEffectYaml() {
    }

    /**
     * 从 owner 段的 path 处读取效果列表（getMapList）。
     *
     * @param owner consumable / death_protection 段
     * @param path  "on_consume_effects" 或 "death_effects"
     * @return 解析出的效果列表，无配置时为空列表
     */
    public static List<ConsumeEffect> parse(ConfigurationSection owner, String path) {
        List<ConsumeEffect> result = new ArrayList<>();
        for (Map<?, ?> effectData : owner.getMapList(path)) {
            Object type = effectData.get("type");
            if (!(type instanceof String typeStr)) {
                continue;
            }
            switch (typeStr) {
                case "apply_effects" -> {
                    List<PotionEffect> potionEffects = new ArrayList<>();
                    if (effectData.get("effects") instanceof List<?> effList) {
                        for (Object o : effList) {
                            if (!(o instanceof Map<?, ?> effMap)) {
                                continue;
                            }
                            PotionEffect effect = parsePotionEffect(effMap);
                            if (effect != null) {
                                potionEffects.add(effect);
                            }
                        }
                    }
                    if (!potionEffects.isEmpty()) {
                        float probability = ComponentYamlUtil.mapFloat(effectData, "probability", 1.0f);
                        result.add(ConsumeEffect.applyStatusEffects(potionEffects, probability));
                    }
                }
                case "clear_all_effects" -> result.add(ConsumeEffect.clearAllStatusEffects());
                case "play_sound" -> {
                    if (effectData.get("sound") instanceof String s) {
                        @Subst("entity.player.burp") String sound = s;
                        result.add(ConsumeEffect.playSoundConsumeEffect(Key.key(sound)));
                    }
                }
                case "remove_effects" -> {
                    List<PotionEffectType> effectsList = new ArrayList<>();
                    if (effectData.get("effects") instanceof List<?> effList) {
                        for (Object o : effList) {
                            if (!(o instanceof String s)) {
                                continue;
                            }
                            @Subst("speed") String effect = s;
                            PotionEffectType effectType = RegistryAccess.registryAccess()
                                    .getRegistry(RegistryKey.MOB_EFFECT).get(Key.key(effect));
                            if (effectType != null) {
                                effectsList.add(effectType);
                            }
                        }
                    }
                    if (!effectsList.isEmpty()) {
                        RegistryKeySet<PotionEffectType> keySet =
                                RegistrySet.keySetFromValues(RegistryKey.MOB_EFFECT, effectsList);
                        result.add(ConsumeEffect.removeEffects(keySet));
                    }
                }
                case "teleport_randomly" ->
                        result.add(ConsumeEffect.teleportRandomlyEffect(ComponentYamlUtil.mapFloat(effectData, "diameter", 16.0f)));
                default -> {
                }
            }
        }
        return result;
    }

    /**
     * parse 的对称操作：把效果列表写到 owner 段的 path 处（列表形式）。
     * 列表为空时不写任何内容。
     */
    public static void serialize(List<ConsumeEffect> effects, ConfigurationSection owner, String path) {
        if (effects.isEmpty()) {
            return;
        }
        List<Map<String, Object>> list = new ArrayList<>(effects.size());
        for (ConsumeEffect effect : effects) {
            Map<String, Object> map = new LinkedHashMap<>();
            switch (effect) {
                case ConsumeEffect.ApplyStatusEffects applyEffect -> {
                    map.put("type", "apply_effects");
                    map.put("probability", applyEffect.probability());
                    List<Map<String, Object>> effList = new ArrayList<>();
                    for (PotionEffect potionEffect : applyEffect.effects()) {
                        Map<String, Object> effMap = new LinkedHashMap<>();
                        effMap.put("id", potionEffect.getType().getKey().asString());
                        effMap.put("amplifier", potionEffect.getAmplifier());
                        effMap.put("duration", potionEffect.getDuration());
                        effList.add(effMap);
                    }
                    map.put("effects", effList);
                }
                case ConsumeEffect.ClearAllStatusEffects ignored -> map.put("type", "clear_all_effects");
                case ConsumeEffect.PlaySound playSound -> {
                    map.put("type", "play_sound");
                    map.put("sound", playSound.sound().asString());
                }
                case ConsumeEffect.RemoveStatusEffects removeEffects -> {
                    map.put("type", "remove_effects");
                    List<String> effectKeys = new ArrayList<>();
                    for (TypedKey<PotionEffectType> effectType : removeEffects.removeEffects()) {
                        effectKeys.add(effectType.key().asString());
                    }
                    map.put("effects", effectKeys);
                }
                case ConsumeEffect.TeleportRandomly teleport -> {
                    map.put("type", "teleport_randomly");
                    map.put("diameter", teleport.diameter());
                }
                default -> {
                }
            }
            if (!map.isEmpty()) {
                list.add(map);
            }
        }
        owner.set(path, list);
    }

    private static PotionEffect parsePotionEffect(Map<?, ?> effMap) {
        if (!(effMap.get("id") instanceof String idStr)) {
            return null;
        }
        @Subst("speed") String id = idStr;
        PotionEffectType potionEffectType = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.MOB_EFFECT).get(Key.key(id));
        if (potionEffectType == null) {
            return null;
        }
        int amplifier = ComponentYamlUtil.mapInt(effMap, "amplifier", 0);
        int duration = ComponentYamlUtil.mapInt(effMap, "duration", 200);
        return new PotionEffect(potionEffectType, duration, amplifier);
    }
}
