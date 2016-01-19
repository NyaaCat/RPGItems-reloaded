package de_tr7zw_itemnbtapi;

import org.bukkit.inventory.ItemStack;

/**
 * Copied from tr7zw's ItemNBT API plugin
 * https://github.com/tr7zw/Item-NBT-API
 */
public class NBTItem {

    private ItemStack bukkititem;

    public NBTItem(ItemStack Item) {
        bukkititem = Item;
    }

    public ItemStack getItem() {
        return bukkititem;
    }

    public void setString(String Key, String Text)throws NBTException {
        bukkititem = NBTReflectionutil.setString(bukkititem, Key, Text);
    }

    public String getString(String Key)throws NBTException {
        return NBTReflectionutil.getString(bukkititem, Key);
    }
}