package think.rpgitems.power.impl;

import static think.rpgitems.Events.*;
import static think.rpgitems.Events.DAMAGE_SOURCE_ITEM;

import cat.nyaa.nyaacore.utils.NmsUtils;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;
import think.rpgitems.utils.LightContext;

@Meta(
    defaultTrigger = {"PROJECTILE_HIT"},
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
    implClass = Explosion.Impl.class)
public class Explosion extends BasePower {

  @Property public int distance = 20;

  @Property public double chance = 20;

  @Property(alias = "power")
  public float explosionPower = 4.0f;

  public int getDistance() {
    return distance;
  }

  @Override
  public String getName() {
    return "explosion";
  }

  @Override
  public String displayText() {
    return I18n.formatDefault("power.explosion", getChance(), getExplosionPower());
  }

  /** Chance of triggering this power */
  public double getChance() {
    return chance;
  }

  public float getExplosionPower() {
    return explosionPower;
  }

  public static class Impl
      implements PowerLeftClick<Explosion>,
          PowerRightClick<Explosion>,
          PowerPlain<Explosion>,
          PowerHit<Explosion>,
          PowerProjectileHit<Explosion>,
          PowerLocation<Explosion>,
          PowerBeamHit<Explosion>,
          PowerLivingEntity<Explosion> {

    @Override
    public PowerResult<Void> leftClick(
        Explosion power, Player player, ItemStack stack, PlayerInteractEvent event) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Void> fire(Explosion power, Player player, ItemStack stack) {
      Block targetBlock = player.getTargetBlock(null, power.getDistance());
      if (targetBlock == null) return PowerResult.noop();
      return fire(power, player, stack, targetBlock.getLocation());
    }

    @Override
    public PowerResult<Void> fire(
        Explosion power, Player player, ItemStack stack, Location location) {
      if (ThreadLocalRandom.current().nextDouble(100) < power.getChance()) {
        LightContext.putTemp(
            player.getUniqueId(), DAMAGE_SOURCE, power.getNamespacedKey().toString());
        LightContext.putTemp(player.getUniqueId(), SUPPRESS_MELEE, false);
        LightContext.putTemp(player.getUniqueId(), DAMAGE_SOURCE_ITEM, stack);
        boolean explosion =
            NmsUtils.createExplosion(
                location.getWorld(),
                player,
                location.getX(),
                location.getY(),
                location.getZ(),
                power.getExplosionPower(),
                false,
                false);
        LightContext.clear();
        return explosion ? PowerResult.ok() : PowerResult.fail();
      }
      return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> rightClick(
        Explosion power, Player player, ItemStack stack, PlayerInteractEvent event) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Double> hit(
        Explosion power,
        Player player,
        ItemStack stack,
        LivingEntity entity,
        double damage,
        EntityDamageByEntityEvent event) {
      Location location = entity.getLocation();
      Location start = player.getLocation();
      if (start.distanceSquared(location) >= power.getDistance() * power.getDistance()) {
        player.sendMessage(I18n.formatDefault("message.too.far"));
        return PowerResult.noop();
      }
      return fire(power, player, stack, location).with(damage);
    }

    @Override
    public PowerResult<Void> projectileHit(
        Explosion power, Player player, ItemStack stack, ProjectileHitEvent event) {
      Projectile hit = event.getEntity();
      Location location = hit.getLocation();
      Location start = player.getLocation();
      if (start.distanceSquared(location) >= power.getDistance() * power.getDistance()) {
        player.sendMessage(I18n.formatDefault("message.too.far"));
        return PowerResult.noop();
      }
      return fire(power, player, stack, location);
    }

    @Override
    public Class<? extends Explosion> getPowerClass() {
      return Explosion.class;
    }

    @Override
    public PowerResult<Double> hitEntity(
        Explosion power,
        Player player,
        ItemStack stack,
        LivingEntity entity,
        double damage,
        BeamHitEntityEvent event) {
      Location location = entity.getLocation();
      return fire(power, player, stack, location).with(damage);
    }

    @Override
    public PowerResult<Void> hitBlock(
        Explosion power,
        Player player,
        ItemStack stack,
        Location location,
        BeamHitBlockEvent event) {
      return fire(power, player, stack, location);
    }

    @Override
    public PowerResult<Void> beamEnd(
        Explosion power, Player player, ItemStack stack, Location location, BeamEndEvent event) {
      return fire(power, player, stack, location);
    }

    @Override
    public PowerResult<Void> fire(
        Explosion power,
        Player player,
        ItemStack stack,
        LivingEntity entity,
        @Nullable Double value) {
      Block targetBlock = entity.getTargetBlock(null, power.getDistance());
      if (targetBlock == null || targetBlock.getBlockData().getMaterial().isAir())
        return PowerResult.noop();
      return fire(power, player, stack, targetBlock.getLocation());
    }
  }
}
