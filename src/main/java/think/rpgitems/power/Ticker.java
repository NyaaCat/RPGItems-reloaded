package think.rpgitems.power;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.data.Context;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

/**
 * BukkitRunnable that runs {@link PowerTick#tick(Player, ItemStack)}
 */
public class Ticker extends BukkitRunnable {

    @Override
    public void run() {
        Context.instance().cleanTick();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (ItemManager.canUse(player, null, false) == Event.Result.DENY) continue;
            ItemStack[] armour = player.getInventory().getArmorContents();
            for (ItemStack part : armour) {
                RPGItem item = ItemManager.toRPGItem(part);
                if (item == null)
                    continue;
                item.power(player, part, null, Trigger.TICK);
            }
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            RPGItem item = ItemManager.toRPGItem(mainHand);
            if (item == null)
                continue;
            item.power(player, mainHand, null, Trigger.TICK);

            if (player.isSneaking()) {
                item.power(player, mainHand, null, Trigger.SNEAKING);
            }
        }
    }

}
