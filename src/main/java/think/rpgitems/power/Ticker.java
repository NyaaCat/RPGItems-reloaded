package think.rpgitems.power;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.data.Context;
import think.rpgitems.item.ItemManager;
import think.rpgitems.power.trigger.BaseTriggers;

/**
 * BukkitRunnable that runs {@link PowerTick#tick(Player, ItemStack)}
 * Uses {@link PlayerRPGInventoryCache} to avoid per-tick item lookups.
 */
public class Ticker extends BukkitRunnable {

    private final PlayerRPGInventoryCache cache = PlayerRPGInventoryCache.getInstance();

    @Override
    public void run() {
        Context.instance().cleanTick();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (ItemManager.canUse(player, null, false) == Event.Result.DENY) continue;

            PlayerRPGInventoryCache.CachedPlayerInventory cached = cache.get(player);
            if (!cached.hasAnyRPGItem()) continue;

            boolean isSneaking = player.isSneaking();

            // Process inventory items with tick powers
            for (PlayerRPGInventoryCache.CachedItem item : cached.inventoryItems.values()) {
                item.rpgItem.power(player, item.stack, null, BaseTriggers.TICK_INVENTORY);
                if (isSneaking) {
                    item.rpgItem.power(player, item.stack, null, BaseTriggers.SNEAKING);
                }
            }

            // Process armor items
            for (PlayerRPGInventoryCache.CachedItem item : cached.armorItems) {
                if (item != null) {
                    item.rpgItem.power(player, item.stack, null, BaseTriggers.TICK);
                    if (isSneaking) {
                        item.rpgItem.power(player, item.stack, null, BaseTriggers.SNEAKING);
                    }
                }
            }

            // Process mainhand
            if (cached.mainHand != null) {
                cached.mainHand.rpgItem.power(player, cached.mainHand.stack, null, BaseTriggers.TICK);
                if (isSneaking) {
                    cached.mainHand.rpgItem.power(player, cached.mainHand.stack, null, BaseTriggers.SNEAKING);
                }
            }

            // Process offhand
            if (cached.offHand != null) {
                cached.offHand.rpgItem.power(player, cached.offHand.stack, null, BaseTriggers.TICK_OFFHAND);
                if (isSneaking) {
                    cached.offHand.rpgItem.power(player, cached.offHand.stack, null, BaseTriggers.SNEAKING);
                }
            }
        }
    }

}
