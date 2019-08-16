package think.rpgitems.power;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.data.Context;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

import java.util.Optional;
import java.util.stream.Stream;

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
                Optional<RPGItem> item = ItemManager.toRPGItem(part);
                if (!item.isPresent())
                    continue;
                RPGItem rgi = item.get();
                rgi.power(player, part, null, Trigger.TICK);
            }
            ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
            ItemStack itemInOffHand = player.getInventory().getItemInOffHand();
            Optional<RPGItem> offhand = ItemManager.toRPGItem(itemInOffHand);
            Optional<RPGItem> mainhand = ItemManager.toRPGItem(itemInMainHand);
            mainhand.ifPresent(rpgItem -> rpgItem.power(player, itemInMainHand, null, Trigger.TICK));
            if (player.isSneaking()) {
                mainhand.ifPresent(i -> i.power(player, itemInMainHand, null, Trigger.SNEAKING));
                Stream.of(player.getInventory().getArmorContents())
                      .forEach(i -> Trigger.trigger(player, null, i, Trigger.SNEAKING));
            }
            offhand.ifPresent(i -> i.power(player, itemInOffHand, null, Trigger.TICK_OFFHAND));
        }
    }

}
