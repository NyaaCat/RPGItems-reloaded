package think.rpgitems.power.trigger;

import static think.rpgitems.power.Utils.maxWithCancel;

import java.util.Optional;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerHit;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;

public class Hit<TPower extends Power>
    extends Trigger<EntityDamageByEntityEvent, TPower, PowerHit<TPower>, Double, Optional<Double>> {

  @Property public double minDamage = Double.NEGATIVE_INFINITY;

  @Property public double maxDamage = Double.POSITIVE_INFINITY;

  Hit() {
    super(EntityDamageByEntityEvent.class, PowerHit.class, Double.class, Optional.class, "HIT");
  }

  public Hit(String name, int ignored) {
    super(EntityDamageByEntityEvent.class, PowerHit.class, Double.class, Optional.class, name);
  }

  public Hit(String name) {
    super(
        name, "HIT", EntityDamageByEntityEvent.class, PowerHit.class, Double.class, Optional.class);
  }

  @Override
  public Optional<Double> def(Player player, ItemStack i, EntityDamageByEntityEvent event) {
    return Optional.empty();
  }

  @Override
  public Optional<Double> next(Optional<Double> a, PowerResult<Double> b) {
    return b.isOK() ? Optional.ofNullable(maxWithCancel(a.orElse(null), b.data())) : a;
  }

  @Override
  public PowerResult<Double> warpResult(
      PowerResult<Void> overrideResult,
      TPower power,
      PowerHit<TPower> pimpl,
      Player player,
      ItemStack i,
      EntityDamageByEntityEvent event) {
    return overrideResult.with(event.getDamage());
  }

  @Override
  public PowerResult<Double> run(
      TPower power,
      PowerHit<TPower> pimpl,
      Player player,
      ItemStack i,
      EntityDamageByEntityEvent event) {
    return pimpl.hit(power, player, i, (LivingEntity) event.getEntity(), event.getDamage(), event);
  }

  @Override
  public boolean check(Player player, ItemStack i, EntityDamageByEntityEvent event) {
    return event.getDamage() > minDamage && event.getDamage() < maxDamage;
  }
}
