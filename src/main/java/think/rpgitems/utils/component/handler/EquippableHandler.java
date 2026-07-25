package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

import java.util.List;
import java.util.Objects;

/**
 * equippable 组件。
 * 修复原实现的 bug：{@code equip_on_interaction} 读出来后误调用了
 * {@code builder.swappable(equipOnInteract)}，把前面刚设置的 swappable 值覆盖掉了，
 * 应该调用 {@code builder.equipOnInteract(...)}。
 */
@SuppressWarnings("PatternValidation")
public class EquippableHandler implements ComponentHandler<Equippable> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.EQUIPPABLE;
    }

    @Override
    public String yamlKey() {
        return "equippable";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        String slot = section.getString("slot");
        if (slot == null) {
            throw new IllegalArgumentException("Missing required 'slot' field in equippable configuration.");
        }
        Equippable.Builder builder = Equippable.equippable(EquipmentSlot.valueOf(slot.toUpperCase()));

        if (section.contains("allowed_entities")) {
            List<String> entities = section.getStringList("allowed_entities");
            if (!entities.isEmpty()) {
                List<EntityType> types = entities.stream()
                        .map(e -> {
                            @Subst("player") String entity = e;
                            return RegistryAccess.registryAccess()
                                    .getRegistry(RegistryKey.ENTITY_TYPE).get(Key.key(entity));
                        })
                        .filter(Objects::nonNull)
                        .toList();
                if (!types.isEmpty()) {
                    RegistryKeySet<EntityType> allowedEntities =
                            RegistrySet.keySetFromValues(RegistryKey.ENTITY_TYPE, types);
                    builder.allowedEntities(allowedEntities);
                }
            }
        }

        String assetId = section.getString("asset_id");
        if (assetId != null) {
            builder.assetId(Key.key(assetId));
        }

        String cameraOverlay = section.getString("camera_overlay");
        if (cameraOverlay != null) {
            builder.cameraOverlay(Key.key(cameraOverlay));
        }

        builder.damageOnHurt(section.getBoolean("damage_on_hurt", false));
        builder.dispensable(section.getBoolean("dispensable", false));

        String equipSound = section.getString("equip_sound");
        if (equipSound != null) {
            builder.equipSound(Key.key(equipSound));
        }

        builder.swappable(section.getBoolean("swappable", true));
        // 修复：原来这里错误地又调用了一次 builder.swappable(...)，覆盖了上一行的值
        builder.equipOnInteract(section.getBoolean("equip_on_interaction", false));

        builder.canBeSheared(section.getBoolean("can_be_sheared", false));

        String shearingSound = section.getString("shearing_sound");
        if (shearingSound != null) {
            builder.shearSound(Key.key(shearingSound));
        }
        return builder;
    }

    @Override
    public void encode(Equippable value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("slot", value.slot().name());

        RegistryKeySet<EntityType> allowedEntities = value.allowedEntities();
        if (allowedEntities != null && !allowedEntities.values().isEmpty()) {
            section.set("allowed_entities", allowedEntities.values().stream()
                    .map(e -> e.key().asString()).toList());
        }
        if (value.assetId() != null) {
            section.set("asset_id", value.assetId().asString());
        }
        if (value.cameraOverlay() != null) {
            section.set("camera_overlay", value.cameraOverlay().asString());
        }
        section.set("damage_on_hurt", value.damageOnHurt());
        section.set("dispensable", value.dispensable());
        section.set("equip_sound", value.equipSound().asString());
        section.set("swappable", value.swappable());
        section.set("equip_on_interaction", value.equipOnInteract());
        section.set("can_be_sheared", value.canBeSheared());
        section.set("shearing_sound", value.shearSound().asString());
    }
}
