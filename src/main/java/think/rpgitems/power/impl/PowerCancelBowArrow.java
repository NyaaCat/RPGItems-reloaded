package think.rpgitems.power.impl;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.power.*;

@PowerMeta(immutableTrigger = true, defaultTrigger = "BOW_SHOOT", implClass = PowerCancelBowArrow.Impl.class)
public class PowerCancelBowArrow extends BasePower {

    @Property
    private boolean cancelArrow = true;

    @Override
    public @LangKey(skipCheck = true) String getName() {
        return "cancelbowarrow";
    }

    @Override
    public String displayText() {
        return null;
    }

    public class Impl implements PowerBowShoot {
        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
            if (isCancelArrow()) {
                Entity projectile = e.getProjectile();
                projectile.remove();
            } else {
                e.setCancelled(true);
            }
            return PowerResult.ok(e.getForce());
        }

        @Override
        public Power getPower() {
            return PowerCancelBowArrow.this;
        }
    }

    public boolean isCancelArrow() {
        return cancelArrow;
    }

    public void setCancelArrow(boolean cancelArrow) {
        this.cancelArrow = cancelArrow;
    }
}
