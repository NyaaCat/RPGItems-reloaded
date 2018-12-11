package think.rpgitems.support;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.SetFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.SessionManager;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import think.rpgitems.RPGItems;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

public class WGHandler extends Handler {

    private static final StringFlag warningMessage = new StringFlag("rpgitems-warning");
    private static final SetFlag<String> disabledPower = new SetFlag<>("disabled-rpg-powers", new StringFlag(null));
    private static final SetFlag<String> enabledPower = new SetFlag<>("enabled-rpg-powers", new StringFlag(null));
    private static final SetFlag<String> disabledItem = new SetFlag<>("disabled-rpg-items", new StringFlag(null));
    private static final SetFlag<String> enabledItem = new SetFlag<>("enabled-rpg-items", new StringFlag(null));
    private static final SetFlag<String> disabledPlayer = new SetFlag<>("disabled-rpg-players", new StringFlag(null));
    private static final SetFlag<String> enabledPlayer = new SetFlag<>("enabled-rpg-players", new StringFlag(null));

    private static final Factory FACTORY = new Factory();
    static WorldGuard worldGuardInstance;

    private WGHandler(Session session) {
        super(session);
    }

    public static void init() {
        worldGuardInstance = WorldGuard.getInstance();
        FlagRegistry registry = worldGuardInstance.getFlagRegistry();
        try {
            registry.register(warningMessage);
            registry.register(disabledPower);
            registry.register(enabledPower);
            registry.register(disabledItem);
            registry.register(enabledItem);
            registry.register(disabledPlayer);
            registry.register(enabledPlayer);
            RPGItems.logger.info("WorldGuard custom flags registered");
        } catch (FlagConflictException e) {
            WGSupport.useWorldGuard = false;
            RPGItems.plugin.getLogger().log(Level.SEVERE, "Error WorldGuard registering custom flags", e);
        }
    }

    static void migrate(File file) {
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
            RPGItems.plugin.getLogger().log(Level.WARNING, "Error moving worldguard_region to backup", e);
        }
    }

    public static void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        refreshPlayerWG(p);
    }

    static void refreshPlayerWG(Player p) {
        LocalPlayer localPlayer = WGSupport.wgPlugin.wrapPlayer(p);
        RegionContainer container = worldGuardInstance.getPlatform().getRegionContainer();
        RegionManager regions = container.get(localPlayer.getWorld());
        if (regions == null) return;
        ApplicableRegionSet set = regions.getApplicableRegions(localPlayer.getLocation().toVector().toBlockPoint());
        refresh(set, localPlayer);
    }

    private static void refresh(ApplicableRegionSet set, LocalPlayer localPlayer) {
        UUID uuid = localPlayer.getUniqueId();
        String wm = set.queryValue(localPlayer, warningMessage);
        Collection<String> dp = set.queryValue(localPlayer, disabledPower);
        Collection<String> ep = set.queryValue(localPlayer, enabledPower);
        Collection<String> di = set.queryValue(localPlayer, disabledItem);
        Collection<String> ei = set.queryValue(localPlayer, enabledItem);
        Collection<String> du = set.queryValue(localPlayer, disabledPlayer);
        Collection<String> eu = set.queryValue(localPlayer, enabledPlayer);
        WGSupport.warningMessageByPlayer.put(uuid, wm);
        WGSupport.disabledPowerByPlayer.put(uuid, dp);
        WGSupport.enabledPowerByPlayer.put(uuid, ep);
        WGSupport.disabledItemByPlayer.put(uuid, di);
        WGSupport.enabledItemByPlayer.put(uuid, ei);
        if (eu == null || eu.isEmpty()) {
            WGSupport.disabledByPlayer.put(uuid, du != null && du.contains(uuid.toString()));
        } else {
            WGSupport.disabledByPlayer.put(uuid, !eu.contains(uuid.toString()));
        }
    }

    static void registerHandler() {
        SessionManager sessionManager = worldGuardInstance.getPlatform().getSessionManager();
        sessionManager.registerHandler(FACTORY, null);
    }

    static void unregisterHandler() {
        SessionManager sessionManager = worldGuardInstance.getPlatform().getSessionManager();
        sessionManager.unregisterHandler(FACTORY);
    }

    @Override
    public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType) {
        if (entered.isEmpty() && exited.isEmpty()) return true;
        refresh(toSet, player);
        return true;
    }

    public static class Factory extends Handler.Factory<WGHandler> {
        @Override
        public WGHandler create(Session session) {
            return new WGHandler(session);
        }
    }
}
