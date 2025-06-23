package think.rpgitems.power.impl;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

@Meta(immutableTrigger = true, defaultTrigger = "PROJECTILE_LAUNCH", implClass = CancelProjectile.Impl.class)
public class CancelProjectile extends BasePower {

    @Property
    public boolean cancelProjectile = true;

    @Override
    public String getName() {
        return "cancelprojectile";
    }

    @Override
    public String displayText() {
        return null;
    }

    public boolean isCancelProjectile() {
        return cancelProjectile;
    }

    public class Impl implements PowerProjectileLaunch {
        @Override
        public PowerResult<Float> projectileLaunch(Player player, ItemStack itemStack, ProjectileLaunchEvent e) {
            if (isCancelProjectile()) {
                Entity projectile = e.getEntity();
                projectile.remove();
            } else {
                e.setCancelled(true);
            }
            return PowerResult.ok(());
        }

        @Override
        public Power getPower() {
            return CancelProjectile.this;
        }
    }
}
