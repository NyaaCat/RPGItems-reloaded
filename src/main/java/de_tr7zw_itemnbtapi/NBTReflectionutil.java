package de_tr7zw_itemnbtapi;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * Copied from tr7zw's ItemNBT API plugin
 * https://github.com/tr7zw/Item-NBT-API
 */
class NBTReflectionutil {

    @SuppressWarnings("rawtypes")
    private static Class getCraftItemstack() throws NBTException{
        String Version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        try {
            Class c = Class.forName("org.bukkit.craftbukkit." + Version + ".inventory.CraftItemStack");
            //Constructor<?> cons = c.getConstructor(ItemStack.class);
            //return cons.newInstance(item);
            return c;
        } catch (Exception ex) {
            throw new NBTException("Error in ItemNBTAPI! (Outdated plugin?)", ex);
        }
    }

    private static Object getnewNBTTag() throws NBTException{
        String Version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        try {
            @SuppressWarnings("rawtypes")
            Class c = Class.forName("net.minecraft.server." + Version + ".NBTTagCompound");
            return c.newInstance();
        } catch (Exception ex) {
            throw new NBTException("Error in ItemNBTAPI! (Outdated plugin?)", ex);
        }
    }

    private static Object setNBTTag(Object NBTTag, Object NMSItem) throws NBTException{
        try {
            java.lang.reflect.Method method;
            method = NMSItem.getClass().getMethod("setTag", NBTTag.getClass());
            method.invoke(NMSItem, NBTTag);
            return NMSItem;
        } catch (Exception ex) {
            throw new NBTException("setNBTTag Fail", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object getNMSItemStack(ItemStack item) throws NBTException{
        @SuppressWarnings("rawtypes")
        Class cis = getCraftItemstack();
        java.lang.reflect.Method method;
        try {
            method = cis.getMethod("asNMSCopy", ItemStack.class);
            Object answer = method.invoke(cis, item);
            return answer;
        } catch (Exception e) {
            throw new NBTException("getNMSItemStack Fail", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    private static ItemStack getBukkitItemStack(Object item) throws NBTException{
        @SuppressWarnings("rawtypes")
        Class cis = getCraftItemstack();
        java.lang.reflect.Method method;
        try {
            method = cis.getMethod("asBukkitCopy", item.getClass());
            Object answer = method.invoke(cis, item);
            return (ItemStack) answer;
        } catch (Exception e) {
            throw new NBTException("getBukkitItemStack Fail", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    private static Object getNBTTagCompound(Object nmsitem) throws NBTException{
        @SuppressWarnings("rawtypes")
        Class c = nmsitem.getClass();
        java.lang.reflect.Method method;
        try {
            method = c.getMethod("getTag");
            Object answer = method.invoke(nmsitem);
            return answer;
        } catch (Exception e) {
            throw new NBTException("getNBTTagCompound Fail", e);
        }
    }


    public static ItemStack setString(ItemStack item, String key, String Text) throws NBTException{
        Object nmsitem = getNMSItemStack(item);
        if (nmsitem == null) {
            throw new NBTException("getNMSItemStack returned null", null);
        }
        Object nbttag = getNBTTagCompound(nmsitem);
        if (nbttag == null)
            nbttag = getnewNBTTag();
        java.lang.reflect.Method method;
        try {
            method = nbttag.getClass().getMethod("setString", String.class, String.class);
            method.invoke(nbttag, key, Text);
            nmsitem = setNBTTag(nbttag, nmsitem);
            return getBukkitItemStack(nmsitem);
        } catch (Exception ex) {
            throw new NBTException("setString fail", ex);
        }
    }

    public static String getString(ItemStack item, String key) throws NBTException {
        Object nmsitem = getNMSItemStack(item);
        if (nmsitem == null) {
            throw new NBTException("getNMSItemStack returned null", null);
        }
        Object nbttag = getNBTTagCompound(nmsitem);
        if (nbttag == null)
            nbttag = getnewNBTTag();
        java.lang.reflect.Method method;
        try {
            method = nbttag.getClass().getMethod("getString", String.class);
            return (String) method.invoke(nbttag, key);
        } catch (Exception ex) {
            throw new NBTException("getString fail", ex);
        }
    }
}