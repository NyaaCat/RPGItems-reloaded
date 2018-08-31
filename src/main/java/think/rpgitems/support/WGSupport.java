package think.rpgitems.support;

import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import think.rpgitems.RPGItems;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WGSupport {

    public static boolean useWorldGuard = true;
    public static boolean forceRefresh = false;
    static Map<UUID, Collection<String>> disabledPowerByPlayer;
    static Map<UUID, Collection<String>> enabledPowerByPlayer;
    static Map<UUID, Collection<String>> disabledItemByPlayer;
    static Map<UUID, Collection<String>> enabledItemByPlayer;
    static Map<UUID, Boolean> disabledByPlayer;
    static WorldGuardPlugin wgPlugin;
    static WorldGuard worldGuardInstance;
    private static Plugin plugin;
    private static boolean hasSupport = false;

    public static void load() {
        try {
            worldGuardInstance = WorldGuard.getInstance();
            WGHandler.init();
        } catch (NoClassDefFoundError ignored) {
        }
    }

    public static void init(RPGItems pl) {
        plugin = pl;
        Plugin wgPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        useWorldGuard = plugin.getConfig().getBoolean("support.worldguard", false);
        forceRefresh = plugin.getConfig().getBoolean("support.wgforcerefresh", false);
        if (!(wgPlugin instanceof WorldGuardPlugin)) {
            return;
        }
        hasSupport = true;
        WGSupport.wgPlugin = (WorldGuardPlugin) wgPlugin;
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
        File file = new File(plugin.getDataFolder(), "worldguard_region.yml");
        if (!file.exists()) return;
        Configuration legacyConfig = YamlConfiguration.loadConfiguration(file);
        Map<String, Object> values = legacyConfig.getValues(false);
        values.forEach(
                (k, v) -> {
                    String[] split = k.split("\\.", 2);
                    String worldName = split[0];
                    String regionName = split[1];
                    boolean flag = Boolean.valueOf(v.toString());
                    if (Bukkit.getServer().getWorld(worldName) == null) return;
                    World world = worldGuardInstance.getPlatform().getWorldByName(worldName);
                    RegionManager regionManager = worldGuardInstance.getPlatform().getRegionContainer().get(world);
                    if (regionManager == null) return;
                    ProtectedRegion region = regionManager.getRegion(regionName);
                    if (region == null) return;
                    if (!flag) {
                        region.setFlag(WGHandler.disabledItem, Collections.singleton("*"));
                    } else {
                        region.setFlag(WGHandler.enabledItem, Collections.singleton("*"));
                    }
                }
        );
        Path path = file.toPath();
        Path bak = path.resolveSibling("worldguard_region.bak");
        try {
            Files.move(path, bak);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reload() {
        hasSupport = false;
        try {
            WGHandler.unregisterHandler();
        } catch (NoClassDefFoundError ignored) {
        }
        useWorldGuard = plugin.getConfig().getBoolean("support.worldguard", false);
        forceRefresh = plugin.getConfig().getBoolean("support.wgforcerefresh", false);
        if (wgPlugin == null) {
            return;
        }
        hasSupport = true;
        worldGuardInstance = WorldGuard.getInstance();
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

    public static boolean canNotPvP(Player player) {
        if (!hasSupport || !useWorldGuard)
            return false;

        LocalPlayer localPlayer = wgPlugin.wrapPlayer(player);
        State stat = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().queryState(localPlayer.getLocation(), localPlayer, Flags.PVP);
        return stat != null && !stat.equals(State.ALLOW);
    }

    public static boolean check(Player player, RPGItem item, Collection<? extends Power> powers) {
        if (!hasSupport || !useWorldGuard) {
            return true;
        }
        if (forceRefresh) WGHandler.refreshPlayerWG(player);
        Boolean disabled = disabledByPlayer.get(player.getUniqueId());
        if (disabled != null && disabled) {
            return false;
        }
        Collection<String> disabledPower = disabledPowerByPlayer.get(player.getUniqueId());
        Collection<String> enabledPower = enabledPowerByPlayer.get(player.getUniqueId());
        Collection<String> disabledItem = disabledItemByPlayer.get(player.getUniqueId());
        Collection<String> enabledItem = enabledItemByPlayer.get(player.getUniqueId());

        String itemName = item.getName();
        if (notEnabled(disabledItem, enabledItem, itemName)) return false;

        for (Power power : powers) {
            String powerName = power.getNamespacedKey().toString();
            if (notEnabled(disabledPower, enabledPower, powerName)) return false;
        }
        return true;
    }

    private static boolean notEnabled(Collection<String> disabled, Collection<String> enabled, String name) {
        if (enabled == null || enabled.isEmpty()) {
            return disabled != null && (disabled.contains(name) || disabled.contains("*") || disabled.contains("all"));
        } else return !(enabled.contains(name) || enabled.contains("*"));
    }

    public static void unload() {
        if (!hasSupport) {
            return;
        }
        WGHandler.unregisterHandler();
    }
}
