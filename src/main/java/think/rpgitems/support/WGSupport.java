package think.rpgitems.support;

import cat.nyaa.nyaacore.utils.HexColorUtils;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import think.rpgitems.RPGItems;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Pimpl;
import think.rpgitems.power.RPGItemsPowersPreFireEvent;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class WGSupport {

    public static boolean useWorldGuard = true;
    public static boolean forceRefresh = false;
    static Map<UUID, String> warningMessageByPlayer;
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
            useWorldGuard = plugin.cfg.useWorldGuard;
            forceRefresh = plugin.cfg.wgForceRefresh;
            Plugin wgPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            if (!useWorldGuard || !(wgPlugin instanceof WorldGuardPlugin)) {
                return;
            }
            WGSupport.wgPlugin = (WorldGuardPlugin) wgPlugin;
            String wgVersion = WGSupport.wgPlugin.getDescription().getVersion();
            RPGItems.logger.info("WorldGuard version: " + wgVersion + " found");
            if (!wgVersion.startsWith("7.")) {
                RPGItems.logger.warning("Requires WorldGuard 7.0.0-beta2 or later, disabling integration");
                hasSupport = false;
                return;
            }
            if (wgVersion.contains("0dc5781") && plugin.getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
                RPGItems.logger.warning("FastAsyncWorldEdit's WorldGuard is not supported, disabling integration");
                hasSupport = false;
                return;
            }
            hasSupport = true;
            WGHandler.registerHandler();
            warningMessageByPlayer = new HashMap<>();
            disabledPowerByPlayer = new HashMap<>();
            enabledPowerByPlayer = new HashMap<>();
            disabledItemByPlayer = new HashMap<>();
            enabledItemByPlayer = new HashMap<>();
            disabledByPlayer = new HashMap<>();
            Bukkit.getPluginManager().registerEvents(new EventListener(), plugin);
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                WGHandler.refreshPlayerWG(p);
            }
            File file = new File(plugin.getDataFolder(), "worldguard_region.yml");
            if (file.exists() && !file.isDirectory()) {
                WGHandler.migrate(file);
            }
        } catch (Exception e) {
            RPGItems.logger.log(Level.WARNING, "Error enabling WorldGuard support", e);
            hasSupport = false;
        }
    }

    public static void reload() {
        hasSupport = false;
        try {
            unload();
        } catch (NoClassDefFoundError ignored) {
        }
        init(plugin);
    }

    public static boolean hasSupport() {
        return hasSupport;
    }

    private static Event.Result canPvP(Player player) {
        if (!hasSupport || !useWorldGuard)
            return Event.Result.ALLOW;

        LocalPlayer localPlayer = wgPlugin.wrapPlayer(player);
        State stat = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().queryState(localPlayer.getLocation(), localPlayer, Flags.PVP);
        return (stat == null || stat.equals(State.ALLOW)) ? Event.Result.ALLOW : Event.Result.DENY;
    }

    private static Event.Result canUse(Player player, RPGItem item, Collection<? extends Pimpl> powers) {
        if (!hasSupport || !useWorldGuard) {
            return Event.Result.DEFAULT;
        }
        if (plugin.cfg.wgNoPvP && canPvP(player) == Event.Result.DENY) return Event.Result.DENY;
        if (forceRefresh) WGHandler.refreshPlayerWG(player);
        Boolean disabled = disabledByPlayer.get(player.getUniqueId());
        if (disabled != null && disabled) {
            return Event.Result.DENY;
        }
        Collection<String> disableds = disabledItemByPlayer.get(player.getUniqueId());
        Collection<String> enableds = enabledItemByPlayer.get(player.getUniqueId());

        if (disableds != null && disableds.contains("*")) {
            return Event.Result.DENY;
        }
        if (item == null || item.isIgnoreWorldGuard()) {
            return Event.Result.ALLOW;
        }
        List<String> disabledItems = disableds == null ? null : disableds.stream().map(ItemManager::getItems).flatMap(Set::stream).map(RPGItem::getName).collect(Collectors.toList());
        List<String> enabledItems = enableds == null ? null : enableds.stream().map(ItemManager::getItems).flatMap(Set::stream).map(RPGItem::getName).collect(Collectors.toList());
        String itemName = item.getName();
        if (notEnabled(disabledItems, enabledItems, itemName)) return Event.Result.DENY;

        Collection<String> disabledPower = disabledPowerByPlayer.get(player.getUniqueId());
        Collection<String> enabledPower = enabledPowerByPlayer.get(player.getUniqueId());

        if (powers == null) return Event.Result.ALLOW;
        for (Pimpl power : powers) {
            String powerName = item.getPropertyHolderKey(power.getPower()).toString();
            if (notEnabled(disabledPower, enabledPower, powerName)) return Event.Result.DENY;
        }
        return Event.Result.ALLOW;
    }

    public static Event.Result canUse(Player player, RPGItem item, Collection<? extends Pimpl> powers, boolean showWarn) {
        Event.Result result = canUse(player, item, powers);

        if (result == Event.Result.DENY && showWarn) {
            String message = warningMessageByPlayer.get(player.getUniqueId());
            if (message != null) {
                player.sendMessage(HexColorUtils.hexColored(message));
            }
        }
        return result;
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

    public static class EventListener implements Listener {
        @EventHandler
        public <TEvent extends Event, TPower extends Pimpl, TResult, TReturn> void onPreTrigger(RPGItemsPowersPreFireEvent<TEvent, TPower, TResult, TReturn> event) {
            if (canUse(event.getPlayer(), event.getItem(), event.getPowers(), plugin.cfg.wgShowWarning) == Event.Result.DENY) {
                event.setCancelled(true);
            }
        }
    }
}
