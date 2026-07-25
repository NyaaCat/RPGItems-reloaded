package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.SuspiciousStewEffects;
import io.papermc.paper.potion.SuspiciousEffectEntry;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;
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
 * suspicious_stew_effects:
 * - id: speed
 *   duration: 200
 */
@SuppressWarnings("PatternValidation")
public class SuspiciousStewEffectsHandler implements ComponentHandler<SuspiciousStewEffects> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.SUSPICIOUS_STEW_EFFECTS;
    }

    @Override
    public String yamlKey() {
        return "suspicious_stew_effects";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        List<Map<?, ?>> effects = parent.getMapList(yamlKey());
        if (effects.isEmpty()) {
            return null;
        }
        SuspiciousStewEffects.Builder builder = SuspiciousStewEffects.suspiciousStewEffects();
        for (Map<?, ?> effectData : effects) {
            if (!(effectData.get("id") instanceof String idStr)) {
                continue;
            }
            @Subst("speed") String id = idStr;
            PotionEffectType effectType = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.MOB_EFFECT).get(Key.key(id));
            if (effectType == null) {
                continue;
            }
            int duration = ComponentYamlUtil.mapInt(effectData, "duration", 160);
            builder.add(SuspiciousEffectEntry.create(effectType, duration));
        }
        return builder;
    }

    @Override
    public void encode(SuspiciousStewEffects value, ConfigurationSection config) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (SuspiciousEffectEntry entry : value.effects()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", entry.effect().key().asString());
            map.put("duration", entry.duration());
            list.add(map);
        }
        config.set(yamlKey(), list);
    }
}
