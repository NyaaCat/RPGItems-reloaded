package think.rpgitems.api;

import com.sk89q.worldedit.bukkit.entity.BukkitItem;
import org.bukkit.Bukkit;
import think.rpgitems.api.power.IPower;

import java.util.Map;

/**
 * Public API of RPGItem. Usage:
 *      RPGItemAPI rpgitemPlugin = (RPGItemAPI)Bukkit.getPluginManager().getPlugin(RPGItemAPI.PLUGIN_NAME);
 */
public interface RPGItemAPI {
    String PLUGIN_NAME = "RPGItems";

    void registerPower(IPower power);

    void registerLang(Map<String, String> map);
}
