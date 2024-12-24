package think.rpgitems.power;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.data.Context;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.trigger.BaseTriggers;

import java.util.Optional;

/**
 * BukkitRunnable that runs {@link PowerTick#tick(Player, ItemStack)}
 */
public class Ticker extends BukkitRunnable {

    @Override
    public void run() {
        Context.instance().cleanTick();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (ItemManager.canUse(player, null, false) == Event.Result.DENY) continue;
            ItemStack[] inventory = player.getInventory().getContents();
            for (ItemStack i : inventory) {
                Optional<RPGItem> item = ItemManager.toRPGItem(i);
                if (item.isEmpty()) continue;
                RPGItem rpgItem = item.get();
                rpgItem.power(player,i,null,BaseTriggers.TICK_INVENTORY);
                if(player.isSneaking()){
                    rpgItem.power(player,i,null,BaseTriggers.SNEAK);
                }
            }
            ItemStack[] armour = player.getInventory().getArmorContents();
            for (ItemStack part : armour) {
                Optional<RPGItem> item = ItemManager.toRPGItem(part);
                if (item.isEmpty())
                    continue;
                RPGItem rgi = item.get();
                rgi.power(player, part, null, BaseTriggers.TICK);
                if (player.isSneaking()) {
                    rgi.power(player, part, null, BaseTriggers.SNEAKING);
                }
            }
            ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
            ItemStack itemInOffHand = player.getInventory().getItemInOffHand();
            Optional<RPGItem> offhand = ItemManager.toRPGItem(itemInOffHand);
            Optional<RPGItem> mainhand = ItemManager.toRPGItem(itemInMainHand);
            if (mainhand.isPresent()) {
                mainhand.get().power(player, itemInMainHand, null, BaseTriggers.TICK);

                if (player.isSneaking()) {
                    mainhand.get().power(player, itemInMainHand, null, BaseTriggers.SNEAKING);
                }
            }
            if (offhand.isPresent()) {
                offhand.get().power(player, itemInOffHand, null, BaseTriggers.TICK_OFFHAND);
                if (player.isSneaking()) {
                    offhand.get().power(player, itemInOffHand, null, BaseTriggers.SNEAKING);
                }
            }
        }
    }

}
