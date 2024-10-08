package think.rpgitems.power;

import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
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

    @SuppressWarnings("unchecked")
    public List<String> get(Class<? extends PropertyHolder> cls) {
        switch (this) {
            case POTION_EFFECT_TYPE:
                return Arrays.asList("SPEED",
                        "SLOW",
                        "FAST_DIGGING",
                        "SLOW_DIGGING",
                        "INCREASE_DAMAGE",
                        "HEAL",
                        "HARM",
                        "JUMP",
                        "CONFUSION",
                        "REGENERATION",
                        "DAMAGE_RESISTANCE",
                        "FIRE_RESISTANCE",
                        "WATER_BREATHING",
                        "INVISIBILITY",
                        "BLINDNESS",
                        "NIGHT_VISION",
                        "HUNGER",
                        "WEAKNESS",
                        "POISON",
                        "WITHER",
                        "HEALTH_BOOST",
                        "ABSORPTION",
                        "SATURATION",
                        "GLOWING",
                        "LEVITATION",
                        "LUCK",
                        "UNLUCK",
                        "SLOW_FALLING",
                        "CONDUIT_POWER",
                        "DOLPHINS_GRACE",
                        "BAD_OMEN",
                        "HERO_OF_THE_VILLAGE");
            case TRIGGERS:
                return Power.getTriggers(PowerManager.getMeta(cls).implClass()).stream().map(Trigger::name).collect(Collectors.toList());
            case VISUAL_EFFECT:
                return
                        Stream.concat(
                                        Arrays.stream(Effect.values()).filter(effect -> effect.getType() == Effect.Type.VISUAL),
                                        Arrays.stream(DeprecatedEffect.values())
                                )
                                .map(Enum::name).collect(Collectors.toList());
            case ENCHANTMENT:
                return Arrays.stream(Enchantment.values()).map(e -> e.getKey().toString()).collect(Collectors.toList());
            case NONE:
            default:
                throw new IllegalStateException();
        }
    }

    @SuppressWarnings("unused")
    public enum DeprecatedEffect {
        CLOUD(Particle.CLOUD),
        COLOURED_DUST(Particle.FALLING_DUST),
        CRIT(Particle.CRIT),
        EXPLOSION(Particle.EXPLOSION),
        EXPLOSION_HUGE(Particle.EXPLOSION_EMITTER),
        EXPLOSION_LARGE(Particle.EXPLOSION),
        FIREWORKS_SPARK(Particle.FIREWORK),
        FLAME(Particle.FLAME),
        FLYING_GLYPH(Particle.ENCHANT),
        FOOTSTEP(Particle.NAUTILUS),
        HAPPY_VILLAGER(Particle.HAPPY_VILLAGER),
        HEART(Particle.HEART),
        INSTANT_SPELL(Particle.INSTANT_EFFECT),
        ITEM_BREAK(Particle.ITEM),
        LARGE_SMOKE(Particle.LARGE_SMOKE),
        LAVA_POP(Particle.LAVA),
        LAVADRIP(Particle.DRIPPING_LAVA),
        MAGIC_CRIT(Particle.ENCHANTED_HIT),
        NOTE(Particle.NOTE),
        PARTICLE_SMOKE(Particle.SMOKE),
        PORTAL(Particle.PORTAL),
        POTION_SWIRL(Particle.ENTITY_EFFECT),
        POTION_SWIRL_TRANSPARENT(Particle.ENTITY_EFFECT),
        SLIME(Particle.ITEM_SLIME),
        SMALL_SMOKE(Particle.MYCELIUM),
        SNOW_SHOVEL(Particle.SNOWFLAKE),
        SNOWBALL_BREAK(Particle.ITEM_SNOWBALL),
        SPELL(Particle.EFFECT),
        SPLASH(Particle.SPLASH),
        TILE_BREAK(Particle.BLOCK),
        TILE_DUST(Particle.FALLING_DUST),
        VILLAGER_THUNDERCLOUD(Particle.ANGRY_VILLAGER),
        VOID_FOG(Particle.UNDERWATER),
        WATERDRIP(Particle.DRIPPING_WATER),
        WITCH_MAGIC(Particle.WITCH),
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
