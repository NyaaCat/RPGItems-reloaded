package think.rpgitems.utils;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.block.BlockPredicate;
import io.papermc.paper.datacomponent.DataComponentBuilder;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.*;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import io.papermc.paper.datacomponent.item.blocksattacks.ItemDamageFunction;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.util.TriState;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import think.rpgitems.RPGItems;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@SuppressWarnings({"PatternValidation", "NullableProblems"})
public class ComponentUtil {
    public static enum ComponentStatus {
        UNSET,
        NON_VALUED
    }

    public static List<Map<DataComponentType, Object>> getComponents(ConfigurationSection s) {
        return getComponents(s, null);
    }

    public static List<Map<DataComponentType, Object>> getComponents(ConfigurationSection s, RPGItem item) {
        String itemName = item == null ? "" : ":" + item.getName();
        List<Map<DataComponentType, Object>> components = new ArrayList<>();
        if (!s.getKeys(false).isEmpty()) {
            for (String key : s.getKeys(false)) {
                Map<DataComponentType, Object> componentMap = new HashMap<>();
                switch (key.toLowerCase()) {
                    case "banner_patterns": {
                        BannerPatternLayers.Builder builder = BannerPatternLayers.bannerPatternLayers();
                        ConfigurationSection bannerSection = s.getConfigurationSection(key);
                        if (bannerSection != null) {
                            if (bannerSection.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.BANNER_PATTERNS, ComponentStatus.UNSET);
                            } else {
                                for (String layerKey : bannerSection.getKeys(false)) {
                                    ConfigurationSection layerSection = bannerSection.getConfigurationSection(layerKey);
                                    if (layerSection != null) {
                                        String color = layerSection.getString("color");
                                        @Subst("base") String pattern = layerSection.getString("pattern");
                                        if (color != null && pattern != null) {
                                            PatternType type = RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).get(Key.key(pattern));
                                            if (type != null) {
                                                builder.add(new Pattern(DyeColor.valueOf(color.toUpperCase()), type));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        componentMap.put(DataComponentTypes.BANNER_PATTERNS, builder);
                    }
                    break;
                    case "blocks_attacks": {
                        BlocksAttacks.Builder builder = BlocksAttacks.blocksAttacks();
                        ConfigurationSection blocksSection = s.getConfigurationSection(key);
                        if (blocksSection != null) {
                            if (blocksSection.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.BLOCKS_ATTACKS, ComponentStatus.UNSET);
                            } else {
                                float blockDelaySeconds = (float) blocksSection.getDouble("block_delay_seconds", 0);
                                builder.blockDelaySeconds(blockDelaySeconds);
                                @Subst("item.shield.break") String sound = blocksSection.getString("block_sound", "minecraft:item.shield.break");
                                builder.blockSound(Key.key(sound));
                                @Subst("item.shield.break") String disableSound = blocksSection.getString("disable_sound", "minecraft:item.shield.break");
                                builder.disableSound(Key.key(disableSound));
                                if (blocksSection.isString(key)) {
                                    builder.bypassedBy(TagKey.create(RegistryKey.DAMAGE_TYPE, Key.key(blocksSection.getString(key))));
                                }
                                float disableCooldownScale = (float) blocksSection.getDouble("disable_cooldown_scale", 1.0);
                                builder.disableCooldownScale(disableCooldownScale);
                                ConfigurationSection itemDamageSection = blocksSection.getConfigurationSection("item_damage");
                                if (itemDamageSection != null) {
                                    int base = itemDamageSection.getInt("base", 1);
                                    int factor = itemDamageSection.getInt("factor", 1);
                                    int threshold = itemDamageSection.getInt("threshold", 1);
                                    ItemDamageFunction.Builder itemDamage = ItemDamageFunction.itemDamageFunction().factor(factor);
                                    itemDamage.base(base);
                                    itemDamage.threshold(threshold);
                                    builder.itemDamage(itemDamage.build());
                                }
                                ConfigurationSection reductionsSection = blocksSection.getConfigurationSection("damage_reductions");
                                if (reductionsSection != null) {
                                    List<DamageReduction> reductions = new ArrayList<>();

                                    for (String reductionKey : reductionsSection.getKeys(false)) {
                                        ConfigurationSection reductionSection = reductionsSection.getConfigurationSection(reductionKey);
                                        if (reductionSection != null) {
                                            DamageReduction.Builder reduction = DamageReduction.damageReduction().base((float) reductionSection.getDouble("base", 1));
                                            if (reductionSection.contains("type")) {
                                                if (reductionSection.isString("type")) {
                                                    String typeStr = reductionSection.getString("type");
                                                    if (typeStr != null) {
                                                        RegistryKeySet<DamageType> tagSet = RegistrySet.keySet(RegistryKey.DAMAGE_TYPE, TypedKey.create(RegistryKey.DAMAGE_TYPE, Key.key(typeStr)));
                                                        reduction.type(tagSet);
                                                    }
                                                } else if (reductionSection.isList("type")) {
                                                    List<String> types = reductionSection.getStringList("type");
                                                    if (!types.isEmpty()) {
                                                        List<TypedKey<DamageType>> damageTypes = types.stream()
                                                                .map(type -> TypedKey.create(RegistryKey.DAMAGE_TYPE, Key.key(type)))
                                                                .collect(Collectors.toList());
                                                        RegistryKeySet<DamageType> tagSet = RegistrySet.keySet(RegistryKey.DAMAGE_TYPE, damageTypes);
                                                        reduction.type(tagSet);
                                                    }
                                                }
                                            }

                                            reductions.add(reduction.build());
                                        }
                                    }

                                    builder.damageReductions(reductions);
                                }
                                componentMap.put(DataComponentTypes.BLOCKS_ATTACKS, builder);
                            }
                        }
                    }
                    break;
                    case "break_sound": {
                        ConfigurationSection section = s.getConfigurationSection(key);
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.BREAK_SOUND, ComponentStatus.UNSET);
                            } else {
                                @Subst("entity.item.break") String sound = section.getString("break_sound", "minecraft:entity.item.break");
                                componentMap.put(DataComponentTypes.BREAK_SOUND, Key.key(sound));
                            }
                        }
                    }
                    break;
                    case "consumable": {
                        Consumable.Builder builder = Consumable.consumable();
                        ConfigurationSection consumableSection = s.getConfigurationSection(key);
                        if (consumableSection != null) {
                            if (consumableSection.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.CONSUMABLE, ComponentStatus.UNSET);
                            } else {
                                String animation = consumableSection.getString("animation", "eat");
                                builder.animation(ItemUseAnimation.valueOf(animation.toUpperCase()));
                                double consumeSeconds = consumableSection.getDouble("consume_seconds", 1.6);
                                builder.consumeSeconds((float) consumeSeconds);
                                boolean hasConsumeParticles = consumableSection.getBoolean("has_consume_particles", true);
                                builder.hasConsumeParticles(hasConsumeParticles);
                                @Subst("entity.generic.eat") String sound = consumableSection.getString("sound", "minecraft:entity.generic.eat");
                                builder.sound(Key.key(sound));
                                if (consumableSection.contains("on_consume_effects")) {
                                    ConfigurationSection effectsSection = consumableSection.getConfigurationSection("on_consume_effects");
                                    if (effectsSection != null) {
                                        for (String effectKey : effectsSection.getKeys(false)) {
                                            ConfigurationSection effectSection = effectsSection.getConfigurationSection(effectKey);
                                            if (effectSection != null) {
                                                String type = effectSection.getString("type");
                                                if (type == null) continue;

                                                switch (type) {
                                                    case "apply_effects":
                                                        List<ConsumeEffect> effects = new ArrayList<>();
                                                        if (effectSection.contains("effects")) {
                                                            for (String effKey : effectSection.getConfigurationSection("effects").getKeys(false)) {
                                                                ConfigurationSection effSection = effectSection.getConfigurationSection("effects." + effKey);
                                                                if (effSection != null) {
                                                                    @Subst("speed") String id = effSection.getString("id");
                                                                    int amplifier = effSection.getInt("amplifier", 0);
                                                                    int duration = effSection.getInt("duration", 200);
                                                                    double probability = effSection.getDouble("probability", 1.0);
                                                                    if (id != null) {
                                                                        PotionEffectType potionEffectType = RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).get(Key.key(id));
                                                                        if (potionEffectType != null) {
                                                                            effects.add(ConsumeEffect.applyStatusEffects(List.of(new PotionEffect(potionEffectType, duration, amplifier)), (float) probability));
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        builder.addEffects(effects);
                                                        break;

                                                    case "clear_all_effects":
                                                        builder.addEffect(ConsumeEffect.clearAllStatusEffects());
                                                        break;

                                                    case "play_sound":
                                                        @Subst("entity.player.burp") String playSound = effectSection.getString("sound");
                                                        if (playSound != null) {
                                                            builder.addEffect(ConsumeEffect.playSoundConsumeEffect(Key.key(playSound)));
                                                        }
                                                        break;

                                                    case "remove_effects":
                                                        List<String> removeEffects = effectSection.getStringList("effects");
                                                        if (!removeEffects.isEmpty()) {
                                                            List<PotionEffectType> effectsList = new ArrayList<>();
                                                            for (@Subst("speed") String effect : removeEffects) {
                                                                PotionEffectType effectType = RegistryAccess.registryAccess()
                                                                        .getRegistry(RegistryKey.MOB_EFFECT)
                                                                        .get(Key.key(effect));
                                                                if (effectType != null) {
                                                                    effectsList.add(effectType);
                                                                }
                                                            }
                                                            if (!effectsList.isEmpty()) {
                                                                RegistryKeySet<PotionEffectType> keySet = RegistrySet.keySetFromValues(
                                                                        RegistryKey.MOB_EFFECT, effectsList
                                                                );
                                                                builder.addEffect(ConsumeEffect.removeEffects(keySet));
                                                            }
                                                        }
                                                        break;

                                                    case "teleport_randomly":
                                                        double diameter = effectSection.getDouble("diameter", 16.0);
                                                        builder.addEffect(ConsumeEffect.teleportRandomlyEffect((float) diameter));
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        componentMap.put(DataComponentTypes.CONSUMABLE, builder);
                    }
                    break;
                    case "can_place_on":
                    case "can_break": {
                        ItemAdventurePredicate.Builder builder = ItemAdventurePredicate.itemAdventurePredicate();
                        ConfigurationSection section = s.getConfigurationSection(key);
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                if (key.equalsIgnoreCase("can_place_on")) {
                                    componentMap.put(DataComponentTypes.CAN_PLACE_ON, ComponentStatus.UNSET);
                                } else {
                                    componentMap.put(DataComponentTypes.CAN_BREAK, ComponentStatus.UNSET);
                                }
                            }
                        } else {
                            List<String> blockTypes = s.getStringList(key);
                            if (!blockTypes.isEmpty()) {
                                List<BlockType> blocks = new ArrayList<>();
                                for (@Subst("stone") String block : blockTypes) {
                                    BlockType blockType = RegistryAccess.registryAccess()
                                            .getRegistry(RegistryKey.BLOCK)
                                            .get(Key.key(block));
                                    if (blockType != null) {
                                        blocks.add(blockType);
                                    }
                                }

                                if (!blocks.isEmpty()) {
                                    RegistryKeySet<BlockType> blockKeySet = RegistrySet.keySetFromValues(
                                            RegistryKey.BLOCK, blocks
                                    );
                                    BlockPredicate blockPredicate = BlockPredicate.predicate()
                                            .blocks(blockKeySet)
                                            .build();
                                    builder.addPredicate(blockPredicate);
                                }
                            }


                            if (key.equalsIgnoreCase("can_place_on")) {
                                componentMap.put(DataComponentTypes.CAN_PLACE_ON, builder);
                            } else {
                                componentMap.put(DataComponentTypes.CAN_BREAK, builder);
                            }
                        }
                    }
                    break;
                    case "damage_resistance": {
                        ConfigurationSection section = s.getConfigurationSection(key);
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.DAMAGE_RESISTANT, ComponentStatus.UNSET);
                            } else {
                                if (s.getString(key) != null) {
                                    componentMap.put(DataComponentTypes.DAMAGE_RESISTANT, DamageResistant.damageResistant(TagKey.create(RegistryKey.DAMAGE_TYPE, Key.key(section.getString(key)))));
                                }
                            }
                        }
                    }
                    break;
                    case "death_protection": {
                        DeathProtection.Builder builder = DeathProtection.deathProtection();
                        ConfigurationSection deathSection = s.getConfigurationSection(key);
                        if (deathSection != null) {
                            if (deathSection.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.DEATH_PROTECTION, ComponentStatus.UNSET);
                            } else if (deathSection.contains("death_effects")) {
                                ConfigurationSection effectsSection = deathSection.getConfigurationSection("death_effects");
                                if (effectsSection != null) {
                                    for (String effectKey : effectsSection.getKeys(false)) {
                                        ConfigurationSection effectSection = effectsSection.getConfigurationSection(effectKey);
                                        if (effectSection != null) {
                                            String type = effectSection.getString("type");
                                            if (type == null) continue;

                                            switch (type) {
                                                case "apply_effects":
                                                    List<ConsumeEffect> effects = new ArrayList<>();
                                                    if (effectSection.contains("effects")) {
                                                        for (String effKey : effectSection.getConfigurationSection("effects").getKeys(false)) {
                                                            ConfigurationSection effSection = effectSection.getConfigurationSection("effects." + effKey);
                                                            if (effSection != null) {
                                                                @Subst("speed") String id = effSection.getString("id");
                                                                int amplifier = effSection.getInt("amplifier", 0);
                                                                int duration = effSection.getInt("duration", 200);
                                                                double probability = effSection.getDouble("probability", 1.0);
                                                                if (id != null) {
                                                                    PotionEffectType potionEffectType = RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).get(Key.key(id));
                                                                    if (potionEffectType != null) {
                                                                        effects.add(ConsumeEffect.applyStatusEffects(List.of(new PotionEffect(potionEffectType, duration, amplifier)), (float) probability));
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    builder.addEffects(effects);
                                                    break;

                                                case "clear_all_effects":
                                                    builder.addEffect(ConsumeEffect.clearAllStatusEffects());
                                                    break;

                                                case "play_sound":
                                                    @Subst("entity.player.burp") String playSound = effectSection.getString("sound");
                                                    if (playSound != null) {
                                                        builder.addEffect(ConsumeEffect.playSoundConsumeEffect(Key.key(playSound)));
                                                    }
                                                    break;

                                                case "remove_effects":
                                                    List<String> removeEffects = effectSection.getStringList("effects");
                                                    if (!removeEffects.isEmpty()) {
                                                        List<PotionEffectType> effectsList = new ArrayList<>();
                                                        for (@Subst("speed") String effect : removeEffects) {
                                                            PotionEffectType effectType = RegistryAccess.registryAccess()
                                                                    .getRegistry(RegistryKey.MOB_EFFECT)
                                                                    .get(Key.key(effect));
                                                            if (effectType != null) {
                                                                effectsList.add(effectType);
                                                            }
                                                        }
                                                        if (!effectsList.isEmpty()) {
                                                            RegistryKeySet<PotionEffectType> keySet = RegistrySet.keySetFromValues(
                                                                    RegistryKey.MOB_EFFECT, effectsList
                                                            );
                                                            builder.addEffect(ConsumeEffect.removeEffects(keySet));
                                                        }
                                                    }
                                                    break;

                                                case "teleport_randomly":
                                                    double diameter = effectSection.getDouble("diameter", 16.0);
                                                    builder.addEffect(ConsumeEffect.teleportRandomlyEffect((float) diameter));
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        componentMap.put(DataComponentTypes.DEATH_PROTECTION, builder);
                    }
                    break;
                    case "dyed_color": {
                        DyedItemColor.Builder builder = DyedItemColor.dyedItemColor();
                        ConfigurationSection dyedSection = s.getConfigurationSection(key);
                        if (dyedSection != null) {
                            if (dyedSection.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.DYED_COLOR, ComponentStatus.UNSET);
                            }
                        } else {
                            String colorValue = s.getString(key);
                            if (colorValue != null) {
                                Color color;
                                if (colorValue.contains(",")) {
                                    String[] rgbParts = colorValue.split(",");
                                    if (rgbParts.length == 3) {
                                        try {
                                            int red = Integer.parseInt(rgbParts[0].trim());
                                            int green = Integer.parseInt(rgbParts[1].trim());
                                            int blue = Integer.parseInt(rgbParts[2].trim());
                                            color = Color.fromRGB(red, green, blue);
                                        } catch (NumberFormatException e) {
                                            throw new IllegalArgumentException("Invalid RGB format: " + colorValue);
                                        }
                                    } else {
                                        throw new IllegalArgumentException("Invalid RGB format, expected 3 values: " + colorValue);
                                    }
                                } else {
                                    try {
                                        int rgb = Integer.parseInt(colorValue.trim());
                                        color = Color.fromRGB(rgb);
                                    } catch (NumberFormatException e) {
                                        throw new IllegalArgumentException("Invalid color value: " + colorValue);
                                    }
                                }
                                builder.color(color);
                            }
                        }
                        componentMap.put(DataComponentTypes.DYED_COLOR, builder);
                    }
                    break;
                    case "enchantable": {
                        if (s.isConfigurationSection(key)) {
                            if (s.getConfigurationSection(key).getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.ENCHANTABLE, ComponentStatus.UNSET);
                            } else if (s.getInt(key) >= 1) {
                                componentMap.put(DataComponentTypes.ENCHANTABLE, Enchantable.enchantable(s.getInt(key)));
                            }
                        }
                    }
                    break;
                    case "enchantment_glint_override": {
                        if (s.isConfigurationSection(key)) {
                            if (s.getConfigurationSection(key).getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, ComponentStatus.UNSET);
                            }
                        } else if (s.isBoolean(key)) {
                            componentMap.put(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, s.getBoolean(key));
                        }
                    }
                    break;
                    case "equippable": {
                        Equippable.Builder builder = Equippable.equippable(EquipmentSlot.CHEST);
                        ConfigurationSection equippableSection = s.getConfigurationSection(key);
                        if (equippableSection != null) {
                            if (equippableSection.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.EQUIPPABLE, ComponentStatus.UNSET);
                            } else {
                                String slot = equippableSection.getString("slot");
                                if (slot == null) {
                                    throw new IllegalArgumentException("Missing required 'slot' field in equippable configuration.");
                                }
                                builder = Equippable.equippable(EquipmentSlot.valueOf(slot.toUpperCase()));
                                if (equippableSection.contains("allowed_entities")) {
                                    List<String> entities = equippableSection.getStringList("allowed_entities");
                                    if (!entities.isEmpty()) {
                                        RegistryKeySet<EntityType> allowedEntities = RegistrySet.keySetFromValues(
                                                RegistryKey.ENTITY_TYPE,
                                                entities.stream()
                                                        .map(entity -> RegistryAccess.registryAccess()
                                                                .getRegistry(RegistryKey.ENTITY_TYPE)
                                                                .get(Key.key(entity)))
                                                        .filter(Objects::nonNull)
                                                        .toList()
                                        );
                                        builder.allowedEntities(allowedEntities);
                                    }
                                }

                                String assetId = equippableSection.getString("asset_id");
                                if (assetId != null) {
                                    builder.assetId(Key.key(assetId));
                                }

                                String cameraOverlay = equippableSection.getString("camera_overlay");
                                if (cameraOverlay != null) {
                                    builder.cameraOverlay(Key.key(cameraOverlay));
                                }

                                boolean damageOnHurt = equippableSection.getBoolean("damage_on_hurt", false);
                                builder.damageOnHurt(damageOnHurt);

                                boolean dispensable = equippableSection.getBoolean("dispensable", false);
                                builder.dispensable(dispensable);

                                String equipSound = equippableSection.getString("equip_sound");
                                if (equipSound != null) {
                                    builder.equipSound(Key.key(equipSound));
                                }

                                boolean swappable = equippableSection.getBoolean("swappable", true);
                                builder.swappable(swappable);

                                boolean equipOnInteract = equippableSection.getBoolean("equip_on_interaction", false);
                                builder.swappable(equipOnInteract);
                            }
                        }
                        componentMap.put(DataComponentTypes.EQUIPPABLE, builder);
                    }
                    break;
                    case "food": {
                        FoodProperties.Builder builder = FoodProperties.food();
                        ConfigurationSection foodSection = s.getConfigurationSection(key);
                        if (foodSection != null) {
                            if (foodSection.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.FOOD, ComponentStatus.UNSET);
                            } else {
                                boolean canAlwaysEat = foodSection.getBoolean("can_always_eat", false);
                                builder.canAlwaysEat(canAlwaysEat);

                                int nutrition = Math.max(foodSection.getInt("nutrition"), 0);
                                builder.nutrition(nutrition);

                                float saturation = (float) foodSection.getDouble("saturation", 0.0);
                                builder.saturation(saturation);
                            }
                        }
                        componentMap.put(DataComponentTypes.FOOD, builder);
                    }
                    break;
                    case "glider": {
                        ConfigurationSection gliderSection = s.getConfigurationSection(key);
                        if (gliderSection != null) {
                            if (gliderSection.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.GLIDER, ComponentStatus.UNSET);
                            }
                        } else if (s.getBoolean(key)) {
                            componentMap.put(DataComponentTypes.GLIDER, ComponentStatus.NON_VALUED);
                        }
                    }
                    break;
                    case "intangible_projectile": {
                        ConfigurationSection section = s.getConfigurationSection(key);
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.INTANGIBLE_PROJECTILE, ComponentStatus.UNSET);
                            }
                        } else if (s.getBoolean(key)) {
                            componentMap.put(DataComponentTypes.INTANGIBLE_PROJECTILE, ComponentStatus.NON_VALUED);
                        }
                    }
                    break;
                    case "max_damage": {
                        if (s.getInt("max_stack_size") > 1) {
                            throw new IllegalArgumentException("Item cannot be both damageable and stackable" + (item == null ? "" : ":" + item.getName()));
                        }
                        ConfigurationSection section = s.getConfigurationSection(key);
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.MAX_DAMAGE, ComponentStatus.UNSET);
                            }
                        } else if (s.isInt(key) && s.getInt(key) > 0) {
                            componentMap.put(DataComponentTypes.MAX_DAMAGE, s.getInt(key));
                        }
                    }
                    break;
                    case "max_stack_size": {
                        if (s.isConfigurationSection("max_damage")) {
                            throw new IllegalArgumentException("Item cannot be both damageable and stackable" + itemName);
                        }
                        ConfigurationSection section = s.getConfigurationSection(key);
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.MAX_STACK_SIZE, ComponentStatus.UNSET);
                            }
                        } else if (s.getInt(key) >= 1 && s.getInt(key) <= 99) {
                            componentMap.put(DataComponentTypes.MAX_STACK_SIZE, s.getInt(key));
                        } else {
                            throw new IllegalArgumentException("Max stack size should be between 1 and 99" + itemName);
                        }
                    }
                    break;
                    case "potion_duration_scale": {
                        ConfigurationSection section = s.getConfigurationSection(key);
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.POTION_DURATION_SCALE, ComponentStatus.UNSET);
                            }
                        } else if (s.isDouble(key)) {
                            double scale = s.getDouble(key);
                            if (scale < 0) {
                                throw new IllegalArgumentException("Potion duration scale must be greater than 0: " + itemName);
                            }
                            componentMap.put(DataComponentTypes.POTION_DURATION_SCALE, scale);
                        }
                    }
                    case "profile": {
                        ConfigurationSection section = s.getConfigurationSection(key);
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.PROFILE, ComponentStatus.UNSET);
                            }
                        } else if (s.getString(key) != null) {
                            String value = s.getString(key);
                            ResolvableProfile.Builder profile;
                            if (isBase64(value)) {
                                profile = ResolvableProfile.resolvableProfile().addProperty(new ProfileProperty("textures", value)).uuid(RPGItems.getUUID());
                            } else {
                                profile = ResolvableProfile.resolvableProfile().name(value).uuid(RPGItems.getUUID());
                            }
                            componentMap.put(DataComponentTypes.PROFILE, profile);
                        }
                    }
                    break;
                    case "provides_banner_patterns": {
                        ConfigurationSection section = s.getConfigurationSection(key);
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.BANNER_PATTERNS, ComponentStatus.UNSET);
                            }
                        } else if (s.isString(key)) {
                            componentMap.put(DataComponentTypes.BANNER_PATTERNS, TagKey.create(RegistryKey.BANNER_PATTERN, Key.key(s.getString(key))));
                        }
                    }
                    break;
                    case "provides_trim_material": {
                        ConfigurationSection section = s.getConfigurationSection(key);
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.PROVIDES_TRIM_MATERIAL, ComponentStatus.UNSET);
                            }
                        } else if (s.isString(key)) {
                            componentMap.put(DataComponentTypes.PROVIDES_TRIM_MATERIAL, RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL).get(Key.key(s.getString(key))));
                        }
                    }
                    break;
                    case "rarity": {
                        ConfigurationSection section = s.getConfigurationSection(key);
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.RARITY, ComponentStatus.UNSET);
                            }
                        } else if (s.getString(key) != null) {
                            componentMap.put(DataComponentTypes.RARITY, ItemRarity.valueOf(s.getString(key).toUpperCase()));
                        }
                    }
                    break;
                    case "tool": {
                        Tool.Builder builder = Tool.tool();

                        ConfigurationSection toolSection = s.getConfigurationSection(key);
                        if (toolSection != null) {
                            if (toolSection.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.TOOL, ComponentStatus.UNSET);
                            } else {
                                boolean can = toolSection.getBoolean("can_destroy_blocks_in_creative", true);
                                builder.canDestroyBlocksInCreative(can);

                                int damagePerBlock = toolSection.getInt("damage_per_block", 1);
                                builder.damagePerBlock(Math.max(0, damagePerBlock));

                                float defaultMiningSpeed = (float) toolSection.getDouble("default_mining_speed", 1.0);
                                builder.defaultMiningSpeed(Math.max(0.0f, defaultMiningSpeed));

                                if (toolSection.contains("rules")) {
                                    @NotNull List<Map<?, ?>> rulesList = toolSection.getMapList("rules");
                                    List<Tool.Rule> rules = new ArrayList<>();

                                    for (Map<?, ?> ruleData : rulesList) {
                                        Object blocks = ruleData.get("blocks");
                                        RegistryKeySet<BlockType> blockKeySet = null;
                                        if (blocks instanceof String blockString) {
                                            BlockType type = RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK).get(Key.key(blockString));
                                            if (type != null) {
                                                blockKeySet = RegistrySet.keySetFromValues(RegistryKey.BLOCK, List.of(type));
                                            }
                                        } else if (blocks instanceof List) {
                                            List<String> blockStrings = (List<String>) blocks;
                                            if (!blockStrings.isEmpty()) {
                                                blockKeySet = RegistrySet.keySetFromValues(
                                                        RegistryKey.BLOCK,
                                                        blockStrings.stream()
                                                                .map(block -> RegistryAccess.registryAccess()
                                                                        .getRegistry(RegistryKey.BLOCK)
                                                                        .get(Key.key(block)))
                                                                .filter(Objects::nonNull)
                                                                .toList()
                                                );
                                            }
                                        }

                                        if (blockKeySet != null) {
                                            Float speed = ruleData.containsKey("speed") ? ((Number) ruleData.get("speed")).floatValue() : null;

                                            TriState correctForDrops = TriState.NOT_SET;
                                            if (ruleData.containsKey("correct_for_drops")) {
                                                boolean correctForDropsValue = (boolean) ruleData.get("correct_for_drops");
                                                correctForDrops = correctForDropsValue ? TriState.TRUE : TriState.FALSE;
                                            }

                                            Tool.Rule rule = Tool.rule(blockKeySet, speed, correctForDrops);
                                            rules.add(rule);
                                        }
                                    }
                                    builder.addRules(rules);
                                }
                            }
                        }
                        componentMap.put(DataComponentTypes.TOOL, builder);
                    }
                    break;
                    case "tooltip_display": {
                        ConfigurationSection section = s.getConfigurationSection(key);
                        TooltipDisplay.Builder builder = TooltipDisplay.tooltipDisplay();
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.TOOLTIP_DISPLAY, ComponentStatus.UNSET);
                            } else {
                                List<String> hiddenComponents = section.getStringList("hidden_components");
                                if (!hiddenComponents.isEmpty()) {
                                    Set<DataComponentType> hiddenTypes = hiddenComponents.stream()
                                            .map(String::toLowerCase)
                                            .map((string -> Registry.DATA_COMPONENT_TYPE.get(Key.key(string))))
                                            .collect(Collectors.toSet());
                                    builder.hiddenComponents(hiddenTypes);
                                }
                                componentMap.put(DataComponentTypes.TOOLTIP_DISPLAY, builder);
                            }
                        }
                    }
                    break;
                    case "tooltip_style": {
                        ConfigurationSection section = s.getConfigurationSection(key);
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.TOOLTIP_STYLE, ComponentStatus.UNSET);
                            } else if (s.getString(key) != null) {
                                componentMap.put(DataComponentTypes.TOOLTIP_STYLE, Key.key(s.getString(key)));
                            }
                        }
                    }
                    break;
                    case "trim": {
                        ItemArmorTrim.Builder builder = ItemArmorTrim.itemArmorTrim(new ArmorTrim(TrimMaterial.AMETHYST, TrimPattern.BOLT));
                        ConfigurationSection trimSection = s.getConfigurationSection(key);
                        if (trimSection != null) {
                            if (trimSection.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.TRIM, ComponentStatus.UNSET);
                            } else {
                                String materialKey = trimSection.getString("material");
                                String patternKey = trimSection.getString("pattern");

                                if (materialKey != null && patternKey != null) {
                                    TrimMaterial material = RegistryAccess.registryAccess()
                                            .getRegistry(RegistryKey.TRIM_MATERIAL)
                                            .get(Key.key(materialKey));

                                    TrimPattern pattern = RegistryAccess.registryAccess()
                                            .getRegistry(RegistryKey.TRIM_PATTERN)
                                            .get(Key.key(patternKey));

                                    if (material != null && pattern != null) {
                                        ArmorTrim armorTrim = new ArmorTrim(material, pattern);
                                        builder.armorTrim(armorTrim);
                                    }
                                }
                            }
                        }
                        componentMap.put(DataComponentTypes.TRIM, builder);
                    }
                    break;
                    case "use_cooldown": {
                        ConfigurationSection cooldownSection = s.getConfigurationSection(key);
                        if (cooldownSection != null) {
                            if (cooldownSection.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.USE_COOLDOWN, ComponentStatus.UNSET);
                            } else {
                                float seconds = (float) cooldownSection.getDouble("seconds") <= 0 ? 1.0E-20f : (float) cooldownSection.getDouble("seconds");
                                UseCooldown.Builder builder = UseCooldown.useCooldown(seconds);

                                String cooldownGroupKey = cooldownSection.getString("cooldown_group");
                                if (cooldownGroupKey != null) {
                                    builder.cooldownGroup(Key.key(cooldownGroupKey));
                                }
                                componentMap.put(DataComponentTypes.USE_COOLDOWN, builder);
                            }
                        }
                    }
                    break;
                    case "use_remainder": {
                        ConfigurationSection remainderSection = s.getConfigurationSection(key);
                        if (remainderSection != null) {
                            if (remainderSection.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.USE_REMAINDER, ComponentStatus.UNSET);
                            }
                        } else if (s.isString(key)) {
                            if (ItemManager.getItemByName(s.getString(key)) != null) {
                                UseRemainder useRemainder = UseRemainder.useRemainder(ItemManager.getItemByName(s.getString(key)).toItemStack());
                                componentMap.put(DataComponentTypes.USE_REMAINDER, useRemainder);
                            }
                        }
                    }
                    break;
                    case "weapon": {
                        ConfigurationSection section = s.getConfigurationSection(key);
                        if (section != null) {
                            if (section.getBoolean("unset")) {
                                componentMap.put(DataComponentTypes.WEAPON, ComponentStatus.UNSET);
                            } else {
                                Weapon.Builder builder = Weapon.weapon();
                                if (section.contains("item_damage_per_attack")) {
                                    int damage = section.getInt("item_damage_per_attack", 1);
                                    if (damage < 0) {
                                        throw new IllegalArgumentException("Weapon damage cannot be negative: " + itemName);
                                    }
                                    builder.itemDamagePerAttack(damage);
                                }
                                if (section.contains("disable_blocking_for_seconds")) {
                                    int disableBlockingForSeconds = section.getInt("disable_blocking_for_seconds", 0);
                                    if (disableBlockingForSeconds <= 0) {
                                        throw new IllegalArgumentException("Weapon attack speed must be greater than 0: " + itemName);
                                    }
                                    builder.disableBlockingForSeconds(disableBlockingForSeconds);
                                }
                                componentMap.put(DataComponentTypes.WEAPON, builder);
                            }
                        }
                    }
                }
                components.add(componentMap);
            }
        }
        return components;
    }

    public static void toConfigSection(List<Map<DataComponentType, Object>> componentMaps, ConfigurationSection config) {
        for (Map<DataComponentType, Object> componentMap : componentMaps) {
            for (Map.Entry<DataComponentType, Object> entry : componentMap.entrySet()) {
                DataComponentType type = entry.getKey();
                Object value = entry.getValue();
                if (!(value instanceof Enum<?>) && value instanceof DataComponentBuilder<?>) {
                    value = ((DataComponentBuilder<?>) value).build();
                }

                if (type == DataComponentTypes.BANNER_PATTERNS) {
                    if (value == ComponentStatus.UNSET) {
                        config.set("banner_patterns.unset", true);
                    } else if (value instanceof BannerPatternLayers patterns) {
                        ConfigurationSection bannerSection = config.createSection("banner_patterns");
                        List<Pattern> layers = patterns.patterns();
                        for (int i = 0; i < layers.size(); i++) {
                            Pattern pattern = layers.get(i);
                            ConfigurationSection layerSection = bannerSection.createSection("layer_" + i);
                            layerSection.set("color", pattern.getColor().name());
                            layerSection.set("pattern", RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).getKey(pattern.getPattern()).asString());
                        }
                    }
                } else if (type == DataComponentTypes.BLOCKS_ATTACKS) {
                    if (value instanceof BlocksAttacks builder) {
                        ConfigurationSection blocksSection = config.createSection("blocks_attacks");

                        blocksSection.set("block_delay_seconds", builder.blockDelaySeconds());
                        blocksSection.set("disable_cooldown_scale", builder.disableCooldownScale());

                        if (builder.blockSound() != null) {
                            blocksSection.set("block_sound", builder.blockSound().key().value());
                        }

                        if (builder.disableSound() != null) {
                            blocksSection.set("disable_sound", builder.disableSound().key().value());
                        }

                        if (builder.bypassedBy() != null) {
                            blocksSection.set("bypassed_by", builder.bypassedBy().key().value());
                        }

                        ConfigurationSection itemDamageSection = blocksSection.createSection("item_damage");
                        ItemDamageFunction itemDamage = builder.itemDamage();
                        itemDamageSection.set("base", itemDamage.base());
                        itemDamageSection.set("factor", itemDamage.factor());
                        itemDamageSection.set("threshold", itemDamage.threshold());

                        List<DamageReduction> reductions = builder.damageReductions();
                        if (!reductions.isEmpty()) {
                            ConfigurationSection reductionsSection = blocksSection.createSection("damage_reductions");

                            int index = 1;
                            for (DamageReduction reduction : reductions) {
                                ConfigurationSection reductionSection = reductionsSection.createSection("reduction_" + index);

                                if (reduction instanceof DamageReduction reductionBuilder) {
                                    reductionSection.set("base", reductionBuilder.base());
                                    reductionSection.set("factor", reductionBuilder.factor());
                                }
                                if (reduction.type() != null) {
                                    RegistryKeySet<DamageType> typeSet = reduction.type();
                                    if (typeSet.size() == 1) {
                                        reductionSection.set("type", typeSet.values().iterator().next().key().asString());
                                    } else if (typeSet.size() > 1) {
                                        List<String> typeList = typeSet.values().stream()
                                                .map(typedKey -> typedKey.key().asString())
                                                .collect(Collectors.toList());
                                        reductionSection.set("type", typeList);
                                    }
                                }

                                index++;
                            }
                        }
                    } else if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.createSection("blocks_attacks").set("unset", true);
                    }
                }
                if (type == DataComponentTypes.CONSUMABLE) {
                    if (value == ComponentStatus.UNSET) {
                        config.set("consumable.unset", true);
                    } else if (value instanceof Consumable consumable) {
                        ConfigurationSection consumableSection = config.createSection("consumable");
                        consumableSection.set("animation", consumable.animation().name());
                        consumableSection.set("consume_seconds", consumable.consumeSeconds());
                        consumableSection.set("has_consume_particles", consumable.hasConsumeParticles());
                        consumableSection.set("sound", consumable.sound().asString());

                        List<ConsumeEffect> effects = consumable.consumeEffects();
                        if (!effects.isEmpty()) {
                            ConfigurationSection effectsSection = consumableSection.createSection("on_consume_effects");
                            for (int i = 0; i < effects.size(); i++) {
                                ConsumeEffect effect = effects.get(i);
                                ConfigurationSection effectSection = effectsSection.createSection("effect_" + i);

                                if (effect instanceof ConsumeEffect.ApplyStatusEffects applyEffect) {
                                    effectSection.set("type", "apply_effects");
                                    List<PotionEffect> potionEffects = applyEffect.effects();
                                    ConfigurationSection effectsList = effectSection.createSection("effects");
                                    for (int j = 0; j < potionEffects.size(); j++) {
                                        PotionEffect potionEffect = potionEffects.get(j);
                                        ConfigurationSection potionSection = effectsList.createSection("effect_" + j);
                                        potionSection.set("id", potionEffect.getType().getKey().asString());
                                        potionSection.set("amplifier", potionEffect.getAmplifier());
                                        potionSection.set("duration", potionEffect.getDuration());
                                    }
                                    effectSection.set("probability", applyEffect.probability());
                                } else if (effect instanceof ConsumeEffect.ClearAllStatusEffects) {
                                    effectSection.set("type", "clear_all_effects");
                                } else if (effect instanceof ConsumeEffect.PlaySound) {
                                    effectSection.set("type", "play_sound");
                                    effectSection.set("sound", ((ConsumeEffect.PlaySound) effect).sound().asString());
                                } else if (effect instanceof ConsumeEffect.RemoveStatusEffects) {
                                    effectSection.set("type", "remove_effects");
                                    List<PotionEffectType> removeTypes = new ArrayList<>();
                                    for (TypedKey<PotionEffectType> effectType : ((ConsumeEffect.RemoveStatusEffects) effect).removeEffects()) {
                                        removeTypes.add(RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).get(effectType));
                                    }
                                    List<String> effectKeys = removeTypes.stream()
                                            .map(effectType -> effectType.getKey().asString())
                                            .collect(Collectors.toList());
                                    effectSection.set("effects", effectKeys);
                                } else if (effect instanceof ConsumeEffect.TeleportRandomly) {
                                    effectSection.set("type", "teleport_randomly");
                                    effectSection.set("diameter", ((ConsumeEffect.TeleportRandomly) effect).diameter());
                                }
                            }
                        }
                    }
                } else if (type == DataComponentTypes.CAN_PLACE_ON || type == DataComponentTypes.CAN_BREAK) {
                    String key = type == DataComponentTypes.CAN_PLACE_ON ? "can_place_on" : "can_break";
                    if (value == ComponentStatus.UNSET) {
                        config.set(key + ".unset", true);
                    } else if (value instanceof ItemAdventurePredicate predicate) {
                        List<BlockPredicate> predicates = predicate.predicates();
                        List<String> blockTypes = predicates.stream()
                                .flatMap(p -> StreamSupport.stream(Objects.requireNonNull(p.blocks()).spliterator(), false))
                                .map(block -> block.key().asString())
                                .collect(Collectors.toList());

                        config.set(key, blockTypes);
                    }
                } else if (type == DataComponentTypes.DAMAGE_RESISTANT) {
                    if (value == ComponentStatus.UNSET) {
                        config.set("damage_resistance.unset", true);
                    } else if (value instanceof DamageResistant damageResistant) {
                        config.set("damage_resistance", damageResistant.types().key().asString());
                    }
                } else if (type == DataComponentTypes.DEATH_PROTECTION) {
                    if (value == ComponentStatus.UNSET) {
                        config.set("death_protection.unset", true);
                    } else if (value instanceof DeathProtection deathProtection) {
                        ConfigurationSection deathSection = config.createSection("death_protection");

                        List<ConsumeEffect> effects = deathProtection.deathEffects();
                        if (!effects.isEmpty()) {
                            ConfigurationSection effectsSection = deathSection.createSection("death_effects");
                            for (int i = 0; i < effects.size(); i++) {
                                ConsumeEffect effect = effects.get(i);
                                ConfigurationSection effectSection = effectsSection.createSection("effect_" + i);

                                if (effect instanceof ConsumeEffect.ApplyStatusEffects applyEffect) {
                                    effectSection.set("type", "apply_effects");
                                    List<PotionEffect> potionEffects = applyEffect.effects();
                                    ConfigurationSection effectsList = effectSection.createSection("effects");
                                    for (int j = 0; j < potionEffects.size(); j++) {
                                        PotionEffect potionEffect = potionEffects.get(j);
                                        ConfigurationSection potionSection = effectsList.createSection("effect_" + j);
                                        potionSection.set("id", potionEffect.getType().getKey().asString());
                                        potionSection.set("amplifier", potionEffect.getAmplifier());
                                        potionSection.set("duration", potionEffect.getDuration());
                                    }
                                    effectSection.set("probability", applyEffect.probability());
                                } else if (effect instanceof ConsumeEffect.ClearAllStatusEffects) {
                                    effectSection.set("type", "clear_all_effects");
                                } else if (effect instanceof ConsumeEffect.PlaySound) {
                                    effectSection.set("type", "play_sound");
                                    effectSection.set("sound", ((ConsumeEffect.PlaySound) effect).sound().asString());
                                } else if (effect instanceof ConsumeEffect.RemoveStatusEffects) {
                                    effectSection.set("type", "remove_effects");
                                    List<PotionEffectType> removeTypes = new ArrayList<>();
                                    for (TypedKey<PotionEffectType> effectType : ((ConsumeEffect.RemoveStatusEffects) effect).removeEffects()) {
                                        removeTypes.add(RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).get(effectType));
                                    }
                                    List<String> effectKeys = removeTypes.stream()
                                            .map(effectType -> effectType.getKey().asString())
                                            .collect(Collectors.toList());
                                    effectSection.set("effects", effectKeys);
                                } else if (effect instanceof ConsumeEffect.TeleportRandomly) {
                                    effectSection.set("type", "teleport_randomly");
                                    effectSection.set("diameter", ((ConsumeEffect.TeleportRandomly) effect).diameter());
                                }
                            }
                        }
                    }
                } else if (type == DataComponentTypes.DYED_COLOR) {
                    if (value == ComponentStatus.UNSET) {
                        config.set("dyed_color.unset", true);
                    } else if (value instanceof DyedItemColor dyedColor) {
                        ConfigurationSection dyedSection = config.createSection("dyed_color");
                        Color color = dyedColor.color();
                        dyedSection.set("color", color.getRed() + "," + color.getGreen() + "," + color.getBlue());
                    }
                } else if (type == DataComponentTypes.EQUIPPABLE) {
                    if (value == ComponentStatus.UNSET) {
                        config.set("equippable.unset", true);
                    } else if (value instanceof Equippable equippable) {
                        ConfigurationSection equippableSection = config.createSection("equippable");
                        equippableSection.set("slot", equippable.slot().name());

                        RegistryKeySet<EntityType> allowedEntities = equippable.allowedEntities();
                        if (allowedEntities != null && !allowedEntities.values().isEmpty()) {
                            List<String> entities = allowedEntities.values().stream()
                                    .map(entityType -> entityType.key().asString())
                                    .collect(Collectors.toList());
                            equippableSection.set("allowed_entities", entities);
                        }
                        if (equippable.assetId() != null) {
                            equippableSection.set("asset_id", equippable.assetId().asString());
                        }

                        if (equippable.cameraOverlay() != null) {
                            equippableSection.set("camera_overlay", equippable.cameraOverlay().asString());
                        }

                        equippableSection.set("damage_on_hurt", equippable.damageOnHurt());
                        equippableSection.set("dispensable", equippable.dispensable());

                        equippableSection.set("equip_sound", equippable.equipSound().asString());

                        equippableSection.set("swappable", equippable.swappable());
                        equippableSection.set("equip_on_interaction", equippable.equipOnInteract());
                    }
                } else if (type == DataComponentTypes.TOOL) {
                    if (value instanceof Tool builder) {
                        ConfigurationSection toolSection = config.createSection("tool");

                        toolSection.set("can_destroy_blocks_in_creative", builder.canDestroyBlocksInCreative());
                        toolSection.set("damage_per_block", builder.damagePerBlock());
                        toolSection.set("default_mining_speed", builder.defaultMiningSpeed());

                        List<Tool.Rule> rules = builder.rules();
                        if (!rules.isEmpty()) {
                            List<Map<String, Object>> rulesList = new ArrayList<>();
                            for (Tool.Rule rule : rules) {
                                Map<String, Object> ruleMap = new HashMap<>();

                                RegistryKeySet<BlockType> blockKeySet = rule.blocks();
                                List<String> blockKeys = blockKeySet.values().stream()
                                        .map(blockTypeTypedKey -> blockTypeTypedKey.key().asString())
                                        .collect(Collectors.toList());
                                ruleMap.put("blocks", blockKeys);

                                if (rule.speed() != null) {
                                    ruleMap.put("speed", rule.speed());
                                }

                                if (rule.correctForDrops() != TriState.NOT_SET) {
                                    ruleMap.put("correct_for_drops", rule.correctForDrops().toBoolean());
                                }

                                rulesList.add(ruleMap);
                            }
                            toolSection.set("rules", rulesList);
                        }
                    } else if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.createSection("tool").set("unset", true);
                    }
                } else if (type == DataComponentTypes.FOOD) {
                    if (value instanceof FoodProperties builder) {
                        ConfigurationSection foodSection = config.createSection("food");

                        foodSection.set("nutrition", builder.nutrition());
                        foodSection.set("saturation", builder.saturation());
                        foodSection.set("can_always_eat", builder.canAlwaysEat());
                    } else if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.createSection("food").set("unset", true);
                    }
                } else if (type == DataComponentTypes.ENCHANTABLE) {
                    if (value == ComponentStatus.UNSET) {
                        config.set("enchantable.unset", true);
                    } else if (value instanceof Enchantable enchantable) {
                        config.set("enchantable", enchantable.value());
                    }
                } else if (type == DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE) {
                    if (value == ComponentStatus.UNSET) {
                        config.set("enchantment_glint_override.unset", true);
                    } else if (value instanceof Boolean) {
                        config.set("enchantment_glint_override", value);
                    }
                }
                handleBooleanComponent(componentMap, config, DataComponentTypes.GLIDER, "glider");
                if (type == DataComponentTypes.TOOLTIP_DISPLAY) {
                    if (value == ComponentStatus.UNSET) {
                        config.set("tooltip_display.unset", true);
                    } else if (value instanceof TooltipDisplay builder) {
                        ConfigurationSection tooltipSection = config.createSection("tooltip_display");
                        Set<DataComponentType> hiddenComponents = builder.hiddenComponents();
                        if (!hiddenComponents.isEmpty()) {
                            List<String> hiddenKeys = hiddenComponents.stream()
                                    .map(dtype -> dtype.getKey().value())
                                    .collect(Collectors.toList());
                            tooltipSection.set("hidden_components", hiddenKeys);
                        }
                    }
                }

                handleBooleanComponent(componentMap, config, DataComponentTypes.INTANGIBLE_PROJECTILE, "intangible_projectile");

                if (type == DataComponentTypes.MAX_DAMAGE) {
                    if (value instanceof Integer damageValue) {
                        config.set("max_damage", damageValue);
                    } else if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.createSection("max_damage").set("unset", true);
                    }
                } else if (type == DataComponentTypes.MAX_STACK_SIZE) {
                    if (value instanceof Integer stackSize) {
                        config.set("max_stack_size", stackSize);
                    } else if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.createSection("max_stack_size").set("unset", true);
                    }
                } else if (type == DataComponentTypes.POTION_DURATION_SCALE) {
                    float v = 1.0f;
                    try {
                        v = Float.parseFloat(String.valueOf(value));
                    } catch (NumberFormatException ignore) {
                    }
                    if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.createSection("potion_duration_scale").set("unset", true);
                    } else {
                        config.set("potion_duration_scale", v);
                    }
                } else if (type == DataComponentTypes.PROVIDES_BANNER_PATTERNS) {
                    if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.set("provides_banner_patterns.unset", true);
                    } else {
                        TagKey<PatternType> pattern = (TagKey<PatternType>) value;
                        config.set("provides_banner_patterns", pattern.key().value());
                    }
                } else if (type == DataComponentTypes.PROVIDES_TRIM_MATERIAL) {
                    if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.set("provides_trim_material.unset", true);
                    } else {
                        TrimMaterial trimMaterial = (TrimMaterial) value;
                        config.set("provides_trim_material", RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL).getKey(trimMaterial).value());
                    }
                } else if (type == DataComponentTypes.RARITY) {
                    if (value instanceof ItemRarity itemRarity) {
                        config.set("rarity", itemRarity.name().toLowerCase());
                    } else if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.createSection("rarity").set("unset", true);
                    }
                } else if (type == DataComponentTypes.TOOLTIP_STYLE) {
                    if (value instanceof Key key) {
                        config.set("tooltip_style", key.toString());
                    } else if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.createSection("tooltip_style").set("unset", true);
                    }
                } else if (type == DataComponentTypes.TRIM) {
                    if (value instanceof ItemArmorTrim builder) {
                        ConfigurationSection trimSection = config.createSection("trim");
                        ArmorTrim armorTrim = builder.armorTrim();
                        if (RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL).getKey(armorTrim.getMaterial()) != null) {
                            trimSection.set("material", RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL).getKey(armorTrim.getMaterial()).asString());
                        }
                        if (RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN).getKey(armorTrim.getPattern()) != null) {
                            trimSection.set("pattern", RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN).getKey(armorTrim.getPattern()).asString());
                        }
                    } else if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.createSection("trim").set("unset", true);
                    }
                } else if (type == DataComponentTypes.USE_COOLDOWN) {
                    if (value instanceof UseCooldown builder) {
                        ConfigurationSection cooldownSection = config.createSection("use_cooldown");
                        cooldownSection.set("seconds", builder.seconds());
                        if (builder.cooldownGroup() != null) {
                            cooldownSection.set("cooldown_group", builder.cooldownGroup().toString());
                        }
                    } else if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.createSection("use_cooldown").set("unset", true);
                    }
                } else if (type == DataComponentTypes.USE_REMAINDER) {
                    if (value instanceof UseRemainder useRemainder) {
                        ItemStack itemStack = useRemainder.transformInto();
                        String itemName = null;
                        if (ItemManager.toRPGItem(itemStack).isPresent()) {
                            itemName = ItemManager.toRPGItem(itemStack).get().getName();
                        }
                        if (itemName != null) {
                            config.createSection("use_remainder");
                            config.set("use_remainder", itemName);
                        }
                    } else if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.createSection("use_remainder").set("unset", true);
                    }
                } else if (type == DataComponentTypes.PROFILE) {
                    if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.createSection("profile").set("unset", true);
                    } else if (value instanceof ResolvableProfile profile) {
                        if (profile.properties().stream().findFirst().isPresent()) {
                            config.set("profile", profile.properties().stream().findFirst().get().getValue());
                        } else {
                            config.set("profile", profile.name());
                        }
                    }
                } else if (type == DataComponentTypes.WEAPON) {
                    if (value instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                        config.createSection("weapon").set("unset", true);
                    } else if (value instanceof Weapon builder) {
                        ConfigurationSection weaponSection = config.createSection("weapon");
                        weaponSection.set("item_damage_per_attack", builder.itemDamagePerAttack());
                        weaponSection.set("disable_blocking_for_seconds", builder.disableBlockingForSeconds());
                    }
                }
            }
        }
    }

    private static void handleBooleanComponent(Map<DataComponentType, Object> componentMap, ConfigurationSection s, DataComponentType type, String sectionName) {
        if (componentMap.containsKey(type)) {
            Object component = componentMap.get(type);
            if (component instanceof ComponentStatus status && status == ComponentStatus.UNSET) {
                s.createSection(sectionName).set("unset", true);
            } else if (component == ComponentStatus.NON_VALUED) {
                s.set(sectionName, true);
            }
        }
    }

    private static boolean isBase64(String input) {
        if (input == null || input.length() % 4 != 0) {
            return false;
        }
        try {
            Base64.getDecoder().decode(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
