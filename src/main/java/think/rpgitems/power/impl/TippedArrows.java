package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

/**
 * Power tippedarrow.
 *
 * <p>The arrow power will fire an tipped arrow on right click with {@link #type effect} for {@link
 * #duration} ticks at power {@link #amplifier}
 */
@SuppressWarnings("WeakerAccess")
@Meta(immutableTrigger = true, implClass = TippedArrows.Impl.class)
public class TippedArrows extends BasePower {

  @Property(order = 3, required = true)
  public int amplifier = 1;

  @Property(order = 2)
  public int duration = 15;

  @Deserializer(PotionEffectUtils.class)
  @Serializer(PotionEffectUtils.class)
  @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
  @Property(order = 1)
  public PotionEffectType type = PotionEffectType.POISON;

  @Override
  public String getName() {
    return "tippedarrow";
  }

  @Override
  public String displayText() {
    return I18n.formatDefault(
        "power.tippedarrow",
        getType().getName().toLowerCase().replaceAll("_", " "),
        getAmplifier() + 1,
        ((double) getDuration()) / 20d,
        (0) / 20d);
  }

  /** Type of potion effect */
  public PotionEffectType getType() {
    return type;
  }

  /** Amplifier of potion effect */
  public int getAmplifier() {
    return amplifier;
  }

  /** Duration of potion effect, in ticks */
  public int getDuration() {
    return duration;
  }

  public static class Impl implements PowerRightClick<TippedArrows> {
    @Override
    public PowerResult<Void> rightClick(
        TippedArrows power, Player player, ItemStack stack, PlayerInteractEvent event) {
      player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
      Events.registerRPGProjectile(power.getItem(), stack, player);
      org.bukkit.entity.TippedArrow arrow =
          player.launchProjectile(org.bukkit.entity.TippedArrow.class);
      arrow.addCustomEffect(
          new PotionEffect(power.getType(), power.getDuration(), power.getAmplifier()), true);
      Events.autoRemoveProjectile(arrow.getEntityId());
      return PowerResult.ok();
    }

    @Override
    public Class<? extends TippedArrows> getPowerClass() {
      return TippedArrows.class;
    }
  }
}
