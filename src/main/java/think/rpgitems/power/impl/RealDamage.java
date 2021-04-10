package think.rpgitems.power.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.data.Context;
import think.rpgitems.power.*;

import static java.lang.Double.max;
import static java.lang.Double.min;


/**
 * Power realdamage.
 * <p>
 * The item will do {@link #realDamage} to {@link LivingEntity} player hits
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "HIT", implClass = RealDamage.Impl.class)
public class RealDamage extends BasePower {

    @Property(order = 1, required = true)
    public double realDamage = 0;
    @Property
    public double minDamage = 0;

    /**
     * Minimum damage to trigger
     */
    public double getMinDamage() {
        return minDamage;
    }

    @Override
    public String getName() {
        return "realdamage";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.realdamage", getRealDamage());
    }

    /**
     * Damage of this power
     */
    public double getRealDamage() {
        return realDamage;
    }

    public static class Impl implements PowerHit<RealDamage> {

        @Override
        public PowerResult<Double> hit(RealDamage power, Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            if (damage < power.getMinDamage()) return PowerResult.noop();
            if (entity.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                PotionEffect e = entity.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                if (e.getAmplifier() >= 4) return PowerResult.noop();
            }
            Context.instance().putExpiringSeconds(player.getUniqueId(), "realdamage.target", entity, 3);

            double health = entity.getHealth();
            double newHealth = health - power.getRealDamage();
            newHealth = max(newHealth, 0.1);//Bug workaround
            newHealth = min(newHealth, entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            entity.setHealth(newHealth);
            return PowerResult.ok(damage);
        }

        @Override
        public Class<? extends RealDamage> getPowerClass() {
            return RealDamage.class;
        }
    }
}
