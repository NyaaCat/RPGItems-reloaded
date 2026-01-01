package think.rpgitems.power;

import cat.nyaa.nyaacore.utils.ItemTagUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.trigger.BaseTriggers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches RPGItem references per player to reduce overhead in the Ticker.
 * Instead of calling ItemManager.toRPGItem() for every item every tick,
 * we cache the results and only rebuild on inventory changes.
 */
public class PlayerRPGInventoryCache {
    private static final PlayerRPGInventoryCache INSTANCE = new PlayerRPGInventoryCache();

    private final Map<UUID, CachedPlayerInventory> cache = new ConcurrentHashMap<>();

    private PlayerRPGInventoryCache() {
    }

    public static PlayerRPGInventoryCache getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the cached inventory for a player, building it if necessary.
     */
    public CachedPlayerInventory get(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), uuid -> buildCache(player));
    }

    /**
     * Invalidates the entire cache for a player.
     * Call when inventory changes in a way that could affect multiple slots.
     */
    public void invalidate(UUID playerId) {
        cache.remove(playerId);
    }

    /**
     * Removes a player from the cache entirely.
     * Call when player quits.
     */
    public void remove(UUID playerId) {
        cache.remove(playerId);
    }

    /**
     * Clears all cached data.
     * Call on plugin reload/disable.
     */
    public void clearAll() {
        cache.clear();
    }

    /**
     * Builds the cache for a player by scanning their inventory.
     */
    private CachedPlayerInventory buildCache(Player player) {
        CachedPlayerInventory cached = new CachedPlayerInventory();

        // Cache inventory items with TICK_INVENTORY or SNEAK triggers
        ItemStack[] inventory = player.getInventory().getContents();
        for (int i = 0; i < inventory.length; i++) {
            ItemStack stack = inventory[i];
            Optional<RPGItem> rpgItem = ItemManager.toRPGItem(stack);
            if (rpgItem.isPresent()) {
                RPGItem item = rpgItem.get();
                if (hasAnyTrigger(item, BaseTriggers.TICK_INVENTORY, BaseTriggers.SNEAK)) {
                    // Ensure UUID is set before caching (prevents updateItem() in tick path)
                    if (ItemTagUtils.getString(stack, RPGItem.NBT_ITEM_UUID).isEmpty()) {
                        item.updateItem(stack, false, player);
                    }
                    cached.inventoryItems.put(i, new CachedItem(item, stack));
                    cached.hasAnyRPGItem = true;
                }
            }
        }

        // Cache armor items with TICK or SNEAKING triggers
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack stack = armor[i];
            Optional<RPGItem> rpgItem = ItemManager.toRPGItem(stack);
            if (rpgItem.isPresent()) {
                RPGItem item = rpgItem.get();
                if (hasAnyTrigger(item, BaseTriggers.TICK, BaseTriggers.SNEAKING)) {
                    // Ensure UUID is set before caching (prevents updateItem() in tick path)
                    if (ItemTagUtils.getString(stack, RPGItem.NBT_ITEM_UUID).isEmpty()) {
                        item.updateItem(stack, false, player);
                    }
                    cached.armorItems[i] = new CachedItem(item, stack);
                    cached.hasAnyRPGItem = true;
                }
            }
        }

        // Cache mainhand with TICK or SNEAKING triggers
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        Optional<RPGItem> mainHandRPG = ItemManager.toRPGItem(mainHand);
        if (mainHandRPG.isPresent()) {
            RPGItem item = mainHandRPG.get();
            if (hasAnyTrigger(item, BaseTriggers.TICK, BaseTriggers.SNEAKING)) {
                // Ensure UUID is set before caching (prevents updateItem() in tick path)
                if (ItemTagUtils.getString(mainHand, RPGItem.NBT_ITEM_UUID).isEmpty()) {
                    item.updateItem(mainHand, false, player);
                }
                cached.mainHand = new CachedItem(item, mainHand);
                cached.hasAnyRPGItem = true;
            }
        }

        // Cache offhand with TICK_OFFHAND or SNEAKING triggers
        ItemStack offHand = player.getInventory().getItemInOffHand();
        Optional<RPGItem> offHandRPG = ItemManager.toRPGItem(offHand);
        if (offHandRPG.isPresent()) {
            RPGItem item = offHandRPG.get();
            if (hasAnyTrigger(item, BaseTriggers.TICK_OFFHAND, BaseTriggers.SNEAKING)) {
                // Ensure UUID is set before caching (prevents updateItem() in tick path)
                if (ItemTagUtils.getString(offHand, RPGItem.NBT_ITEM_UUID).isEmpty()) {
                    item.updateItem(offHand, false, player);
                }
                cached.offHand = new CachedItem(item, offHand);
                cached.hasAnyRPGItem = true;
            }
        }

        return cached;
    }

    /**
     * Checks if an RPGItem has any power with any of the specified triggers.
     */
    @SafeVarargs
    private static <T> boolean hasAnyTrigger(RPGItem item, T... triggers) {
        Set<T> triggerSet = Set.of(triggers);
        return item.getPowers().stream()
                .anyMatch(p -> p.getTriggers().stream().anyMatch(triggerSet::contains));
    }

    /**
     * Represents a cached item with its RPGItem reference and ItemStack.
     */
    public static class CachedItem {
        public final RPGItem rpgItem;
        public final ItemStack stack;

        public CachedItem(RPGItem rpgItem, ItemStack stack) {
            this.rpgItem = rpgItem;
            this.stack = stack;
        }
    }

    /**
     * Cached inventory data for a player.
     */
    public static class CachedPlayerInventory {
        // Slot index -> CachedItem for inventory items with tick powers
        public final Map<Integer, CachedItem> inventoryItems = new HashMap<>();
        // Armor slots (0=boots, 1=leggings, 2=chestplate, 3=helmet)
        public final CachedItem[] armorItems = new CachedItem[4];
        // Main hand item
        public CachedItem mainHand;
        // Off hand item
        public CachedItem offHand;
        // Quick check flag - if false, skip this player entirely
        public boolean hasAnyRPGItem = false;

        /**
         * Returns true if the player has any RPGItems with tick powers.
         */
        public boolean hasAnyRPGItem() {
            return hasAnyRPGItem;
        }
    }
}
