package think.rpgitems.item;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import think.rpgitems.data.Locale;

public class LocaleInventory extends InventoryView {
    
    private Player player;
    private InventoryView view;
    private Inventory real;
    private Inventory fake;
    private String locale;
    
    public LocaleInventory(Player player, InventoryView inventoryView) {
        locale = Locale.getPlayerLocale(player);
        this.player = player;
        real = inventoryView.getTopInventory();
        view = inventoryView;
        fake = Bukkit.createInventory(real.getHolder(), real.getSize(), real.getTitle());
        reload();
    }
    
    public InventoryView getView() {
        return view;
    }
    
    public void reload() {
        fake.setContents(real.getContents());
        Iterator<ItemStack> it = fake.iterator();
        while (it.hasNext()) {
            ItemStack item = it.next();
            RPGItem rItem = ItemManager.toRPGItem(item);
            if (rItem == null)
                continue;
            item.setType(rItem.getItem());
            ItemMeta meta = rItem.getLocaleMeta(locale);
            if (!(meta instanceof LeatherArmorMeta) && rItem.getItem().isBlock())
                item.setDurability(rItem.getDataValue());
            item.setItemMeta(meta);
        }
        fake.setContents(fake.getContents());
    }
    
    public Inventory getReal() {
        return real;
    }

    @Override
    public Inventory getBottomInventory() {
        return player.getInventory();
    }

    @Override
    public HumanEntity getPlayer() {
        return player;
    }

    @Override
    public Inventory getTopInventory() {
        return fake;
    }

    @Override
    public InventoryType getType() {
        return InventoryType.CHEST;
    }

    public void sumbitChanges() {
        real.setContents(fake.getContents());
    }

}
