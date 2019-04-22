package think.rpgitems.power.impl;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.power.PowerBowShoot;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;

@PowerMeta(defaultTrigger = "BOW_SHOOT")
public class PowerCancelBowArrow extends BasePower implements PowerBowShoot {

    @Property
    public boolean cancelArrow = true;

    @Override
    public @LangKey(skipCheck = true) String getName() {
        return "cancelbowarrow";
    }

    @Override
    public String displayText() {
        return null;
    }

    @Override
    public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
        if (cancelArrow) {
            Entity projectile = e.getProjectile();
            projectile.remove();
        } else {
            e.setCancelled(true);
        }
        return PowerResult.ok(e.getForce());
    }
}
