package think.rpgitems.support;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.SetFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.SessionManager;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collection;
import java.util.Set;

import static org.bukkit.Bukkit.getServer;
import static think.rpgitems.support.WorldGuard.useCustomFlag;

public class WGHandler extends Handler {

    private static final SetFlag<String> disabledPower = new SetFlag<>("disabled-rpg-powers", new StringFlag(null));

    private static final Factory FACTORY = new Factory();

    private WGHandler(Session session) {
        super(session);
    }

    public static void init() {
        WorldGuardPlugin wg = (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");
        WorldGuard.majorVersion = Character.digit(wg.getDescription().getVersion().charAt(0), 9);
        WorldGuard.minorVersion = Character.digit(wg.getDescription().getVersion().charAt(2), 9);
        WorldGuard.pointVersion = Character.digit(wg.getDescription().getVersion().charAt(4), 9);
        if (!(WorldGuard.majorVersion > 6 || (WorldGuard.majorVersion == 6 && WorldGuard.minorVersion >= 2) || (WorldGuard.majorVersion == 6 && WorldGuard.minorVersion == 1 && WorldGuard.pointVersion >= 3))) {
            useCustomFlag = false;
        }
        if (useCustomFlag) {
            FlagRegistry registry = wg.getFlagRegistry();
            try {
                registry.register(disabledPower);
                think.rpgitems.Plugin.logger.info("[RPGItems] WorldGuard custom flag disabled-rpg-powers registered");
            } catch (FlagConflictException e) {
                e.printStackTrace();
                useCustomFlag = false;
            }
        }
    }

    public static void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        refreshPlayerWG(p);
    }

    public static void refreshPlayerWG(Player p) {
        Location loc = p.getLocation();
        RegionContainer container = WorldGuard.wgPlugin.getRegionContainer();
        RegionManager regions = container.get(loc.getWorld());
        if(regions == null)return;
        ApplicableRegionSet set = regions.getApplicableRegions(BukkitUtil.toVector(loc));
        LocalPlayer localPlayer = WorldGuard.wgPlugin.wrapPlayer(p);
        Collection<String> disabled = set.queryValue(localPlayer, disabledPower);
        WorldGuard.disabledNowByPlayer.put(p.getUniqueId(), disabled);
    }

    public static void registerHandler() {
        SessionManager sessionManager = WorldGuard.wgPlugin.getSessionManager();
        sessionManager.registerHandler(FACTORY, null);
    }

    public static void unregisterHandler() {
        SessionManager sessionManager = WorldGuard.wgPlugin.getSessionManager();
        sessionManager.unregisterHandler(FACTORY);
    }

    @Override
    public boolean onCrossBoundary(Player player, Location from, Location to, ApplicableRegionSet toSet, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType) {
        if (entered.isEmpty() && exited.isEmpty()) return true;
        Collection<String> disabled = toSet.queryValue(getPlugin().wrapPlayer(player), disabledPower);
        WorldGuard.disabledNowByPlayer.put(player.getUniqueId(), disabled);
        return true;
    }

    public static class Factory extends Handler.Factory<WGHandler> {
        @Override
        public WGHandler create(Session session) {
            return new WGHandler(session);
        }
    }
}
