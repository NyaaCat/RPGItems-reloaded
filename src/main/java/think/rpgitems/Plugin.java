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
package think.rpgitems;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import think.rpgitems.commands.Commands;
import think.rpgitems.commands.RPGItemUpdateCommandHandler;
import think.rpgitems.config.ConfigUpdater;
import think.rpgitems.data.Font;
import think.rpgitems.data.Locale;
import think.rpgitems.item.ItemManager;
import think.rpgitems.power.*;
import think.rpgitems.support.WorldGuard;

import java.util.List;
import java.util.logging.Logger;

public class Plugin extends JavaPlugin {

    public static Logger logger = Logger.getLogger("RPGItems");
    public static Plugin plugin;
    public static Events listener;
    public static Updater updater;

    @Override
    public void onLoad() {
        plugin = this;
        saveDefaultConfig();
        Font.load();
        WorldGuard.load();
    }

    @Override
    public void onEnable() {
        Locale.init(this);
        updateConfig();
        WorldGuard.init(this);
        ConfigurationSection conf = getConfig();
        if (conf.getBoolean("autoupdate", true)) {
            startUpdater();
        }
        if (conf.getBoolean("localeInv", false)) {
            Events.useLocaleInv = true;
        }
        getServer().getPluginManager().registerEvents(listener = new Events(), this);
        getServer().getPluginCommand("rpgitemupdate").setExecutor(new RPGItemUpdateCommandHandler());
        ItemManager.load(this);
        Commands.register(new Handler());
        Commands.register(new PowerHandler());
        new PowerTicker().runTaskTimer(this, 0, 1);
    }

    public void startUpdater() {
        getLogger().info("The updater is currently under maintenance,");
        getServer().getConsoleSender().sendMessage("[RPGItems] Please check " + ChatColor.DARK_GRAY + ChatColor.ITALIC + ChatColor.BOLD + "www.github.com/NyaaCat/RPGitems-reloaded" + ChatColor.RESET + " for updates.");

        if (updater != null) {
        }
        //updater = new Updater(this, 70226, this.getFile(), Updater.UpdateType.DEFAULT, false);
    }

    public void updateConfig() {
        ConfigUpdater.updateConfig(getConfig());
        saveConfig();
    }

    @Override
    public void onDisable() {
        WorldGuard.unload();
        this.getServer().getScheduler().cancelAllTasks();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        StringBuilder out = new StringBuilder();
        out.append(label).append(' ');
        for (String arg : args)
            out.append(arg).append(' ');
        Commands.exec(sender, out.toString());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        StringBuilder out = new StringBuilder();
        out.append(alias).append(' ');
        for (String arg : args)
            out.append(arg).append(' ');
        return Commands.complete(sender, out.toString());
    }
}
