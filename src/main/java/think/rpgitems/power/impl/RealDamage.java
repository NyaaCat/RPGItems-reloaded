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
import static think.rpgitems.power.Utils.checkCooldown;


/**
 * Power realdamage.
 * <p>
 * The item will do {@link #realDamage} to {@link LivingEntity} player hits
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(immutableTrigger = true, implClass = RealDamage.Impl.class)
public class RealDamage extends BasePower {

    @Property(order = 0)
    public int cooldown = 0;
    @Property
    public int cost = 0;
    @Property(order = 1, required = true)
    public double realDamage = 0;
    @Property
    public double minDamage = 0;

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

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
        return I18n.format("power.realdamage", getRealDamage());
    }

    /**
     * Damage of this power
     */
    public double getRealDamage() {
        return realDamage;
    }

    public class Impl implements PowerHit {

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            if (damage < getMinDamage()) return PowerResult.noop();
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            if (entity.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                PotionEffect e = entity.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                if (e.getAmplifier() >= 4) return PowerResult.noop();
            }
            Context.instance().putExpiringSeconds(player.getUniqueId(), "realdamage.target", entity, 3);

            double health = entity.getHealth();
            double newHealth = health - getRealDamage();
            newHealth = max(newHealth, 0.1);//Bug workaround
            newHealth = min(newHealth, entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            entity.setHealth(newHealth);
            return PowerResult.ok(damage);
        }

        @Override
        public Power getPower() {
            return RealDamage.this;
        }
    }
}
