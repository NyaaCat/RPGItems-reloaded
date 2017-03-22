package think.rpgitems.power;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.Plugin;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.support.WorldGuard;

/**
 * BukkitRunnable that runs {@link RPGItem#tick(Player, ItemStack)}
 */
public class PowerTicker extends BukkitRunnable {

    @Override
    public void run() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (!WorldGuard.canPvP(player)) continue;
            ItemStack[] armour = player.getInventory().getArmorContents();
            for (ItemStack part : armour) {
                RPGItem item = ItemManager.toRPGItem(part);
                if (item == null)
                    continue;
                item.tick(player, part);
                if (item.getDurability(part) <= 0) {
                    part.setType(Material.AIR);
                }
            }
            ItemStack part = player.getInventory().getItemInMainHand();
            RPGItem item = ItemManager.toRPGItem(part);
            if (item == null)
                continue;
            if (item.getDurability(part) <= 0) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin.plugin, new Runnable() {
                    @Override
                    public void run() {
                        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                    }
                }, 1L);
            }
            item.tick(player, part);
        }
    }

}
