package think.rpgitems.power.impl;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

/**
 * Power consumehit.
 *
 * <p>The consume power will remove one item when player hits something.
 */
@Meta(defaultTrigger = "HIT", implClass = ConsumeHit.Impl.class)
public class ConsumeHit extends BasePower {

  @Override
  public String getName() {
    return "consumehit";
  }

  @Override
  public String displayText() {
    return I18n.formatDefault("power.consumehit");
  }

  public static class Impl implements PowerHit<ConsumeHit> {
    @Override
    public PowerResult<Double> hit(
        ConsumeHit power,
        final Player player,
        ItemStack stack,
        LivingEntity entity,
        double damage,
        EntityDamageByEntityEvent event) {
      int count = stack.getAmount() - 1;
      if (count == 0) {
        stack.setAmount(0);
        stack.setType(Material.AIR);
      } else {
        stack.setAmount(count);
      }
      return PowerResult.ok(damage);
    }

    @Override
    public Class<? extends ConsumeHit> getPowerClass() {
      return ConsumeHit.class;
    }
  }
}
