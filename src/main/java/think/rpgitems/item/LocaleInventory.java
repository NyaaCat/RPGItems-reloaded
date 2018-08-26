package think.rpgitems.item;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class LocaleInventory extends InventoryView {

    private Player player;
    private InventoryView view;
    private Inventory real;
    private Inventory fake;

    public LocaleInventory(Player player, InventoryView inventoryView) {
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
        for (ItemStack item : fake) {
            RPGItem rItem = ItemManager.toRPGItem(item);
            if (rItem == null)
                continue;
            item.setType(rItem.getItem());
            ItemMeta meta = rItem.getLocaleMeta();
            if (!(meta instanceof LeatherArmorMeta) && rItem.getItem().isBlock())
                ((Damageable)meta).setDamage(((Damageable)rItem.getLocaleMeta()).getDamage());
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
