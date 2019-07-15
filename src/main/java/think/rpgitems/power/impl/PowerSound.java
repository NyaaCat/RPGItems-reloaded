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
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class)
public class PowerSound extends BasePower implements PowerLeftClick, PowerRightClick, PowerPlain, PowerHit, PowerBowShoot, PowerHitTaken, PowerHurt {
    /**
     * Pitch of sound
     */
    @Property
    public float pitch = 1.0f;
    /**
     * Volume of sound
     */
    @Property
    public float volume = 1.0f;
    /**
     * Sound to be played
     */
    @Property
    public String sound = "";
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;
    /**
     * Display text of this power
     */
    @Property
    public String display = "Plays sound";
    /**
     * Cooldown time of this power
     */
    @Property
    public long cooldown = 0;

    @Property
    public boolean requireHurtByEntity = true;

    @Override
    public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
        if (!requireHurtByEntity || event instanceof EntityDamageByEntityEvent) {
            return fire(target, stack).with(damage);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
        if (!requireHurtByEntity || event instanceof EntityDamageByEntityEvent) {
            return fire(target, stack);
        }
        return PowerResult.noop();
    }

    @Override
    public String getName() {
        return "sound";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + display;
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, cooldown, true, true)) return PowerResult.cd();
        return this.sound(player, stack);
    }

    private PowerResult<Void> sound(Entity player, ItemStack stack) {
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        Location location = player.getLocation();
        location.getWorld().playSound(location, sound, volume, pitch);
        return PowerResult.ok();
    }

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (!checkCooldown(this, player, cooldown, true, true)) return PowerResult.cd();
        return sound(entity, stack).with(damage);
    }

    @Override
    public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
        return fire(player, stack).with(event.getForce());
    }
}
