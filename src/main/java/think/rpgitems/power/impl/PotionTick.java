package think.rpgitems.power.impl;

import static java.lang.Double.min;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

/**
 * Power potiontick.
 *
 * <p>The potiontick power will give the welder {@link #effect} level {@link #amplifier} while
 * held/worn
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "TICK", implClass = PotionTick.Impl.class)
public class PotionTick extends BasePower {

    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @Property(order = 1, required = true)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    public PotionEffectType effect = PotionEffectType.SPEED;

    @Property(order = 0)
    public int amplifier = 1;

    @Property(order = 2)
    public int interval = 0;

    @Property(order = 3)
    public int duration = 60;

    @Property public boolean clear = false;

    /** Duration of this power */
    public int getDuration() {
        return duration;
    }

    /** Interval of this power */
    public int getInterval() {
        return interval;
    }

    @Override
    public String getName() {
        return "potiontick";
    }

    @Override
    public String displayText() {
        return isClear()
                ? I18n.formatDefault(
                        "power.potiontick.clear",
                        getEffect().getName().toLowerCase().replaceAll("_", " "))
                : I18n.formatDefault(
                        "power.potiontick.display",
                        getEffect().getName().toLowerCase().replaceAll("_", " "),
                        getAmplifier() + 1);
    }

    /** Whether to remove the effect instead of adding it. */
    public boolean isClear() {
        return clear;
    }

    /** Type of potion effect */
    public PotionEffectType getEffect() {
        return effect;
    }

    /** Amplifier of potion effect */
    public int getAmplifier() {
        return amplifier;
    }

    public static class Impl implements PowerTick<PotionTick>, PowerSneaking<PotionTick> {
        @Override
        public PowerResult<Void> tick(PotionTick power, Player player, ItemStack stack) {
            return fire(power, player, stack);
        }

        private PowerResult<Void> fire(PotionTick power, Player player, ItemStack stack) {
            double health = player.getHealth();
            boolean hasEffect = false;
            for (PotionEffect potionEffect : player.getActivePotionEffects()) {
                if (potionEffect.getType().equals(power.getEffect())) {
                    hasEffect = true;
                    if (power.isClear()) {
                        player.removePotionEffect(power.getEffect());
                    } else if (potionEffect.getDuration() <= 5
                            || potionEffect.getAmplifier() < power.getAmplifier())
                        player.addPotionEffect(
                                new PotionEffect(
                                        power.getEffect(),
                                        power.getDuration(),
                                        power.getAmplifier(),
                                        true),
                                true);
                    break;
                }
            }
            if (!hasEffect && !power.isClear()) {
                player.addPotionEffect(
                        new PotionEffect(
                                power.getEffect(), power.getDuration(), power.getAmplifier(), true),
                        true);
            }
            if (power.getEffect().equals(PotionEffectType.HEALTH_BOOST) && health > 0) {
                health = min(health, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                player.setHealth(health);
            }
            return PowerResult.ok();
        }

        @Override
        public Class<? extends PotionTick> getPowerClass() {
            return PotionTick.class;
        }

        @Override
        public PowerResult<Void> sneaking(PotionTick power, Player player, ItemStack stack) {
            return fire(power, player, stack);
        }
    }
}
