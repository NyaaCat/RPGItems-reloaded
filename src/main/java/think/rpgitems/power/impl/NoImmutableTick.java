package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

/**
 * Power noimmutabletick.
 *
 * <p>Cancel the damage delay (no-damage-tick)
 */
@Meta(defaultTrigger = "HIT", implClass = NoImmutableTick.Impl.class)
public class NoImmutableTick extends BasePower {

  @Property public int immuneTime = 1;

  public int getImmuneTime() {
    return immuneTime;
  }

  @Override
  public String getName() {
    return "noimmutabletick";
  }

  @Override
  public String displayText() {
    return I18n.formatDefault("power.noimmutabletick");
  }

  public static class Impl implements PowerHit<NoImmutableTick> {

    @Override
    public PowerResult<Double> hit(
        NoImmutableTick power,
        Player player,
        ItemStack stack,
        LivingEntity entity,
        double damage,
        EntityDamageByEntityEvent event) {
      Bukkit.getScheduler()
          .runTaskLater(
              RPGItems.plugin, () -> entity.setNoDamageTicks(power.getImmuneTime() + 10), 0);
      Bukkit.getScheduler()
          .runTaskLater(
              RPGItems.plugin, () -> entity.setNoDamageTicks(power.getImmuneTime() + 10), 1);
      return PowerResult.ok(damage);
    }

    @Override
    public Class<? extends NoImmutableTick> getPowerClass() {
      return NoImmutableTick.class;
    }
  }
}
