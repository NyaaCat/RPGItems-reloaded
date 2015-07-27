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

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;

public class WorldGuard {

    private static WorldGuardPlugin plugin;
    private static boolean hasSupport = false;
    private static int majorVersion;
    public static boolean useWorldGuard = true;

    public static void init(think.rpgitems.Plugin plugin2) {
        Plugin plugin = plugin2.getServer().getPluginManager().getPlugin("WorldGuard");
        useWorldGuard = plugin2.getConfig().getBoolean("support.worldguard", false);
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return;
        }
        hasSupport = true;
        WorldGuard.plugin = (WorldGuardPlugin) plugin;
        majorVersion = Character.digit(plugin.getDescription().getVersion().charAt(0), 9);
        think.rpgitems.Plugin.logger.info("[RPGItems] World Guard version " + majorVersion + " found");
    }

    public static boolean isEnabled() {
        return hasSupport;
    }

    public static boolean canBuild(Player player, Location location) {
        if (!hasSupport || !useWorldGuard)
            return true;
        return plugin.canBuild(player, location);
    }

    @SuppressWarnings("deprecation")
    public static boolean canPvP(Player player) {
        if (!hasSupport || !useWorldGuard)
            return true;
        
        if(majorVersion >= 6)
            return plugin.getRegionContainer().createQuery().queryState(player.getLocation(), player, DefaultFlag.PVP).equals(State.ALLOW);
        else
            return plugin.getGlobalRegionManager().allows(DefaultFlag.PVP, player.getLocation(), plugin.wrapPlayer(player));
    }
}
