package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerProjectileLaunch;
import think.rpgitems.power.PowerResult;

class ProjectileLaunch<TPower extends Power>
    extends Trigger<ProjectileLaunchEvent, TPower, PowerProjectileLaunch<TPower>, Void, Void> {
  ProjectileLaunch() {
    super(
        ProjectileLaunchEvent.class,
        PowerProjectileLaunch.class,
        Void.class,
        Void.class,
        "PROJECTILE_LAUNCH");
  }

  public ProjectileLaunch(String name) {
    super(
        name,
        "PROJECTILE_LAUNCH",
        ProjectileLaunchEvent.class,
        PowerProjectileLaunch.class,
        Void.class,
        Void.class);
  }

  @Override
  public PowerResult<Void> run(
      TPower power,
      PowerProjectileLaunch<TPower> pimpl,
      Player player,
      ItemStack i,
      ProjectileLaunchEvent event) {
    return pimpl.projectileLaunch(power, player, i, event);
  }
}
