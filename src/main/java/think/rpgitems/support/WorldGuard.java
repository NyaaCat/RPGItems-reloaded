/*
 *  This file is part of RPG Items.
 *
 *  RPG Items is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RPG Items is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RPG Items.  If not, see <http://www.gnu.org/licenses/>.
 */
package think.rpgitems.support;

import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class WorldGuard {

    private static Plugin plugin;
    private static WorldGuardPlugin wgPlugin;
    private static boolean hasSupport = false;
    private static int majorVersion;
    private static FileConfiguration config;
    public static boolean useWorldGuard = true;

    public static void init(think.rpgitems.Plugin pl) {
        plugin = pl;
        Plugin wgPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        useWorldGuard = plugin.getConfig().getBoolean("support.worldguard", false);
        if (wgPlugin == null || !(wgPlugin instanceof WorldGuardPlugin)) {
            return;
        }
        hasSupport = true;
        WorldGuard.wgPlugin = (WorldGuardPlugin) wgPlugin;
        majorVersion = Character.digit(wgPlugin.getDescription().getVersion().charAt(0), 9);
        think.rpgitems.Plugin.logger.info("[RPGItems] WorldGuard version " + majorVersion + " found");
        loadConfig();
    }
    
    public static void reload() {
        useWorldGuard = plugin.getConfig().getBoolean("support.worldguard", false);
        if (wgPlugin == null || !(wgPlugin instanceof WorldGuardPlugin)) {
            return;
        }
        hasSupport = true;
        WorldGuard.wgPlugin = (WorldGuardPlugin) wgPlugin;
        majorVersion = Character.digit(wgPlugin.getDescription().getVersion().charAt(0), 9);
        think.rpgitems.Plugin.logger.info("[RPGItems] WorldGuard version " + majorVersion + " found");
        loadConfig();
    }

    public static boolean isEnabled() {
        return hasSupport;
    }

    public static boolean canBuild(Player player, Location location) {
        if (!hasSupport || !useWorldGuard)
            return true;
        return wgPlugin.canBuild(player, location);
    }

    @SuppressWarnings("deprecation")
    public static boolean canPvP(Player player) {
        if (!hasSupport || !useWorldGuard)
            return true;
        return wgPlugin.getGlobalRegionManager().allows(DefaultFlag.PVP, player.getLocation(), wgPlugin.wrapPlayer(player));
    }

    public static void loadConfig() {
        config = null;
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "worldguard_region.yml"));
    }

    public static boolean canUseRPGItem(Location loc) {
        if (!hasSupport || !useWorldGuard) {
            return true;
        }
        String worldName = loc.getWorld().getName();
        RegionManager regions = wgPlugin.getGlobalRegionManager().get(loc.getWorld());
        ApplicableRegionSet set = regions.getApplicableRegions(BukkitUtil.toVector(loc));
        for (ProtectedRegion region : set) {
            String regionName = region.getId();
            if (hasRPGItemFlag(worldName, regionName)) {
                return getRPGItemFlag(worldName, regionName);
            }
        }
        if (hasRPGItemFlag(worldName, "__global__")) {
            return getRPGItemFlag(worldName, "__global__");
        }
        return true;
    }

    public static boolean canUseRPGItem(Player player) {
        return canUseRPGItem(player.getLocation());
    }

    public static void setRPGItemFlag(String worldName, String regionName, Boolean flag) {
        config.set(worldName + "." + regionName, flag);
        saveConfig();
        return;
    }

    public static boolean hasRPGItemFlag(String worldName, String regionName) {
        if (config.contains(worldName + "." + regionName)) {
            return true;
        }
        return false;
    }

    public static boolean getRPGItemFlag(String worldName, String regionName) {
        return config.getBoolean(worldName + "." + regionName);
    }

    public static void removeRPGItemFlag(String worldName, String regionName) {
        config.set(worldName + "." + regionName, null);
        saveConfig();
        return;
    }

    public static void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), "worldguard_region.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

}
