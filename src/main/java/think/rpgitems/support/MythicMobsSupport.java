package think.rpgitems.support;


import io.lumine.mythic.api.MythicPlugin;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.plugin.Plugin;
import think.rpgitems.RPGItems;

import java.util.logging.Level;

public class MythicMobsSupport {
    public static boolean useMythicMobs = true;
    static MythicPlugin mythicPlugin;
    private static RPGItems plugin;
    private static boolean hasSupport = false;
    public static boolean hasSupport(){
        return hasSupport;
    }
    public static void init(RPGItems plugin) {
        try{
            useMythicMobs = plugin.cfg.useMythicMobs;
            Plugin MM = plugin.getServer().getPluginManager().getPlugin("MythicMobs");
            if (!useMythicMobs || !(MM instanceof MythicPlugin)) {
                return;
            }
            String mmVersion = MM.getDescription().getVersion();
            RPGItems.logger.info("MythicMobs version: " + mmVersion + " found");
            hasSupport = true;
        }catch (Exception e) {
            RPGItems.logger.log(Level.WARNING, "Error enabling MythicMobs support", e);
            hasSupport = false;
        }
    }
}
