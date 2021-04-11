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
@Meta(defaultTrigger = {"DYING"}, implClass = Rescue.Impl.class)
public class Rescue extends BasePower {
    private static Cache<UUID, Long> rescueTime = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build();
    @Property(order = 1)
    public int healthTrigger = 4;
    @Property(order = 2)
    public boolean useBed = true;
    @Property(order = 3)
    public boolean inPlace = false;
    @Property
    public double damageTrigger = 1024;

    /**
     * Damage trigger of rescue
     */
    public double getDamageTrigger() {
        return damageTrigger;
    }

    @Override
    public String getName() {
        return "rescue";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.rescue.display", ((double) getHealthTrigger()) / 2, (double) 0 / 20d);
    }

    /**
     * Health trigger of rescue
     */
    public int getHealthTrigger() {
        return healthTrigger;
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

    public static class Impl implements PowerHurt<Rescue>, PowerHitTaken<Rescue> {

        // shouldn't be called if takeHit works. leave it as-is now
        @Override
        public PowerResult<Void> hurt(Rescue power, Player target, ItemStack stack, EntityDamageEvent event) {
            double health = target.getHealth() - event.getFinalDamage();
            if (health > power.getHealthTrigger()) return PowerResult.noop();
            rescue(power, target, stack, event, false);
            return PowerResult.ok();
        }

        private PowerResult<Double> rescue(Rescue power, Player target, ItemStack stack, EntityDamageEvent event, boolean canceled) {
            rescueTime.put(target.getUniqueId(), System.currentTimeMillis());
            target.sendTitle("", I18n.formatDefault("power.rescue.info"), 0, 40, 40);
            DamageCause cause = event.getCause();
            if (!canceled) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 2, 255));
                target.setHealth(power.getHealthTrigger() + event.getDamage());
            } else {
                event.setCancelled(true);
            }
            target.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 10), true);
            target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 2), true);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 400, 2), true);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 10, 1);

            if (power.isInPlace() && cause != DamageCause.DRAGON_BREATH
                        && cause != DamageCause.DROWNING
                        && cause != DamageCause.SUFFOCATION
                        && cause != DamageCause.VOID) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 160, 10));
            } else if (power.isUseBed() && target.getBedSpawnLocation() != null)
                target.teleport(target.getBedSpawnLocation());
            else
                target.teleport(target.getWorld().getSpawnLocation());

            return PowerResult.ok(0.0);
        }

        @Override
        public Class<? extends Rescue> getPowerClass() {
            return Rescue.class;
        }

        @Override
        public PowerResult<Double> takeHit(Rescue power, Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            double health = target.getHealth() - event.getFinalDamage();
            if (health > power.getHealthTrigger() && event.getFinalDamage() < power.getDamageTrigger()) return PowerResult.noop();
            Long last = rescueTime.getIfPresent(target.getUniqueId());
            if (last != null && System.currentTimeMillis() - last < 3000) {
                event.setCancelled(true);
                return PowerResult.ok(0.0);
            }
            return rescue(power, target, stack, event, true);
        }
    }
}
