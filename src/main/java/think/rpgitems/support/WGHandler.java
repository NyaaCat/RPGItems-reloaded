package think.rpgitems.support;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
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
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import think.rpgitems.RPGItems;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import static think.rpgitems.support.WGSupport.worldGuardInstance;

public class WGHandler extends Handler {

    static final SetFlag<String> disabledPower = new SetFlag<>("disabled-rpg-powers", new StringFlag(null));
    static final SetFlag<String> enabledPower = new SetFlag<>("enabled-rpg-powers", new StringFlag(null));
    static final SetFlag<String> disabledItem = new SetFlag<>("disabled-rpg-items", new StringFlag(null));
    static final SetFlag<String> enabledItem = new SetFlag<>("enabled-rpg-items", new StringFlag(null));
    static final SetFlag<String> disabledPlayer = new SetFlag<>("disabled-rpg-players", new StringFlag(null));
    static final SetFlag<String> enabledPlayer = new SetFlag<>("enabled-rpg-players", new StringFlag(null));

    private static final Factory FACTORY = new Factory();

    private WGHandler(Session session) {
        super(session);
    }

    public static void init() {
        FlagRegistry registry = worldGuardInstance.getFlagRegistry();
        try {
            registry.register(disabledPower);
            registry.register(enabledPower);
            registry.register(disabledItem);
            registry.register(enabledItem);
            registry.register(disabledPlayer);
            registry.register(enabledPlayer);
            RPGItems.logger.info("[RPGItems] WorldGuard custom flags registered");
        } catch (FlagConflictException e) {
            e.printStackTrace();
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
        ApplicableRegionSet set = regions.getApplicableRegions(localPlayer.getLocation().toVector());
        refresh(set, localPlayer);
    }

    private static void refresh(ApplicableRegionSet set, LocalPlayer localPlayer) {
        UUID uuid = localPlayer.getUniqueId();
        Collection<String> dp = set.queryValue(localPlayer, disabledPower);
        Collection<String> ep = set.queryValue(localPlayer, enabledPower);
        Collection<String> di = set.queryValue(localPlayer, disabledItem);
        Collection<String> ei = set.queryValue(localPlayer, enabledItem);
        Collection<String> du = set.queryValue(localPlayer, disabledPlayer);
        Collection<String> eu = set.queryValue(localPlayer, enabledPlayer);
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
