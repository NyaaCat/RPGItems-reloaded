package think.rpgitems.utils.component;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BundleContents;
import io.papermc.paper.datacomponent.item.ChargedProjectiles;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.DyeColor;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Salmon;
import org.bukkit.entity.TropicalFish;
import think.rpgitems.utils.component.handler.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 所有组件 handler 的注册表。新增组件时只需要：
 * 1. 写一个 XxxHandler 实现 ComponentHandler；
 * 2. 加进下面的 HANDLERS 列表。
 * 三个调用点（解析 / 序列化 / 应用）都不需要再改。
 */
public final class ComponentRegistry {

    private static final List<ComponentHandler<?>> HANDLERS = List.of(
            new AttackRangeHandler(),
            new BaseColorHandler(),
            new BannerPatternsHandler(),
            new BlocksAttacksHandler(),
            new BreakSoundHandler(),
            new ConsumableHandler(),
            new ItemAdventurePredicateHandler(DataComponentTypes.CAN_PLACE_ON, "can_place_on"),
            new ItemAdventurePredicateHandler(DataComponentTypes.CAN_BREAK, "can_break"),
            new DamageResistanceHandler(),
            new DeathProtectionHandler(),
            new DyedColorHandler(),
            new EnchantableHandler(),
            new EnchantmentGlintOverrideHandler(),
            new EquippableHandler(),
            new FireworkExplosionHandler(),
            new FireworksHandler(),
            new FoodHandler(),
            new NonValuedHandler(DataComponentTypes.GLIDER, "glider"),
            new InstrumentHandler(),
            new NonValuedHandler(DataComponentTypes.INTANGIBLE_PROJECTILE, "intangible_projectile"),
            new JukeboxPlayableHandler(),
            new KineticWeaponHandler(),
            new LodestoneTrackerHandler(),
            new MapColorHandler(),
            new MapDecorationsHandler(),
            new MapIdHandler(),
            new MaxDamageHandler(),
            new MaxStackSizeHandler(),
            new MinimumAttackChargeHandler(),
            new NoteBlockSoundHandler(),
            new OminousBottleAmplifierHandler(),
            new PiercingWeaponHandler(),
            new PotDecorationsHandler(),
            new PotionContentsHandler(),
            new PotionDurationScaleHandler(),
            new ProfileHandler(),
            new ProvidesBannerPatternsHandler(),
            new RarityHandler(),
            new RecipesHandler(),
            new RepairableHandler(),
            new RepairCostHandler(),
            new SeededContainerLootHandler(),
            new StoredEnchantmentsHandler(),
            new SulfurCubeContentHandler(),
            new SuspiciousStewEffectsHandler(),
            new SwingAnimationHandler(),
            new ToolHandler(),
            new TooltipDisplayHandler(),
            new TooltipStyleHandler(),
            new TrimHandler(),
            new UseCooldownHandler(),
            new UseEffectsHandler(),
            new UseRemainderHandler(),
            new WeaponHandler(),
            new WritableBookContentHandler(),
            new WrittenBookContentHandler(),
            // 值为"物品列表"的组件：列表元素是 rpgitem id，参照 use_remainder 换成对应物品
            new ItemStackListHandler<>(DataComponentTypes.CHARGED_PROJECTILES, "charged_projectiles",
                    ChargedProjectiles::chargedProjectiles, ChargedProjectiles.Builder::add, ChargedProjectiles.Builder::build),
            new ItemStackListHandler<>(DataComponentTypes.BUNDLE_CONTENTS, "bundle_contents",
                    BundleContents::bundleContents, BundleContents.Builder::add, BundleContents.Builder::build),
            new ItemStackListHandler<>(DataComponentTypes.CONTAINER, "container",
                    ItemContainerContents::containerContents, ItemContainerContents.Builder::add, ItemContainerContents.Builder::build),
            // 简单枚举类组件：DataComponentTypes 里值类型为纯 Java enum 的组件
            new EnumHandler<>(DataComponentTypes.DYE, "dye", DyeColor.class),
            new EnumHandler<>(DataComponentTypes.FOX_VARIANT, "fox/variant", Fox.Type.class),
            new EnumHandler<>(DataComponentTypes.SALMON_SIZE, "salmon/size", Salmon.Variant.class),
            new EnumHandler<>(DataComponentTypes.PARROT_VARIANT, "parrot/variant", Parrot.Variant.class),
            new EnumHandler<>(DataComponentTypes.TROPICAL_FISH_PATTERN, "tropical_fish/pattern", TropicalFish.Pattern.class),
            new EnumHandler<>(DataComponentTypes.TROPICAL_FISH_BASE_COLOR, "tropical_fish/base_color", DyeColor.class),
            new EnumHandler<>(DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR, "tropical_fish/pattern_color", DyeColor.class),
            new EnumHandler<>(DataComponentTypes.MOOSHROOM_VARIANT, "mooshroom/variant", MushroomCow.Variant.class),
            new EnumHandler<>(DataComponentTypes.RABBIT_VARIANT, "rabbit/variant", Rabbit.Type.class),
            new EnumHandler<>(DataComponentTypes.HORSE_VARIANT, "horse/variant", Horse.Color.class),
            new EnumHandler<>(DataComponentTypes.LLAMA_VARIANT, "llama/variant", Llama.Color.class),
            new EnumHandler<>(DataComponentTypes.AXOLOTL_VARIANT, "axolotl/variant", Axolotl.Variant.class),
            new EnumHandler<>(DataComponentTypes.WOLF_COLLAR, "wolf/collar", DyeColor.class),
            new EnumHandler<>(DataComponentTypes.CAT_COLLAR, "cat/collar", DyeColor.class),
            new EnumHandler<>(DataComponentTypes.SHEEP_COLOR, "sheep/color", DyeColor.class),
            new EnumHandler<>(DataComponentTypes.SHULKER_COLOR, "shulker/color", DyeColor.class),
            // 简单 registry 类组件：DataComponentTypes 里值类型为 Keyed（数据驱动注册表）的组件
            new KeyedHandler<>(DataComponentTypes.DAMAGE_TYPE, "damage_type", RegistryKey.DAMAGE_TYPE),
            new KeyedHandler<>(DataComponentTypes.PROVIDES_TRIM_MATERIAL, "provides_trim_material", RegistryKey.TRIM_MATERIAL),
            new KeyedHandler<>(DataComponentTypes.VILLAGER_VARIANT, "villager/variant", RegistryKey.VILLAGER_TYPE),
            new KeyedHandler<>(DataComponentTypes.WOLF_VARIANT, "wolf/variant", RegistryKey.WOLF_VARIANT),
            new KeyedHandler<>(DataComponentTypes.WOLF_SOUND_VARIANT, "wolf/sound_variant", RegistryKey.WOLF_SOUND_VARIANT),
            new KeyedHandler<>(DataComponentTypes.PIG_VARIANT, "pig/variant", RegistryKey.PIG_VARIANT),
            new KeyedHandler<>(DataComponentTypes.PIG_SOUND_VARIANT, "pig/sound_variant", RegistryKey.PIG_SOUND_VARIANT),
            new KeyedHandler<>(DataComponentTypes.COW_VARIANT, "cow/variant", RegistryKey.COW_VARIANT),
            new KeyedHandler<>(DataComponentTypes.COW_SOUND_VARIANT, "cow/sound_variant", RegistryKey.COW_SOUND_VARIANT),
            new KeyedHandler<>(DataComponentTypes.CHICKEN_VARIANT, "chicken/variant", RegistryKey.CHICKEN_VARIANT),
            new KeyedHandler<>(DataComponentTypes.CHICKEN_SOUND_VARIANT, "chicken/sound_variant", RegistryKey.CHICKEN_SOUND_VARIANT),
            new KeyedHandler<>(DataComponentTypes.FROG_VARIANT, "frog/variant", RegistryKey.FROG_VARIANT),
            new KeyedHandler<>(DataComponentTypes.PAINTING_VARIANT, "painting/variant", RegistryKey.PAINTING_VARIANT),
            new KeyedHandler<>(DataComponentTypes.ZOMBIE_NAUTILUS_VARIANT, "zombie_nautilus/variant", RegistryKey.ZOMBIE_NAUTILUS_VARIANT),
            new KeyedHandler<>(DataComponentTypes.CAT_VARIANT, "cat/variant", RegistryKey.CAT_VARIANT),
            new KeyedHandler<>(DataComponentTypes.CAT_SOUND_VARIANT, "cat/sound_variant", RegistryKey.CAT_SOUND_VARIANT)
    );

    private static final Map<String, ComponentHandler<?>> BY_YAML_KEY = HANDLERS.stream()
            .collect(Collectors.toUnmodifiableMap(ComponentHandler::yamlKey, Function.identity()));

    private static final Map<DataComponentType, ComponentHandler<?>> BY_TYPE = HANDLERS.stream()
            .collect(Collectors.toUnmodifiableMap(ComponentHandler::type, Function.identity()));


    public static ComponentHandler<?> byYamlKey(String key) {
        return BY_YAML_KEY.get(key.toLowerCase());
    }

    public static ComponentHandler<?> byType(DataComponentType type) {
        return BY_TYPE.get(type);
    }

    /**
     * 全部已注册组件的类型集合，用于 RPGItem#updateItem 里统一 resetData，
     * 避免维护一份和 HANDLERS 手动同步的 reset 列表。
     */
    public static java.util.Collection<DataComponentType> allTypes() {
        return BY_TYPE.keySet();
    }
}