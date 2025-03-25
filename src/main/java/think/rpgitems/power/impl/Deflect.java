package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.utils.TridentUtils;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.BaseTriggers;
import think.rpgitems.power.trigger.Trigger;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static think.rpgitems.power.Utils.*;

/**
 * Power deflect.
 * <p>
 * Deflect arrows or fireballs towards player within {@link #facing} when
 * 1. manual triggered when some of initiative trigger are enabled with a cooldown of {@link #cooldown} and duration {@link #duration}
 * 2. auto triggered when {@link BaseTriggers#HIT_TAKEN} is enabled with a chance of {@link #chance} and a cooldown of {@link #cooldownpassive}
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = Deflect.Impl.class)
public class Deflect extends BasePower {

    private static final Map<UUID, Long> time = new HashMap<>();
    @Property(order = 2)
    public int cooldown = 0;
    @Property(order = 4)
    public int cooldownpassive = 0;
    @Property
    public int cost = 0;
    @Property
    public int deflectCost = 0;
    @Property
    public int chance = 50;
    @Property
    public int duration = 50;
    @Property(order = 0, required = true)
    public double facing = 30;

    public static Map<UUID, Long> getTime() {
        return time;
    }

    @Override
    public void init(ConfigurationSection section) {
        cooldownpassive = section.getInt("cooldownpassive", 20);
        boolean passive = section.getBoolean("passive", false);
        boolean initiative = section.getBoolean("initiative", true);
        boolean isRight = section.getBoolean("isRight", true);
        triggers = new HashSet<>();
        if (passive) {
            triggers.add(BaseTriggers.HIT_TAKEN);
        }
        if (initiative) {
            triggers.add(isRight ? BaseTriggers.RIGHT_CLICK : BaseTriggers.LEFT_CLICK);
        }
        super.init(section);
    }

    @Override
    public Set<Trigger> getTriggers() {
        HashSet<Trigger> triggers = new HashSet<>(super.getTriggers());
        triggers.add(BaseTriggers.HIT_TAKEN);
        return triggers;
    }

    /**
     * Chance in percentage of triggering this power in passive mode
     */
    public int getChance() {
        return chance;
    }

    /**
     * Cooldown time of this power in passive mode
     */
    public int getCooldownpassive() {
        return cooldownpassive;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    /**
     * Cost of each deflecting
     */
    public int getDeflectCost() {
        return deflectCost;
    }

    /**
     * Duration of this power
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Maximum view angle
     */
    public double getFacing() {
        return facing;
    }

    @Override
    public String getName() {
        return "deflect";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.deflect", (double) getCooldown() / 20d);
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    public class Impl implements PowerHitTaken, PowerRightClick, PowerLeftClick, PowerPlain, PowerBowShoot {

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!(target.getInventory().getItemInMainHand().equals(stack) || target.getInventory().getItemInOffHand().equals(stack))) {
                return PowerResult.noop();
            }
            if (!(event instanceof EntityDamageByEntityEvent byEntityEvent)) {
                return PowerResult.noop();
            }
            if (!(byEntityEvent.getDamager() instanceof Projectile)) {
                return PowerResult.noop();
            }
            boolean activated = System.currentTimeMillis() / 50 < getTime().getOrDefault(target.getUniqueId(), 0L);
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("damage",damage);
            PowerActivateEvent powerEvent = new PowerActivateEvent(target,stack,getPower(),argsMap);
            if(!powerEvent.callEvent())
                return PowerResult.fail();
            if (!activated) {
                if (!triggers.contains(BaseTriggers.HIT_TAKEN)
                        || ThreadLocalRandom.current().nextInt(0, 100) >= getChance())
                    return PowerResult.noop();
                if (!checkCooldown(getPower(), target, getCooldownpassive(), false, true)) return PowerResult.cd();
            }

            if (!getItem().consumeDurability(stack, getDeflectCost())) return PowerResult.cost();

            Projectile p = (Projectile) byEntityEvent.getDamager();
            if (!(p.getShooter() instanceof LivingEntity)) return PowerResult.noop();
            LivingEntity source = (LivingEntity) p.getShooter();
            Vector relativePosition = target.getEyeLocation().toVector();
            relativePosition.subtract(source.getEyeLocation().toVector());
            if (getAngleBetweenVectors(target.getEyeLocation().getDirection(), relativePosition.multiply(-1)) < getFacing()
                    && (p instanceof SmallFireball || p instanceof LargeFireball || p instanceof Arrow)) {
                event.setCancelled(true);
                target.getLocation().getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 3.0f);
                Projectile t = target.launchProjectile(p.getClass());
                Events.registerRPGProjectile(t.getEntityId(), getItem().getUid());
                if (p instanceof TippedArrow) {
                    TippedArrow tippedArrowP = (TippedArrow) p;
                    TippedArrow tippedArrowT = (TippedArrow) t;
                    tippedArrowT.setBasePotionType(tippedArrowP.getBasePotionType());
                    tippedArrowP.getCustomEffects().forEach(potionEffect -> tippedArrowT.addCustomEffect(potionEffect, true));
                }
                if (p instanceof Arrow) {
                    Arrow arrowP = (Arrow) p;
                    Arrow arrowT = (Arrow) t;
                    arrowT.setDamage(arrowP.getDamage());
                    arrowT.setCritical(arrowP.isCritical());
//                    arrowT.setKnockbackStrength(arrowP.getKnockbackStrength());
                    arrowT.setPickupStatus(arrowP.getPickupStatus());
                }
                if (p instanceof Trident) {
                    Trident tridentP = (Trident) p;
                    Trident tridentT = (Trident) t;
                    TridentUtils.setTridentItemStack(tridentP, TridentUtils.getTridentItemStack(tridentT));
                }
                t.setGravity(p.hasGravity());
//                t.setBounce(p.doesBounce());
                t.setShooter(target);
                Events.autoRemoveProjectile(t.getEntityId());
                p.eject();
                p.remove();
                return PowerResult.ok(0.0);
            }
            return PowerResult.noop();
        }

        @Override
        public Power getPower() {
            return Deflect.this;
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkAndSetCooldown(getPower(), player, getCooldown(), true, true, getItem().getUid() + "." + "deflect.initiative"))
                return PowerResult.noop();
            if (!getItem().consumeDurability(stack, getCost()))
                return PowerResult.cost();
            getTime().put(player.getUniqueId(), System.currentTimeMillis() / 50 + getDuration());
            return PowerResult.ok();
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
        }
    }
}
