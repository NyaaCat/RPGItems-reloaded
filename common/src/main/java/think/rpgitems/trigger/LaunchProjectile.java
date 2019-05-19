package think.rpgitems.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerProjectileLaunch;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Trigger;

public class LaunchProjectile extends Trigger<ProjectileLaunchEvent, PowerProjectileLaunch, Void, Void> {
    public static final Trigger<ProjectileLaunchEvent, PowerProjectileLaunch, Void, Void> LAUNCH_PROJECTILE = new LaunchProjectile();

    public LaunchProjectile() {
        super(ProjectileLaunchEvent.class, PowerProjectileLaunch.class, Void.class, Void.class, "PROJECTILE_LAUNCH");
    }

    @Override
    public PowerResult<Void> run(PowerProjectileLaunch power, Player player, ItemStack i, ProjectileLaunchEvent event) {
        return power.projectileLaunch(player, i, event);
    }
}
