package think.rpgitems.power.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power rescue.
 * <p>
 * The rescue power teleports the user to spawn (or to their bed when {@link #useBed} is active)
 * or rescue them in place when {@link #inPlace}
 * when their health gets below the {@link #healthTrigger} while in combat with an enemy
 * or when they takes a damage greater than {@link #damageTrigger}
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(immutableTrigger = true, implClass = PowerRescue.Impl.class)
public class PowerRescue extends BasePower {
    private static Cache<UUID, Long> rescueTime = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build();
    @Property(order = 1)
    private int healthTrigger = 4;
    @Property(order = 2)
    private boolean useBed = true;
    @Property(order = 3)
    private boolean inPlace = false;
    @Property(order = 0)
    private long cooldown = 0;
    @Property
    private int cost = 0;
    @Property
    private double damageTrigger = 1024;

    @Override
    public String displayText() {
        return I18n.format("power.rescue.display", ((double) getHealthTrigger()) / 2, (double) getCooldown() / 20d);
    }

    /**
     * Cooldown time of this power
     */
    public long getCooldown() {
        return cooldown;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    /**
     * Damage trigger of rescue
     */
    public double getDamageTrigger() {
        return damageTrigger;
    }

    /**
     * Health trigger of rescue
     */
    public int getHealthTrigger() {
        return healthTrigger;
    }

    @Override
    public String getName() {
        return "rescue";
    }

    public class Impl implements PowerHurt, PowerHitTaken {

        // shouldn't be called if takeHit works. leave it as-is now
        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            double health = target.getHealth() - event.getFinalDamage();
            if (health > getHealthTrigger()) return PowerResult.noop();
            rescue(target, stack, event, false);
            return PowerResult.ok();
        }

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            double health = target.getHealth() - event.getFinalDamage();
            if (health > getHealthTrigger() && event.getFinalDamage() < getDamageTrigger()) return PowerResult.noop();
            Long last = rescueTime.getIfPresent(target.getUniqueId());
            if (last != null && System.currentTimeMillis() - last < 3000) {
                event.setCancelled(true);
                return PowerResult.ok(0.0);
            }
            return rescue(target, stack, event, true);
        }

        private PowerResult<Double> rescue(Player target, ItemStack stack, EntityDamageEvent event, boolean canceled) {
            if (!checkCooldown(getPower(), target, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            rescueTime.put(target.getUniqueId(), System.currentTimeMillis());
            target.sendMessage(I18n.format("power.rescue.info"));
            DamageCause cause = event.getCause();
            if (!canceled) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 2, 255));
                target.setHealth(getHealthTrigger() + event.getDamage());
            } else {
                event.setCancelled(true);
            }
            target.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 10), true);
            target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 2), true);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 400, 2), true);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 10, 1);

            if (isInPlace() && cause != DamageCause.DRAGON_BREATH
                        && cause != DamageCause.DROWNING
                        && cause != DamageCause.SUFFOCATION
                        && cause != DamageCause.VOID) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 160, 10));
            } else if (isUseBed() && target.getBedSpawnLocation() != null)
                target.teleport(target.getBedSpawnLocation());
            else
                target.teleport(target.getWorld().getSpawnLocation());

            return PowerResult.ok(0.0);
        }

        @Override
        public Power getPower() {
            return PowerRescue.this;
        }
    }

    /**
     * Whether rescue in place instead of teleport
     */
    public boolean isInPlace() {
        return inPlace;
    }

    /**
     * Whether use bed instead of home
     */
    public boolean isUseBed() {
        return useBed;
    }
}
