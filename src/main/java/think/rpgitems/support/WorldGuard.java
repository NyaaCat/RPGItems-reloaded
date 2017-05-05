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
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import think.rpgitems.RPGItems;
import think.rpgitems.power.types.Power;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldGuard {

    public static boolean useWorldGuard = true;
    public static boolean useCustomFlag = true;
    public static boolean forceRefresh = false;
    public static Map<UUID, Collection<String>> disabledNowByPlayer;
    static WorldGuardPlugin wgPlugin;
    static int majorVersion;
    static int minorVersion;
    static int pointVersion;
    private static Plugin plugin;
    private static boolean hasSupport = false;
    private static FileConfiguration config;

    public static void load() {
        if (!useWorldGuard || !useCustomFlag) {
            return;
        }
        try {
            WGHandler.init();
        } catch (NoClassDefFoundError ignored) {

        }
    }

    public static void init(RPGItems pl) {
        plugin = pl;
        Plugin wgPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        useWorldGuard = plugin.getConfig().getBoolean("support.worldguard", false);
        useCustomFlag = plugin.getConfig().getBoolean("support.wgcustomflag", true);
        forceRefresh = plugin.getConfig().getBoolean("support.wgforcerefresh", false);
        if (wgPlugin == null || !(wgPlugin instanceof WorldGuardPlugin)) {
            return;
        }
        hasSupport = true;
        WorldGuard.wgPlugin = (WorldGuardPlugin) wgPlugin;
        majorVersion = Character.digit(WorldGuard.wgPlugin.getDescription().getVersion().charAt(0), 9);
        minorVersion = Character.digit(WorldGuard.wgPlugin.getDescription().getVersion().charAt(2), 9);
        pointVersion = Character.digit(WorldGuard.wgPlugin.getDescription().getVersion().charAt(4), 9);
        RPGItems.logger.info("[RPGItems] WorldGuard version " + majorVersion + "." + minorVersion + "." + pointVersion + " : " + WorldGuard.wgPlugin.getDescription().getVersion() + " found");
        loadConfig();
        if (!(WorldGuard.majorVersion > 6 || (WorldGuard.majorVersion == 6 && WorldGuard.minorVersion >= 2) || (WorldGuard.majorVersion == 6 && WorldGuard.minorVersion == 1 && WorldGuard.pointVersion >= 3))) {
            useCustomFlag = false;
        }
        if (!useCustomFlag) {
            return;
        }
        WGHandler.registerHandler();
        disabledNowByPlayer = new HashMap<>();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            WGHandler.refreshPlayerWG(p);
        }
    }

    public static void reload() {
        if (useCustomFlag) {
            WGHandler.unregisterHandler();
        }
        useWorldGuard = plugin.getConfig().getBoolean("support.worldguard", false);
        useCustomFlag = plugin.getConfig().getBoolean("support.wgcustomflag", true);
        forceRefresh = plugin.getConfig().getBoolean("support.wgforcerefresh", false);
        if (wgPlugin == null) {
            return;
        }
        hasSupport = true;
        WorldGuard.wgPlugin = (WorldGuardPlugin) plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        majorVersion = Character.digit(WorldGuard.wgPlugin.getDescription().getVersion().charAt(0), 9);
        minorVersion = Character.digit(WorldGuard.wgPlugin.getDescription().getVersion().charAt(2), 9);
        RPGItems.logger.info("[RPGItems] WorldGuard version " + majorVersion + "." + minorVersion + " : " + WorldGuard.wgPlugin.getDescription().getVersion() + " found");
        loadConfig();
        if (!(WorldGuard.majorVersion > 6 || (WorldGuard.majorVersion == 6 && WorldGuard.minorVersion >= 2) || (WorldGuard.majorVersion == 6 && WorldGuard.minorVersion == 1 && WorldGuard.pointVersion >= 3))) {
            useCustomFlag = false;
        }
        if (!useCustomFlag) {
            return;
        }
        WGHandler.registerHandler();
        disabledNowByPlayer = new HashMap<>();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            WGHandler.refreshPlayerWG(p);
        }
    }

    public static boolean isEnabled() {
        return hasSupport;
    }

    public static boolean canBuild(Player player, Location location) {
        return !hasSupport || !useWorldGuard || wgPlugin.canBuild(player, location);
    }

    public static boolean canPvP(Player player) {
        if (!hasSupport || !useWorldGuard || useCustomFlag)
            return true;

        State stat = wgPlugin.getRegionContainer().createQuery().queryState(player.getLocation(), player, DefaultFlag.PVP);
        return stat == null || stat.equals(State.ALLOW);
    }

    private static void loadConfig() {
        config = null;
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "worldguard_region.yml"));
    }

    public static boolean canUseRPGItem(Location loc) {
        if (!hasSupport || !useWorldGuard) {
            return true;
        }
        String worldName = loc.getWorld().getName();
        RegionContainer container = wgPlugin.getRegionContainer();
        RegionManager regions = container.get(loc.getWorld());
        ApplicableRegionSet set = regions.getApplicableRegions(BukkitUtil.toVector(loc));
        if (!set.isVirtual()) {
            for (ProtectedRegion region : set) {
                String regionName = region.getId();
                if (hasRPGItemFlag(worldName, regionName)) {
                    return getRPGItemFlag(worldName, regionName);
                }
            }
        }
        return !hasRPGItemFlag(worldName, "__global__") || getRPGItemFlag(worldName, "__global__");
    }

    public static boolean canUsePowerNow(Player player, Power pow) {
        if (!hasSupport || !useWorldGuard) {
            return true;
        }
        if (!useCustomFlag) return canUseRPGItem(player);
        if (forceRefresh) WGHandler.refreshPlayerWG(player);
        String name = think.rpgitems.power.Power.powers.inverse().get(pow.getClass());
        if (disabledNowByPlayer == null) return true;
        Collection<String> ban = disabledNowByPlayer.get(player.getUniqueId());
        return !(ban != null && (ban.contains(name) || ban.contains("all")));
    }

    public static boolean canUseRPGItem(Player player) {
        return canUseRPGItem(player.getLocation());
    }

    public static void setRPGItemFlag(String worldName, String regionName, Boolean flag) {
        config.set(worldName + "." + regionName, flag);
        saveConfig();
    }

    public static boolean hasRPGItemFlag(String worldName, String regionName) {
        return config.contains(worldName + "." + regionName);
    }

    public static boolean getRPGItemFlag(String worldName, String regionName) {
        return config.getBoolean(worldName + "." + regionName);
    }

    public static void removeRPGItemFlag(String worldName, String regionName) {
        config.set(worldName + "." + regionName, null);
        saveConfig();
    }

    private static void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), "worldguard_region.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void unload() {
        if (!hasSupport || !useWorldGuard || !useCustomFlag) {
            return;
        }
        WGHandler.unregisterHandler();
    }
}
