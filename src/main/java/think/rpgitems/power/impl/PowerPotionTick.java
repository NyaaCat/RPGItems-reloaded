package think.rpgitems.power.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

import static java.lang.Double.min;
import static think.rpgitems.power.Utils.checkAndSetCooldown;

/**
 * Power potiontick.
 * <p>
 * The potiontick power will give the welder {@link #effect}
 * level {@link #amplifier} while held/worn
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = "TICK", implClass = PowerPotionTick.Impl.class)
public class PowerPotionTick extends BasePower {

    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @Property(order = 1, required = true)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    private PotionEffectType effect = PotionEffectType.SPEED;
    @Property(order = 0)
    private int amplifier = 1;
    @Property
    private int cost = 0;
    @Property(order = 2)
    private int interval = 0;
    @Property(order = 3)
    private int duration = 60;
    @Property
    private boolean clear = false;

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    /**
     * Duration of this power
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Interval of this power
     */
    public int getInterval() {
        return interval;
    }

    @Override
    public String getName() {
        return "potiontick";
    }

    @Override
    public String displayText() {
        return isClear() ?
                       I18n.format("power.potiontick.clear", getEffect().getName().toLowerCase().replaceAll("_", " "))
                       : I18n.format("power.potiontick.display", getEffect().getName().toLowerCase().replaceAll("_", " "), getAmplifier() + 1);
    }

    /**
     * Whether to remove the effect instead of adding it.
     */
    public boolean isClear() {
        return clear;
    }

    /**
     * Type of potion effect
     */
    public PotionEffectType getEffect() {
        return effect;
    }

    /**
     * Amplifier of potion effect
     */
    public int getAmplifier() {
        return amplifier;
    }

    public class Impl implements PowerTick, PowerSneaking {
        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            return fire(player, stack);
        }

        private PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkAndSetCooldown(getPower(), player, getInterval(), false, true, "potiontick." + getEffect().getName()))
                return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            double health = player.getHealth();
            boolean hasEffect = false;
            for (PotionEffect potionEffect : player.getActivePotionEffects()) {
                if (potionEffect.getType().equals(getEffect())) {
                    hasEffect = true;
                    if (isClear()) {
                        player.removePotionEffect(getEffect());
                    } else if (potionEffect.getDuration() <= 5 || potionEffect.getAmplifier() < getAmplifier())
                        player.addPotionEffect(new PotionEffect(getEffect(), getDuration(), getAmplifier(), true), true);
                    break;
                }
            }
            if (!hasEffect && !isClear()) {
                player.addPotionEffect(new PotionEffect(getEffect(), getDuration(), getAmplifier(), true), true);
            }
            if (getEffect().equals(PotionEffectType.HEALTH_BOOST) && health > 0) {
                health = min(health, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                player.setHealth(health);
            }
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return PowerPotionTick.this;
        }

        @Override
        public PowerResult<Void> sneaking(Player player, ItemStack stack) {
            return fire(player, stack);
        }
    }
}
