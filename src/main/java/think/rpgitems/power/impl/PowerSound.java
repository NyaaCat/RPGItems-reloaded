package think.rpgitems.power.impl;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power sound.
 * <p>
 * Play a sound
 * </p>
 */
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = PowerSound.Impl.class)
public class PowerSound extends BasePower {
    @Property
    public float pitch = 1.0f;
    @Property
    public float volume = 1.0f;
    @Property
    public String sound = "";
    @Property
    public int cost = 0;
    @Property
    public String display = "Plays sound";
    @Property
    public int cooldown = 0;

    @Property
    public boolean requireHurtByEntity = true;

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

    @Override
    public String getName() {
        return "sound";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + getDisplay();
    }

    /**
     * Display text of this power
     */
    public String getDisplay() {
        return display;
    }

    /**
     * Pitch of sound
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Sound to be played
     */
    public String getSound() {
        return sound;
    }

    /**
     * Volume of sound
     */
    public float getVolume() {
        return volume;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public class Impl implements PowerLeftClick, PowerRightClick, PowerPlain, PowerHit, PowerBowShoot, PowerHitTaken, PowerHurt {

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            return this.sound(player, stack);
        }

        @Override
        public Power getPower() {
            return PowerSound.this;
        }

        private PowerResult<Void> sound(Entity player, ItemStack stack) {
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            Location location = player.getLocation();
            location.getWorld().playSound(location, getSound(), getVolume(), getPitch());
            return PowerResult.ok();
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            return sound(entity, stack).with(damage);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack);
            }
            return PowerResult.noop();
        }
    }
}
