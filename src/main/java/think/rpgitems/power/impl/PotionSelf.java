package think.rpgitems.power.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

/**
 * Power potionself.
 *
 * <p>On right click it will apply {@link #type effect} for {@link #duration} ticks at power {@link
 * #amplifier}.
 */
@SuppressWarnings("WeakerAccess")
@Meta(
    defaultTrigger = "RIGHT_CLICK",
    generalInterface = PowerPlain.class,
    implClass = PotionSelf.Impl.class)
public class PotionSelf extends BasePower {

  @Property(order = 2)
  public int amplifier = 1;

  @Property(order = 1)
  public int duration = 20;

  @Deserializer(PotionEffectUtils.class)
  @Serializer(PotionEffectUtils.class)
  @Property(order = 3, required = true)
  @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
  public PotionEffectType type = PotionEffectType.HEAL;

  @Property public boolean clear = false;

  @Property public boolean requireHurtByEntity = true;

  @Override
  public void init(ConfigurationSection section) {
    if (section.isInt("amp")) {
      amplifier = section.getInt("amp");
    }
    if (section.isInt("time")) {
      duration = section.getInt("time");
    }

    super.init(section);
  }

  @Override
  public String getName() {
    return "potionself";
  }

  @Override
  public String displayText() {
    return I18n.formatDefault(
        "power.potionself",
        getType().getName().toLowerCase().replaceAll("_", " "),
        getAmplifier() + 1,
        ((double) getDuration()) / 20d);
  }

  /** Type of potion effect */
  public PotionEffectType getType() {
    return type;
  }

  /** Amplifier of potion effect */
  public int getAmplifier() {
    return amplifier;
  }

  /** Time of potion effect, in ticks */
  public int getDuration() {
    return duration;
  }

  /** Whether to remove the effect instead of adding it. */
  public boolean isClear() {
    return clear;
  }

  public boolean isRequireHurtByEntity() {
    return requireHurtByEntity;
  }

  public static class Impl
      implements PowerRightClick<PotionSelf>,
          PowerLeftClick<PotionSelf>,
          PowerSneak<PotionSelf>,
          PowerSprint<PotionSelf>,
          PowerHit<PotionSelf>,
          PowerHitTaken<PotionSelf>,
          PowerPlain<PotionSelf>,
          PowerHurt<PotionSelf>,
          PowerBowShoot<PotionSelf> {
    @Override
    public PowerResult<Double> takeHit(
        PotionSelf power, Player target, ItemStack stack, double damage, EntityDamageEvent event) {
      if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
        return fire(power, target, stack).with(damage);
      }
      return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> fire(PotionSelf power, Player player, ItemStack stack) {
      if (power.isClear()) {
        player.removePotionEffect(power.getType());
      } else {
        player.addPotionEffect(
            new PotionEffect(power.getType(), power.getDuration(), power.getAmplifier()), true);
      }
      return PowerResult.ok();
    }

    @Override
    public Class<? extends PotionSelf> getPowerClass() {
      return PotionSelf.class;
    }

    @Override
    public PowerResult<Void> hurt(
        PotionSelf power, Player target, ItemStack stack, EntityDamageEvent event) {
      if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
        return fire(power, target, stack);
      }
      return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> rightClick(
        PotionSelf power, Player player, ItemStack stack, PlayerInteractEvent event) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Void> leftClick(
        PotionSelf power, Player player, ItemStack stack, PlayerInteractEvent event) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Float> bowShoot(
        PotionSelf power, Player player, ItemStack stack, EntityShootBowEvent event) {
      return fire(power, player, stack).with(event.getForce());
    }

    @Override
    public PowerResult<Void> sneak(
        PotionSelf power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Void> sprint(
        PotionSelf power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Double> hit(
        PotionSelf power,
        Player player,
        ItemStack stack,
        LivingEntity entity,
        double damage,
        EntityDamageByEntityEvent event) {
      return fire(power, player, stack).with(damage);
    }
  }
}
