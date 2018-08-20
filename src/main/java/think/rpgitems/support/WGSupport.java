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

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import think.rpgitems.RPGItems;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WGSupport {

    public static boolean useWorldGuard = true;
    public static boolean useCustomFlag = true;
    public static boolean forceRefresh = false;
    public static Map<UUID, Collection<String>> disabledPowerByPlayer;
    public static Map<UUID, Collection<String>> enabledPowerByPlayer;
    public static Map<UUID, Collection<String>> disabledItemByPlayer;
    public static Map<UUID, Collection<String>> enabledItemByPlayer;
    public static Map<UUID, Boolean> disabledByPlayer;
    static WorldGuardPlugin wgPlugin;
    static WorldEditPlugin wePlugin;
    static WorldGuard worldGuardInstance;
    private static Plugin plugin;
    private static boolean hasSupport = false;
    private static FileConfiguration config;

    public static void load() {
        if (!useWorldGuard || !useCustomFlag) {
            return;
        }
        try {
            WGHandler.init();
        } catch (NoClassDefFoundError ignored) { }
    }

    public static void init(RPGItems pl) {
        plugin = pl;
        Plugin wgPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        Plugin wePlugin = plugin.getServer().getPluginManager().getPlugin("WorldEdit");
        useWorldGuard = plugin.getConfig().getBoolean("support.worldguard", false);
        useCustomFlag = plugin.getConfig().getBoolean("support.wgcustomflag", true);
        forceRefresh = plugin.getConfig().getBoolean("support.wgforcerefresh", false);
        if (!(wgPlugin instanceof WorldGuardPlugin) || !(wePlugin instanceof WorldEditPlugin)) {
            return;
        }
        hasSupport = true;
        WGSupport.wgPlugin = (WorldGuardPlugin) wgPlugin;
        WGSupport.wePlugin = (WorldEditPlugin) wePlugin;
        worldGuardInstance = WorldGuard.getInstance();
        RPGItems.logger.info("[RPGItems] WorldGuard version: " + WGSupport.wgPlugin.getDescription().getVersion() + " found");
        WGHandler.registerHandler();
        disabledPowerByPlayer = new HashMap<>();
        enabledPowerByPlayer = new HashMap<>();
        disabledItemByPlayer = new HashMap<>();
        enabledItemByPlayer = new HashMap<>();
        disabledByPlayer = new HashMap<>();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            WGHandler.refreshPlayerWG(p);
        }
    }

    public static void reload() {
        hasSupport = false;
        if (useCustomFlag) {
            try {
                WGHandler.unregisterHandler();
            } catch (NoClassDefFoundError ignored) {
            }
        }
        useWorldGuard = plugin.getConfig().getBoolean("support.worldguard", false);
        useCustomFlag = plugin.getConfig().getBoolean("support.wgcustomflag", true);
        forceRefresh = plugin.getConfig().getBoolean("support.wgforcerefresh", false);
        if (wgPlugin == null) {
            return;
        }
        hasSupport = true;
        WGSupport.wgPlugin = (WorldGuardPlugin) plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        RPGItems.logger.info("[RPGItems] WorldGuard version: " + WGSupport.wgPlugin.getDescription().getVersion() + " found");

        WGHandler.registerHandler();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            WGHandler.refreshPlayerWG(p);
        }
    }

    public static boolean isEnabled() {
        return hasSupport;
    }

    public static boolean canPvP(Player player) {
        if (!hasSupport || !useWorldGuard || useCustomFlag)
            return true;

        LocalPlayer localPlayer = wgPlugin.wrapPlayer(player);
        State stat = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().queryState(localPlayer.getLocation(), localPlayer, Flags.PVP);
        return stat == null || stat.equals(State.ALLOW);
    }

    public static boolean check(Player player, RPGItem item, Collection<? extends Power> powers) {
        if (!hasSupport || !useWorldGuard) {
            return true;
        }
        if (forceRefresh) WGHandler.refreshPlayerWG(player);
        // TODO
        return true;
    }

    public static void unload() {
        if (!hasSupport || !useWorldGuard || !useCustomFlag) {
            return;
        }
        WGHandler.unregisterHandler();
    }
}
