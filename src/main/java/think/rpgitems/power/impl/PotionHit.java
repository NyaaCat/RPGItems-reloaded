package think.rpgitems.power.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

import java.util.Random;

/**
 * Power potionhit.
 * <p>
 * On hit it will apply {@link #type effect} for {@link #duration} ticks at power {@link #amplifier} with a chance of hitting of 1/{@link #chance}.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "HIT", generalInterface = PowerLivingEntity.class, implClass = PotionHit.Impl.class)
public class PotionHit extends BasePower {

    @Property
    public int chance = 20;
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @Property(order = 3, required = true)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    public PotionEffectType type = PotionEffectType.INSTANT_DAMAGE;
    @Property(order = 1)
    public int duration = 20;
    @Property(order = 2)
    public int amplifier = 1;
    @Property
    public int cost = 0;

    private final Random rand = new Random();

    /**
     * Amplifier of potion effect
     */
    public int getAmplifier() {
        return amplifier;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    /**
     * Duration of potion effect
     */
    public int getDuration() {
        return duration;
    }

    @Override
    public String getName() {
        return "potionhit";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.potionhit", (int) ((1d / (double) getChance()) * 100d), getType().getName().toLowerCase().replace('_', ' '));
    }

    /**
     * Chance of triggering this power
     */
    public int getChance() {
        return chance;
    }

    /**
     * Type of potion effect
     */
    public PotionEffectType getType() {
        return type;
    }

    public Random getRand() {
        return rand;
    }

    public class Impl implements PowerHit, PowerLivingEntity {
        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }


        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, Double value) {
            if (getRand().nextInt(getChance()) != 0) {
                return PowerResult.noop();
            }
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            entity.addPotionEffect(new PotionEffect(getType(), getDuration(), getAmplifier()), true);
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return PotionHit.this;
        }
    }
}
