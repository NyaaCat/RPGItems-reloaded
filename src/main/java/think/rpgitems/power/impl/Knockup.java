package think.rpgitems.power.impl;

import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

/**
 * Power knockup.
 *
 * <p>The knockup power will send the hit target flying with a chance of 1/{@link #getChance()} and
 * a power of {@link #getKnockUpPower()}.
 */
@Meta(defaultTrigger = "HIT", implClass = Knockup.Impl.class)
public class Knockup extends BasePower {

  @Property(order = 0)
  public int chance = 20;

  @Property(order = 1, alias = "power")
  public double knockUpPower = 2;

  private Random rand = new Random();

  /** Power of knock up */
  public double getKnockUpPower() {
    return knockUpPower;
  }

  @Override
  public String getName() {
    return "knockup";
  }

  @Override
  public String displayText() {
    return I18n.formatDefault("power.knockup", (int) ((1d / (double) getChance()) * 100d));
  }

  protected Random getRand() {
    return rand;
  }

  /** Chance of triggering this power */
  public int getChance() {
    return chance;
  }

  public static class Impl implements PowerHit<Knockup> {

    @Override
    public PowerResult<Double> hit(
        Knockup power,
        Player player,
        ItemStack stack,
        LivingEntity entity,
        double damage,
        EntityDamageByEntityEvent event) {
      if (power.getRand().nextInt(power.getChance()) == 0) {
        Bukkit.getScheduler()
            .runTask(
                RPGItems.plugin,
                () ->
                    entity.setVelocity(
                        player.getLocation().getDirection().setY(power.getKnockUpPower())));
      }
      return PowerResult.ok(damage);
    }

    @Override
    public Class<? extends Knockup> getPowerClass() {
      return Knockup.class;
    }
  }
}
