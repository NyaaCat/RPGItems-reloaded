package think.rpgitems.power;

import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.power.trigger.Trigger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Preset {
    NONE,
    POTION_EFFECT_TYPE,
    TRIGGERS,
    VISUAL_EFFECT,
    ENCHANTMENT,
    ;

    public List<String> get(Class<? extends PropertyHolder> cls) {
        return switch (this) {
            case POTION_EFFECT_TYPE ->
                    Arrays.stream(PotionEffectType.values()).map(PotionEffectType::getName).collect(Collectors.toList());
            case TRIGGERS ->
                    Power.getTriggers(PowerManager.getMeta(cls).implClass()).stream().map(Trigger::name).collect(Collectors.toList());
            case VISUAL_EFFECT -> Stream.concat(
                            Arrays.stream(Effect.values()).filter(effect -> effect.getType() == Effect.Type.VISUAL),
                            Arrays.stream(DeprecatedEffect.values())
                    )
                    .map(Enum::name).collect(Collectors.toList());
            case ENCHANTMENT ->
                    Arrays.stream(Enchantment.values()).map(Enchantment::getName).collect(Collectors.toList());
            default -> throw new IllegalStateException();
        };
    }

    @SuppressWarnings("unused")
    public enum DeprecatedEffect {
        CLOUD(Particle.CLOUD),
        COLOURED_DUST(Particle.FALLING_DUST),
        CRIT(Particle.CRIT),
        EXPLOSION(Particle.EXPLOSION_NORMAL),
        EXPLOSION_HUGE(Particle.EXPLOSION_HUGE),
        EXPLOSION_LARGE(Particle.EXPLOSION_LARGE),
        FIREWORKS_SPARK(Particle.FIREWORKS_SPARK),
        FLAME(Particle.FLAME),
        FLYING_GLYPH(Particle.ENCHANTMENT_TABLE),
        FOOTSTEP(Particle.NAUTILUS), // TODO: FOOTSTEP is gone
        HAPPY_VILLAGER(Particle.VILLAGER_HAPPY),
        HEART(Particle.HEART),
        INSTANT_SPELL(Particle.SPELL_INSTANT),
        ITEM_BREAK(Particle.ITEM_CRACK),
        LARGE_SMOKE(Particle.SMOKE_LARGE),
        LAVA_POP(Particle.LAVA),
        LAVADRIP(Particle.DRIP_LAVA),
        MAGIC_CRIT(Particle.CRIT_MAGIC),
        NOTE(Particle.NOTE),
        PARTICLE_SMOKE(Particle.SMOKE_NORMAL),
        PORTAL(Particle.PORTAL),
        POTION_SWIRL(Particle.SPELL_MOB),
        POTION_SWIRL_TRANSPARENT(Particle.SPELL_MOB_AMBIENT),
        SLIME(Particle.SLIME),
        SMALL_SMOKE(Particle.TOWN_AURA),
        SNOW_SHOVEL(Particle.SNOW_SHOVEL),
        SNOWBALL_BREAK(Particle.SNOWBALL),
        SPELL(Particle.SPELL),
        SPLASH(Particle.WATER_SPLASH),
        TILE_BREAK(Particle.BLOCK_CRACK),
        TILE_DUST(Particle.BLOCK_DUST),
        VILLAGER_THUNDERCLOUD(Particle.VILLAGER_ANGRY),
        VOID_FOG(Particle.SUSPENDED_DEPTH),
        WATERDRIP(Particle.DRIP_WATER),
        WITCH_MAGIC(Particle.SPELL_WITCH),
        ;
        private final Particle particle;
        DeprecatedEffect(Particle particle) {
            this.particle = particle;
        }
        public Particle getParticle() {
            return particle;
        }
    }
}
