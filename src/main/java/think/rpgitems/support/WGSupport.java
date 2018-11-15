package think.rpgitems.support;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import think.rpgitems.RPGItems;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WGSupport {

    public static boolean useWorldGuard = true;
    public static boolean forceRefresh = false;
    static Map<UUID, Collection<String>> disabledPowerByPlayer;
    static Map<UUID, Collection<String>> enabledPowerByPlayer;
    static Map<UUID, Collection<String>> disabledItemByPlayer;
    static Map<UUID, Collection<String>> enabledItemByPlayer;
    static Map<UUID, Boolean> disabledByPlayer;
    static WorldGuardPlugin wgPlugin;
    private static RPGItems plugin;
    private static boolean hasSupport = false;

    public static void load() {
        try {
            WGHandler.init();
        } catch (NoClassDefFoundError ignored) {
        }
    }

    public static void init(RPGItems pl) {
        try {
            plugin = pl;
            Plugin wgPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            useWorldGuard = plugin.cfg.useWorldGuard;
            forceRefresh = plugin.cfg.wgForceRefresh;
            if (!(wgPlugin instanceof WorldGuardPlugin)) {
                return;
            }
            WGSupport.wgPlugin = (WorldGuardPlugin) wgPlugin;
            String wgVersion = WGSupport.wgPlugin.getDescription().getVersion();
            RPGItems.logger.info("[RPGItems] WorldGuard version: " + wgVersion + " found");
            if (!wgVersion.startsWith("7.")){
                RPGItems.logger.warning("[RPGItems] Requires WorldGuard 7.x, disabling integration");
            }
            hasSupport = true;
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
            if (file.exists() && !file.isDirectory()) {
                WGHandler.migrate(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            RPGItems.logger.info("[RPGItems] Error enabling WorldGuard support");
        }
    }

    public static void reload() {
        hasSupport = false;
        try {
            WGHandler.unregisterHandler();
        } catch (NoClassDefFoundError ignored) {
        }
        useWorldGuard = plugin.cfg.useWorldGuard;
        forceRefresh = plugin.cfg.wgForceRefresh;
        if (wgPlugin == null) {
            return;
        }
        hasSupport = true;
        WGHandler.worldGuardInstance = WorldGuard.getInstance();
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

    private static boolean canNotPvP(Player player) {
        if (!hasSupport || !useWorldGuard)
            return false;

        LocalPlayer localPlayer = wgPlugin.wrapPlayer(player);
        State stat = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().queryState(localPlayer.getLocation(), localPlayer, Flags.PVP);
        return stat != null && !stat.equals(State.ALLOW);
    }

    public static boolean canUse(Player player, RPGItem item, Collection<? extends Power> powers) {
        if (canNotPvP(player)) return false;
        if (item == null) return true;
        if (!hasSupport || !useWorldGuard || item.ignoreWorldGuard) {
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
        if (powers == null) return true;
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
