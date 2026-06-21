package think.rpgitems.power.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.data.Context;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import java.util.HashMap;

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
@Meta(defaultTrigger = "HIT", implClass = RealDamage.Impl.class)
public class RealDamage extends BasePower {

    @Property(order = 0)
    public int cooldown = 0;
    @Property
    public int cost = 0;
    @Property(order = 1, required = true)
    public double realDamage = 0;
    @Property
    public double minDamage = 0;
    @Property
    public boolean canKill = false;
    @Property
    public double minHealth = 0.1;
    @Property
    public boolean ignoreInvulnerability = false;

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

    public boolean isCanKill() {
        return canKill;
    }

    public boolean isIgnoreInvulnerability() {
        return ignoreInvulnerability;
    }

    public double getMinHealth() {
        return minHealth;
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
        if(canKill) {
            return I18n.formatDefault("power.realdamage.cankill", getRealDamage());
        }
        return I18n.formatDefault("power.realdamage.default", getRealDamage(), getMinHealth());
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
            HashMap<String, Object> argsMap = new HashMap<>();
            argsMap.put("target", entity);
            argsMap.put("damage", damage);
            PowerActivateEvent powerEvent = new PowerActivateEvent(player, stack, getPower(), argsMap);
            if (!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (damage < getMinDamage()) return PowerResult.noop();
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            if (!isIgnoreInvulnerability()) {
                if (entity.isInvulnerable()) return PowerResult.noop();
                if (entity.hasPotionEffect(PotionEffectType.RESISTANCE)) {
                    PotionEffect e = entity.getPotionEffect(PotionEffectType.RESISTANCE);
                    if (e.getAmplifier() >= 4) return PowerResult.noop();
                }
            }
            Context.instance().putExpiringSeconds(player.getUniqueId(), "realdamage.target", entity, 3);

            double health = entity.getHealth();
            double realDamage = Math.max(0, getRealDamage());
            double newHealth = health - realDamage;
            if (!canKill) {
                if (health > realDamage && newHealth < minHealth) {
                    newHealth = minHealth;
                }
            } else if (newHealth <= 0) {
                entity.kill(DamageSource.builder(DamageType.PLAYER_ATTACK).withDirectEntity(player).withCausingEntity(player).build());
                return PowerResult.ok(damage);
            }
            if (health != newHealth) {
                entity.setHealth(newHealth);
            }
            return PowerResult.ok(damage);
        }

        @Override
        public Power getPower() {
            return RealDamage.this;
        }
    }
}
