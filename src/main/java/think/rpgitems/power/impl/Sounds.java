package think.rpgitems.power.impl;

import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;
import think.rpgitems.utils.cast.CastUtils;

/**
 * Power sound.
 *
 * <p>Play a sound
 */
@Meta(
    defaultTrigger = "RIGHT_CLICK",
    generalInterface = {
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
      PowerBeamHit.class,
      PowerLocation.class
    },
    implClass = Sounds.Impl.class)
public class Sounds extends BasePower {
  @Property public float pitch = 1.0f;
  @Property public float volume = 1.0f;
  @Property public String sound = "";
  @Property public String display = "Plays sound";
  @Property public int delay = 0;

  public int getDelay() {
    return delay;
  }

  @Property public PlayLocation playLocation = PlayLocation.HIT_LOCATION;

  @Property public double firingRange = 20;

  public double getFiringRange() {
    return firingRange;
  }

  @Property public boolean requireHurtByEntity = true;

  @Override
  public String getName() {
    return "sound";
  }

  @Override
  public String displayText() {
    return ChatColor.GREEN + getDisplay();
  }

  /** Display text of this power */
  public String getDisplay() {
    return display;
  }

  /** Pitch of sound */
  public float getPitch() {
    return pitch;
  }

  /** Sound to be played */
  public String getSound() {
    return sound;
  }

  /** Volume of sound */
  public float getVolume() {
    return volume;
  }

  public boolean isRequireHurtByEntity() {
    return requireHurtByEntity;
  }

  public PlayLocation getPlayLocation() {
    return playLocation;
  }

  public static class Impl
      implements PowerLeftClick<Sounds>,
          PowerRightClick<Sounds>,
          PowerPlain<Sounds>,
          PowerHit<Sounds>,
          PowerBowShoot<Sounds>,
          PowerHitTaken<Sounds>,
          PowerHurt<Sounds>,
          PowerBeamHit<Sounds>,
          PowerProjectileHit<Sounds>,
          PowerTick<Sounds>,
          PowerSneaking<Sounds>,
          PowerLivingEntity<Sounds>,
          PowerLocation<Sounds> {

    @Override
    public PowerResult<Void> leftClick(
        Sounds power, Player player, ItemStack stack, PlayerInteractEvent event) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Void> fire(Sounds power, Player player, ItemStack stack) {
      Location location = player.getLocation();
      if (power.getPlayLocation().equals(PlayLocation.TARGET)) {
        CastUtils.CastLocation castLocation =
            CastUtils.rayTrace(
                player,
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                power.getFiringRange());
        location = castLocation.getTargetLocation();
      }
      return this.sound(power, player, stack, location);
    }

    @Override
    public Class<? extends Sounds> getPowerClass() {
      return Sounds.class;
    }

    private PowerResult<Void> sound(
        Sounds power, Entity player, ItemStack stack, Location location) {
      if (power.getDelay() > 0) {
        new BukkitRunnable() {
          @Override
          public void run() {
            location
                .getWorld()
                .playSound(location, power.getSound(), power.getVolume(), power.getPitch());
          }
        }.runTaskLater(RPGItems.plugin, power.getDelay());
      } else {
        location
            .getWorld()
            .playSound(location, power.getSound(), power.getVolume(), power.getPitch());
      }
      return PowerResult.ok();
    }

    @Override
    public PowerResult<Void> rightClick(
        Sounds power, Player player, ItemStack stack, PlayerInteractEvent event) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Double> hit(
        Sounds power,
        Player player,
        ItemStack stack,
        LivingEntity entity,
        double damage,
        EntityDamageByEntityEvent event) {
      Location location = entity.getLocation();
      if (power.getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
      } else if (power.getPlayLocation().equals(PlayLocation.SELF)) {
        location = player.getLocation();
      }
      return sound(power, entity, stack, location).with(damage);
    }

    @Override
    public PowerResult<Float> bowShoot(
        Sounds power, Player player, ItemStack stack, EntityShootBowEvent event) {
      return fire(power, player, stack).with(event.getForce());
    }

    @Override
    public PowerResult<Double> takeHit(
        Sounds power, Player target, ItemStack stack, double damage, EntityDamageEvent event) {
      if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
        return fire(power, target, stack).with(damage);
      }
      return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> hurt(
        Sounds power, Player target, ItemStack stack, EntityDamageEvent event) {
      if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
        return fire(power, target, stack);
      }
      return PowerResult.noop();
    }

    @Override
    public PowerResult<Double> hitEntity(
        Sounds power,
        Player player,
        ItemStack stack,
        LivingEntity entity,
        double damage,
        BeamHitEntityEvent event) {
      Location location = entity.getLocation();
      if (power.getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
      } else if (power.getPlayLocation().equals(PlayLocation.SELF)) {
        location = player.getLocation();
      }
      return sound(power, player, stack, location).with(damage);
    }

    @Override
    public PowerResult<Void> hitBlock(
        Sounds power, Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
      if (power.getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
      } else if (power.getPlayLocation().equals(PlayLocation.SELF)) {
        location = player.getLocation();
      }
      return sound(power, player, stack, location);
    }

    @Override
    public PowerResult<Void> beamEnd(
        Sounds power, Player player, ItemStack stack, Location location, BeamEndEvent event) {
      if (power.getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
      } else if (power.getPlayLocation().equals(PlayLocation.SELF)) {
        location = player.getLocation();
      }
      return sound(power, player, stack, location);
    }

    @Override
    public PowerResult<Void> projectileHit(
        Sounds power, Player player, ItemStack stack, ProjectileHitEvent event) {
      Location location = event.getEntity().getLocation();
      if (power.getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
      } else if (power.getPlayLocation().equals(PlayLocation.SELF)) {
        location = player.getLocation();
      }
      return sound(power, player, stack, location);
    }

    @Override
    public PowerResult<Void> sneaking(Sounds power, Player player, ItemStack stack) {
      return sound(power, player, stack, player.getEyeLocation());
    }

    @Override
    public PowerResult<Void> tick(Sounds power, Player player, ItemStack stack) {
      return sound(power, player, stack, player.getEyeLocation());
    }

    @Override
    public PowerResult<Void> fire(
        Sounds power, Player player, ItemStack stack, LivingEntity entity, @Nullable Double value) {
      Location location = entity.getLocation();
      if (power.getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
      } else if (power.getPlayLocation().equals(PlayLocation.SELF)) {
        location = player.getLocation();
      }
      return sound(power, player, stack, location);
    }

    @Override
    public PowerResult<Void> fire(Sounds power, Player player, ItemStack stack, Location location) {
      if (power.getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
      } else if (power.getPlayLocation().equals(PlayLocation.SELF)) {
        location = player.getLocation();
      }
      return sound(power, player, stack, location);
    }
  }
}
