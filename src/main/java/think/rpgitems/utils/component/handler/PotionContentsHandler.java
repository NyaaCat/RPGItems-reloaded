package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;
import think.rpgitems.utils.component.ComponentYamlUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PotionContentsHandler implements ComponentHandler<PotionContents> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.POTION_CONTENTS;
    }

    @Override
    public String yamlKey() {
        return "potion_contents";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        PotionContents.Builder builder = PotionContents.potionContents();

        String colorValue = section.getString("custom_color");
        if (colorValue != null) {
            builder.customColor(ComponentYamlUtil.parseColor(colorValue));
        }
        if (section.getString("custom_name") != null) {
            builder.customName(section.getString("custom_name", ""));
        }
        if (section.getString("potion") != null) {
            builder.potion(PotionType.valueOf(section.getString("potion", "awkward").toUpperCase()));
        }

        for (Map<?, ?> effectData : section.getMapList("custom_effects")) {
            if (!(effectData.get("id") instanceof String idStr)) {
                continue;
            }
            @Subst("speed") String id = idStr;
            PotionEffectType potionEffectType = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.MOB_EFFECT).get(Key.key(id));
            if (potionEffectType == null) {
                continue;
            }
            int amplifier = ComponentYamlUtil.mapInt(effectData, "amplifier", 0);
            int duration = ComponentYamlUtil.mapInt(effectData, "duration", 200);
            boolean ambient = ComponentYamlUtil.mapBoolean(effectData, "ambient", false);
            boolean showParticles = ComponentYamlUtil.mapBoolean(effectData, "show_particles", true);
            boolean showIcon = ComponentYamlUtil.mapBoolean(effectData, "show_icon", true);
            builder.addCustomEffect(new PotionEffect(potionEffectType, duration, amplifier, ambient, showParticles, showIcon));
        }
        return builder;
    }

    @Override
    public void encode(PotionContents value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());

        Color color = value.customColor();
        if (color != null) {
            section.set("custom_color", ComponentYamlUtil.colorToString(color));
        }
        if (value.customName() != null) {
            section.set("custom_name", value.customName());
        }
        if (value.potion() != null) {
            section.set("potion", value.potion().getKey().getKey().toUpperCase());
        }

        List<Map<String, Object>> effectList = new ArrayList<>();
        for (PotionEffect effect : value.customEffects()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", effect.getType().key().asString());
            map.put("amplifier", effect.getAmplifier());
            map.put("duration", effect.getDuration());
            map.put("ambient", effect.isAmbient());
            map.put("show_particles", effect.hasParticles());
            map.put("show_icon", effect.hasIcon());
            effectList.add(map);
        }
        section.set("custom_effects", effectList);
    }
}
