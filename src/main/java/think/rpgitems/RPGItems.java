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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.data.Font;
import think.rpgitems.item.ItemManager;
import think.rpgitems.power.PowerManager;
import think.rpgitems.power.PowerTicker;
import think.rpgitems.power.impl.BasePower;
import think.rpgitems.support.WGSupport;

import java.util.logging.Logger;

public class RPGItems extends JavaPlugin {

    public static Logger logger = Logger.getLogger("RPGItems");
    public static RPGItems plugin;
    public static Events listener;
    public Handler commandHandler;
    public I18n i18n;

    @Override
    public void onLoad() {
        plugin = this;
        PowerManager.registerPowers(RPGItems.plugin, BasePower.class.getPackage().getName());
        PowerManager.addDescriptionResolver(RPGItems.plugin, (power, property) -> {
            if (property == null) {
                @LangKey(skipCheck = true) String powerKey = "power.properties." + power.getKey() + ".main_description";
                return I18n.format(powerKey);
            }
            @LangKey(skipCheck = true) String key = "power.properties." + power.getKey() + "." + property;
            if (I18n.getInstance().hasKey(key)) {
                return I18n.format(key);
            }
            @LangKey(skipCheck = true) String baseKey = "power.properties.base." + property;
            if (I18n.getInstance().hasKey(baseKey)) {
                return I18n.format(baseKey);
            }
            return null;
        });
        saveDefaultConfig();
        Font.load();
        WGSupport.load();
    }

    @Override
    public void onEnable() {
        plugin = this;
        if (Bukkit.class.getPackage().getImplementationVersion().startsWith("git-Bukkit-")) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "======================================");
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "RPGItems plugin require Spigot API, Please make sure you are using Spigot.");
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "======================================");
        }
        try {
            Bukkit.spigot();
        } catch (NoSuchMethodError e) {
            getCommand("rpgitem").setExecutor((sender, command, label, args) -> {
                sender.sendMessage(ChatColor.RED + "======================================");
                sender.sendMessage(ChatColor.RED + "RPGItems plugin require Spigot API, Please make sure you are using Spigot.");
                sender.sendMessage(ChatColor.RED + "======================================");
                return true;
            });
        }
        WGSupport.init(this);
        ConfigurationSection conf = getConfig();
        if (conf.getBoolean("localeInv", false)) {
            Events.useLocaleInv = true;
        }
        i18n = new I18n(this, conf.getString("language"));
        commandHandler = new Handler(this, i18n);
        getCommand("rpgitem").setExecutor(commandHandler);
        getCommand("rpgitem").setTabCompleter(commandHandler);
        Bukkit.getScheduler().runTask(plugin, () -> {
            getServer().getPluginManager().registerEvents(listener = new Events(), this);
            ItemManager.load(this);
            new PowerTicker().runTaskTimer(this, 0, 1);
        });
    }

    @Override
    public void onDisable() {
        WGSupport.unload();
        getCommand("rpgitem").setExecutor(null);
        getCommand("rpgitem").setTabCompleter(null);
        this.getServer().getScheduler().cancelTasks(plugin);
    }
}
