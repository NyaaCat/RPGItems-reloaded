package think.rpgitems.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerProjectileHit;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Trigger;

public class ProjectileHit extends Trigger<ProjectileHitEvent, PowerProjectileHit, Void, Void> {
    public static final Trigger<ProjectileHitEvent, PowerProjectileHit, Void, Void> PROJECTILE_HIT = new ProjectileHit();

    public ProjectileHit() {
        super(ProjectileHitEvent.class, PowerProjectileHit.class, Void.class, Void.class, "PROJECTILE_HIT");
    }

    @Override
    public PowerResult<Void> run(PowerProjectileHit power, Player player, ItemStack i, ProjectileHitEvent event) {
        return power.projectileHit(player, i, event);
    }
}
