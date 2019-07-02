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
@PowerMeta(immutableTrigger = true, generalInterface = PowerLivingEntity.class)
public class PowerPotionHit extends BasePower implements PowerHit, PowerLivingEntity {

    /**
     * Chance of triggering this power
     */
    @Property
    public int chance = 20;
    /**
     * Type of potion effect
     */
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @Property(order = 3, required = true)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    public PotionEffectType type = PotionEffectType.HARM;
    /**
     * Duration of potion effect
     */
    @Property(order = 1)
    public int duration = 20;
    /**
     * Amplifier of potion effect
     */
    @Property(order = 2)
    public int amplifier = 1;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    private Random rand = new Random();

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        return fire(player, stack, entity, damage).with(damage);
    }

    @Override
    public String displayText() {
        return I18n.format("power.potionhit", (int) ((1d / (double) chance) * 100d), type.getName().toLowerCase().replace('_', ' '));
    }

    @Override
    public String getName() {
        return "potionhit";
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, Double value) {
        if (rand.nextInt(chance) != 0) {
            return PowerResult.noop();
        }
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        entity.addPotionEffect(new PotionEffect(type, duration, amplifier), true);
        return PowerResult.ok();
    }
}
