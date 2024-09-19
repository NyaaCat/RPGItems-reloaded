package think.rpgitems.support;


import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.plugin.Plugin;
import think.rpgitems.RPGItems;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderAPISupport {
    public static boolean usePlaceholderAPI = true;
    static PlaceholderAPIPlugin papiPlugin;
    private static RPGItems plugin;
    private static boolean hasSupport = false;
    public static boolean hasSupport(){
        return hasSupport;
    }
    public static void init(RPGItems plugin) {
        try{
            usePlaceholderAPI = plugin.cfg.usePlaceholderAPI;
            Plugin PAPI = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI");
            if (!usePlaceholderAPI || !(PAPI instanceof PlaceholderAPIPlugin)) {
                return;
            }
            String papiVersion = PAPI.getDescription().getVersion();
            RPGItems.logger.info("PlaceholderAPI version: " + papiVersion + " found");
            Pattern pattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
            Matcher matcher = pattern.matcher(papiVersion);
            if (matcher.find()) { // Check if there is a match
                int middleNumber = Integer.parseInt(matcher.group(2));
                int lastNumber = Integer.parseInt(matcher.group(3));
                if (middleNumber < 10 || (middleNumber == 10 && lastNumber < 2)) {
                    RPGItems.logger.warning("Requires PlaceholderAPI 2.10.2 or later, disabling integration");
                    hasSupport = false;
                    return;
                }
            } else {
                RPGItems.logger.warning("Failed to parse PlaceholderAPI version, disabling integration");
                hasSupport = false;
                return;
            }
            hasSupport = true;
        }catch (Exception e){
            RPGItems.logger.log(Level.WARNING, "Error enabling PlaceholderAPI support", e);
            hasSupport = false;
        }
        if(hasSupport()){
            new PlaceholderAPIExpansion(plugin).register();
        }
    }
}
