package think.rpgitems.power;

import org.bukkit.Effect;
import think.rpgitems.power.impl.PowerParticle;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Preset {
    NONE,
    POTION_EFFECT_TYPE,
    TRIGGERS,
    VISUAL_EFFECT,
    ;

    @SuppressWarnings("unchecked")
    public List<String> get(Class<? extends Power> cls) {
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
                        "DOLPHINS_GRACE");
            case TRIGGERS:
                return Power.getTriggerTypes(cls).stream().map(Trigger::name).collect(Collectors.toList());
            case VISUAL_EFFECT:
                return
                        Stream.concat(
                                Arrays.stream(Effect.values()).filter(effect -> effect.getType() == Effect.Type.VISUAL),
                                Arrays.stream(PowerParticle.DeprecatedEffect.values())
                        )
                              .map(Enum::name).collect(Collectors.toList());
            case NONE:
            default:
                throw new IllegalStateException();
        }
    }
}
