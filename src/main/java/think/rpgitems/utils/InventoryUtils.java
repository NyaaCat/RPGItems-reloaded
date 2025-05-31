package think.rpgitems.utils;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import think.rpgitems.AdminCommands;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InventoryUtils {
    private static final List<Integer> inventoryHashcodes = new ArrayList<>();
    private static final NamespacedKey fillerKey = new NamespacedKey(RPGItems.plugin, "RPGItemFiller");
    private static final NamespacedKey prevKey = new NamespacedKey(RPGItems.plugin, "RPGItemPrev");
    private static final NamespacedKey nextKey = new NamespacedKey(RPGItems.plugin, "RPGItemNext");
    private static final NamespacedKey pageKey = new NamespacedKey(RPGItems.plugin, "RPGItemPage");


    public static List<Integer> getInventoryHashcodes() {
        return inventoryHashcodes;
    }
    public static void addInventoryHashcode(int hashcode) {
        inventoryHashcodes.add(hashcode);
    }
    public static void removeInventoryHashcode(int hashcode) {
        if(inventoryHashcodes.contains(hashcode)) {
            inventoryHashcodes.remove(inventoryHashcodes.indexOf(hashcode));
        }
    }
    public static void openMenu(Player p, int page) {
        openMenu(p,page,null, null, null);
    }
    public static void openMenu(Player p, int page, String displayFilter, String loreFilter, String nameFilter) {
        List<RPGItem> items = new ArrayList<>(ItemManager.items());
        if (nameFilter != null) {
            items.removeIf(item -> !ChatColor.stripColor(item.getName()).toLowerCase().contains(nameFilter.toLowerCase()));
        }
        if (displayFilter != null) {
            items.removeIf(item -> !ChatColor.stripColor(item.getDisplayName()).toLowerCase().contains(displayFilter.toLowerCase()));
        }
        if (loreFilter != null) {
            items.removeIf(item -> !ChatColor.stripColor(item.getDescription().toString()).toLowerCase().contains(loreFilter.toLowerCase()));
        }
        items.sort(Comparator.comparing(item -> ChatColor.stripColor(item.getDisplayName().replaceAll("\\s", "").toLowerCase())));
        int maxPage = (int) Math.ceil((double) items.size() / 45);
        if(page <= 0 || page > maxPage) {
            page = 1;
        }
        Inventory inventory = Bukkit.createInventory(p, 54, I18n.formatDefault("message.menu.title", page));
        addInventoryHashcode(inventory.hashCode());
        int itemsPerPage = 45;
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            inventory.addItem(items.get(i).toItemStack());
        }

        ItemStack filler = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        filler.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        ItemMeta fillerMeta = filler.getItemMeta();
        if(fillerMeta != null) {
            fillerMeta.getPersistentDataContainer().set(fillerKey, PersistentDataType.BOOLEAN, true);
            filler.setItemMeta(fillerMeta);
        }

        ItemStack prevPage = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevPage.getItemMeta();
        if (prevMeta != null) {
            prevMeta.getPersistentDataContainer().set(prevKey, PersistentDataType.BOOLEAN, true);
            prevMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
            prevMeta.setDisplayName(I18n.formatDefault("message.menu.prev"));
            prevPage.setItemMeta(prevMeta);
        }

        ItemStack nextPage = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextPage.getItemMeta();
        if (nextMeta != null) {
            nextMeta.getPersistentDataContainer().set(nextKey, PersistentDataType.BOOLEAN, true);
            nextMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
            nextMeta.setDisplayName(I18n.formatDefault("message.menu.next"));
            nextPage.setItemMeta(nextMeta);
        }

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        if (page > 1) {
            inventory.setItem(45, prevPage);
        }
        if (items.size() > page * itemsPerPage) {
            inventory.setItem(53, nextPage);
        }
        p.openInventory(inventory);
    }
    public static void handelClickEvent(InventoryClickEvent event) {
        if(inventoryHashcodes.contains(event.getInventory().hashCode())){
            if(event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.CHEST) {
                event.setCancelled(true);
                if(event.getCurrentItem() != null){
                    ItemStack item = event.getCurrentItem();
                    if(item.getType() != Material.AIR && item.hasItemMeta()){
                        ItemMeta meta = item.getItemMeta();
                        PersistentDataContainer pdc = meta.getPersistentDataContainer();
                        if(pdc.has(pageKey, PersistentDataType.INTEGER)){
                            int page = pdc.get(pageKey, PersistentDataType.INTEGER);
                            if(Boolean.TRUE.equals(pdc.get(prevKey, PersistentDataType.BOOLEAN))){
                                openMenu((Player) event.getWhoClicked(),page - 1);
                            }
                            if(Boolean.TRUE.equals(pdc.get(nextKey, PersistentDataType.BOOLEAN))){
                                openMenu((Player) event.getWhoClicked(),page + 1);
                            }
                        }else if(!Boolean.TRUE.equals(pdc.get(fillerKey, PersistentDataType.BOOLEAN))){
                            event.setCursor(event.getCurrentItem());
                        }
                    }
                }
            }
        }
    }
    public static void handleCloseEvent(InventoryCloseEvent event) {
        if(inventoryHashcodes.contains(event.getInventory().hashCode())){
            removeInventoryHashcode(event.getInventory().hashCode());
        }
    }
}
