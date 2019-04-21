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

//    @Property
//    public String nextPowerName;
//
//    @Property
//    public int nextPowerIndex;

    @Override
    public @LangKey(skipCheck = true) String getName() {
        return "cancelbowarrow";
    }

    @Override
    public String displayText() {
        return null;
    }

    @Override
    public PowerResult<Void> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
        if (cancelArrow) {
            Entity projectile = e.getProjectile();
            projectile.remove();
        }
//        ItemManager.toRPGItem(itemStack).ifPresent(rpgItem -> {
//            try {
//                List<PowerBowShoot> collect = rpgItem.getPowers().stream()
//                        .filter(power -> power instanceof PowerBowShoot)
//                        .filter(power -> power.getName().equals(nextPowerName))
//                        .map(power -> ((PowerBowShoot) power))
//                        .collect(Collectors.toList());
//                PowerBowShoot power = collect.get(nextPowerIndex);
//                power.bowShoot(player, itemStack, e);
//            } catch (IndexOutOfBoundsException ex){
//                new Message("item " + rpgItem.getDisplayName() +" don't have "+nextPowerName+" "+nextPowerIndex+" that can be triggered.");
//            }
//        });
        return PowerResult.ok();
    }
}
