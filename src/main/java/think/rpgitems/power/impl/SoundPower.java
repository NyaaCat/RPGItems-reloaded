package think.rpgitems.power.impl;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;
import think.rpgitems.utils.cast.CastUtils;

import javax.annotation.Nullable;

import java.util.HashMap;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power sound.
 * <p>
 * Play a sound
 * </p>
 */
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = {
        PowerLeftClick.class,
        PowerRightClick.class,
        PowerPlain.class,
        PowerSneak.class,
        PowerLivingEntity.class,
        PowerSprint.class,
        PowerHurt.class,
        PowerHit.class,
        PowerHitTaken.class,
        PowerBowShoot.class,
        PowerProjectileLaunch.class,
        PowerBeamHit.class,
        PowerLocation.class
}, implClass = SoundPower.Impl.class)
public class SoundPower extends BasePower {
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
    public int delay = 0;
    @Property
    public PlayLocation playLocation = PlayLocation.HIT_LOCATION;
    @Property
    public double firingRange = 20;
    @Property
    public boolean requireHurtByEntity = true;

    public int getDelay() {
        return delay;
    }

    public double getFiringRange() {
        return firingRange;
    }

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

    public PlayLocation getPlayLocation() {
        return playLocation;
    }

    public class Impl implements PowerLeftClick, PowerRightClick, PowerPlain, PowerHit, PowerBowShoot, PowerHitTaken, PowerHurt, PowerBeamHit, PowerProjectileHit, PowerTick, PowerSneaking, PowerLivingEntity, PowerLocation, PowerProjectileLaunch {

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower());
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            Location location = player.getLocation();
            if (getPlayLocation().equals(PlayLocation.TARGET)) {
                CastUtils.CastLocation castLocation = CastUtils.rayTrace(player, player.getEyeLocation(), player.getEyeLocation().getDirection(), getFiringRange());
                location = castLocation.getTargetLocation();
            }
            return this.sound(player, stack, location);
        }

        @Override
        public Power getPower() {
            return SoundPower.this;
        }

        private PowerResult<Void> sound(Entity player, ItemStack stack, Location location) {
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            if (getDelay() > 0) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        location.getWorld().playSound(location, getSound(), getVolume(), getPitch());
                    }
                }.runTaskLater(RPGItems.plugin, getDelay());
            } else {
                location.getWorld().playSound(location, getSound(), getVolume(), getPitch());
            }
            return PowerResult.ok();
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            Location location = entity.getLocation();
            if (getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
            } else if (getPlayLocation().equals(PlayLocation.SELF)) {
                location = player.getLocation();
            }
            return sound(entity, stack, location).with(damage);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
        }
        
        @Override
        public PowerResult<Void> projectileLaunch(Player player, ItemStack stack, ProjectileLaunchEvent event) {
            return fire(player, stack);
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

        @Override
        public PowerResult<Double> hitEntity(Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            Location location = entity.getLocation();
            if (getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
            } else if (getPlayLocation().equals(PlayLocation.SELF)) {
                location = player.getLocation();
            }
            return sound(player, stack, location).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
            } else if (getPlayLocation().equals(PlayLocation.SELF)) {
                location = player.getLocation();
            }
            return sound(player, stack, location);
        }

        @Override
        public PowerResult<Void> beamEnd(Player player, ItemStack stack, Location location, BeamEndEvent event) {
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
            } else if (getPlayLocation().equals(PlayLocation.SELF)) {
                location = player.getLocation();
            }
            return sound(player, stack, location);
        }

        @Override
        public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            Location location = event.getEntity().getLocation();
            if (getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
            } else if (getPlayLocation().equals(PlayLocation.SELF)) {
                location = player.getLocation();
            }
            return sound(player, stack, location);
        }

        @Override
        public PowerResult<Void> sneaking(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            return sound(player, stack, player.getEyeLocation());
        }

        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            return sound(player, stack, player.getEyeLocation());
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, @Nullable Double value) {
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            Location location = entity.getLocation();
            if (getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
            } else if (getPlayLocation().equals(PlayLocation.SELF)) {
                location = player.getLocation();
            }
            return sound(player, stack, location);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, Location location) {
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
            } else if (getPlayLocation().equals(PlayLocation.SELF)) {
                location = player.getLocation();
            }
            return sound(player, stack, location);
        }
    }
}
