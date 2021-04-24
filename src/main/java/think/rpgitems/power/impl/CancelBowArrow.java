package think.rpgitems.power.impl;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

@Meta(immutableTrigger = true, defaultTrigger = "BOW_SHOOT", implClass = CancelBowArrow.Impl.class)
public class CancelBowArrow extends BasePower {

    @Property public boolean cancelArrow = true;

    @Override
    public String getName() {
        return "cancelbowarrow";
    }

    @Override
    public String displayText() {
        return null;
    }

    public boolean isCancelArrow() {
        return cancelArrow;
    }

    public static class Impl implements PowerBowShoot<CancelBowArrow> {
        @Override
        public PowerResult<Float> bowShoot(
                CancelBowArrow power, Player player, ItemStack itemStack, EntityShootBowEvent e) {
            if (power.isCancelArrow()) {
                Entity projectile = e.getProjectile();
                projectile.remove();
            } else {
                e.setCancelled(true);
            }
            return PowerResult.ok(e.getForce());
        }

        @Override
        public Class<? extends CancelBowArrow> getPowerClass() {
            return CancelBowArrow.class;
        }
    }
}
