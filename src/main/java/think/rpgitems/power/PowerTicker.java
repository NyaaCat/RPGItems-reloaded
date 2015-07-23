package think.rpgitems.power;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

public class PowerTicker extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack[] armour = player.getInventory().getArmorContents();
            for (ItemStack part : armour) {
                RPGItem item = ItemManager.toRPGItem(part);
                if (item == null)
                    continue;
                item.tick(player);
            }
            ItemStack part = player.getItemInHand();
            RPGItem item = ItemManager.toRPGItem(part);
            if (item == null)
                continue;
            item.tick(player);
        }
    }

}
