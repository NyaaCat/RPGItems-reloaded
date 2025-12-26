package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import java.util.HashMap;

/**
 * Power velocitydamage.
 * <p>
 * Do more damage based on player's velocity.
 * Can be configured to only trigger when falling.
 * </p>
 */
@Meta(defaultTrigger = "HIT", implClass = VelocityDamage.Impl.class)
public class VelocityDamage extends BasePower {

    @Property(order = 0)
    public int percentage = 20;

    @Property(order = 1)
    public int velocityFactor = 50;

    @Property(order = 2)
    public double cap = 300;

    @Property(order = 3)
    public double minVelocity = 0.5;

    @Property
    public boolean setBaseDamage = false;

    @Property
    public boolean fallingOnly = false;

    @Property
    public int cooldown = 0;

    @Property
    public int cost = 0;

    public double getCap() {
        return cap;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getCost() {
        return cost;
    }

    @Override
    public String getName() {
        return "velocitydamage";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.velocitydamage", getPercentage(), getVelocityFactor());
    }

    public int getPercentage() {
        return percentage;
    }

    public int getVelocityFactor() {
        return velocityFactor;
    }

    public double getMinVelocity() {
        return minVelocity;
    }

    public boolean isSetBaseDamage() {
        return setBaseDamage;
    }

    public boolean isFallingOnly() {
        return fallingOnly;
    }

    public class Impl implements PowerHit {
        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            HashMap<String, Object> argsMap = new HashMap<>();
            argsMap.put("damage", damage);
            PowerActivateEvent powerEvent = new PowerActivateEvent(player, stack, getPower(), argsMap);
            if (!powerEvent.callEvent())
                return PowerResult.fail();

            Vector velocity = player.getVelocity();
            double velocityValue;

            if (isFallingOnly()) {
                // Only trigger when falling (negative Y velocity)
                if (velocity.getY() >= 0) {
                    return PowerResult.noop();
                }
                velocityValue = Math.abs(velocity.getY());
            } else {
                // Use total velocity magnitude
                velocityValue = velocity.length();
            }

            // Check minimum velocity threshold
            if (velocityValue < getMinVelocity()) {
                return PowerResult.noop();
            }

            // Check cooldown
            if (!Utils.checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) {
                return PowerResult.cd();
            }

            // Check cost
            if (!getItem().consumeDurability(stack, getCost())) {
                return PowerResult.cost();
            }

            double originDamage = damage;
            // Apply percentage bonus + velocity factor bonus
            damage = damage * (1 + getPercentage() / 100.0 + velocityValue * getVelocityFactor() / 100.0);
            damage = Math.max(Math.min(damage, getCap()), originDamage);

            if (damage > originDamage) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.2f);
            }

            if (isSetBaseDamage()) {
                event.setDamage(damage);
            }

            return PowerResult.ok(damage);
        }

        @Override
        public Power getPower() {
            return VelocityDamage.this;
        }
    }
}
