package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerProjectileHit;
import think.rpgitems.power.PowerResult;

class ProjectileHit<TPower extends Power>
    extends Trigger<ProjectileHitEvent, TPower, PowerProjectileHit<TPower>, Void, Void> {
  ProjectileHit() {
    super(
        ProjectileHitEvent.class,
        PowerProjectileHit.class,
        Void.class,
        Void.class,
        "PROJECTILE_HIT");
  }

  public ProjectileHit(String name) {
    super(
        name,
        "PROJECTILE_HIT",
        ProjectileHitEvent.class,
        PowerProjectileHit.class,
        Void.class,
        Void.class);
  }

  @Override
  public PowerResult<Void> run(
      TPower power,
      PowerProjectileHit<TPower> pimpl,
      Player player,
      ItemStack i,
      ProjectileHitEvent event) {
    return pimpl.projectileHit(power, player, i, event);
  }
}
