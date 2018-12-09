package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.utils.TridentUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static think.rpgitems.power.Utils.*;

/**
 * Power deflect.
 * <p>
 * Deflect arrows or fireballs towards player within {@link #facing} when
 * 1. manual triggered when some of initiative trigger are enabled with a cooldown of {@link #cooldown} and duration {@link #duration}
 * 2. auto triggered when {@link Trigger#HIT_TAKEN} is enabled with a chance of {@link #chance} and a cooldown of {@link #cooldownpassive}
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = "RIGHT_CLICK")
public class PowerDeflect extends BasePower implements PowerHitTaken, PowerRightClick, PowerLeftClick, PowerPlain {

    /**
     * Cooldown time of this power
     */
    @Property(order = 2)
    public int cooldown = 20;

    /**
     * Cooldown time of this power in passive mode
     */
    @Property(order = 4)
    public int cooldownpassive = 20;

    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    /**
     * Chance in percentage of triggering this power in passive mode
     */
    @Property
    public int chance = 50;

    /**
     * Duration of this power
     */
    @Property
    public int duration = 50;

    /**
     * Maximum view angle
     */
    @Property(order = 0, required = true)
    public double facing = 30;

    protected static Map<UUID, Long> time = new HashMap<>();

    @Override
    public String displayText() {
        return I18n.format("power.deflect", (double) cooldown / 20d);
    }

    @Override
    public String getName() {
        return "deflect";
    }

    @Override
    public void init(ConfigurationSection section) {
        cooldownpassive = section.getInt("cooldownpassive", 20);
        boolean passive = section.getBoolean("passive", false);
        boolean initiative = section.getBoolean("initiative", true);
        boolean isRight = section.getBoolean("isRight", true);
        triggers = new HashSet<>();
        if (passive) {
            triggers.add(Trigger.HIT_TAKEN);
        }
        if (initiative) {
            triggers.add(isRight ? Trigger.RIGHT_CLICK : Trigger.LEFT_CLICK);
        }
        super.init(section);
    }

    @Override
    public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
        if (!(target.getInventory().getItemInMainHand().equals(stack) || target.getInventory().getItemInOffHand().equals(stack))) {
            return PowerResult.noop();
        }
        boolean activated = System.currentTimeMillis() / 50 < time.getOrDefault(target.getUniqueId(), 0L);

        if (!activated) {
            if (!triggers.contains(Trigger.HIT_TAKEN)
                        || ThreadLocalRandom.current().nextInt(0, 100) >= chance)
                return PowerResult.noop();
            if (!checkCooldown(this, target, cooldownpassive, false, true)) return PowerResult.cd();
        }

        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();

        if (!(event instanceof EntityDamageByEntityEvent)) {
            return PowerResult.noop();
        }
        EntityDamageByEntityEvent byEntityEvent = (EntityDamageByEntityEvent) event;
        if (!(byEntityEvent.getDamager() instanceof Projectile)) {
            return PowerResult.noop();
        }
        Projectile p = (Projectile) byEntityEvent.getDamager();
        if (!(p.getShooter() instanceof LivingEntity)) return PowerResult.noop();
        LivingEntity source = (LivingEntity) p.getShooter();
        Vector relativePosition = target.getEyeLocation().toVector();
        relativePosition.subtract(source.getEyeLocation().toVector());
        if (getAngleBetweenVectors(target.getEyeLocation().getDirection(), relativePosition.multiply(-1)) >= facing) {
            return PowerResult.noop();
        }
        event.setCancelled(true);
        p.setShooter(target);
        Events.registerProjectile(p.getEntityId(), getItem().getUID());
        Vector velocity = p.getVelocity();
        Bukkit.getScheduler().runTaskLater(RPGItems.plugin, () -> {
            if (p instanceof Trident) {
                TridentUtils.setTridentDealtDamage((Trident) p, false);
            }
            p.setVelocity(target.getEyeLocation().getDirection().multiply(velocity.length()));
            if (p instanceof Fireball) {
                ((Fireball) p).setDirection(target.getEyeLocation().getDirection());
            }
        }, 1);

        target.getLocation().getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 3.0f);
        return PowerResult.ok(0.0);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldownByString(this, player, "deflect.initiative", cooldown, true, true))
            return PowerResult.noop();
        if (!getItem().consumeDurability(stack, cost))
            return PowerResult.cost();
        time.put(player.getUniqueId(), System.currentTimeMillis() / 50 + duration);
        return PowerResult.ok();
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public Set<Trigger> getTriggers() {
        HashSet<Trigger> triggers = new HashSet<>(super.getTriggers());
        triggers.add(Trigger.HIT_TAKEN);
        return triggers;
    }
}
