package think.rpgitems.api;

import org.bukkit.inventory.ItemStack;

import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

public class RPGItems {

    /**
     * If the itemstack is a RPGItem this will return the RPGItem version of the item.
     * If the itemstack isn't a RPGItem this will return null.
     * 
     * @param itemstack The item to converted
     * @return The RPGItem or null
     */
    public RPGItem toRPGItem(ItemStack itemstack) {
        return ItemManager.toRPGItem(itemstack);
    }
}
