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
import think.rpgitems.config.ConfigUpdater;
import think.rpgitems.data.Font;
import think.rpgitems.data.Locale;
import think.rpgitems.item.ItemManager;
import think.rpgitems.power.*;
import think.rpgitems.support.WorldGuard;

import java.util.List;
import java.util.logging.Logger;

@SuppressWarnings("deprecation")
public class Plugin extends JavaPlugin {

    public static Logger logger = Logger.getLogger("RPGItems");
    public static Plugin plugin;
    public static Updater updater;

    @Override
    public void onLoad() {
        plugin = this;
        saveDefaultConfig();
        Font.load();
        Power.powers.put("aoe", PowerAOE.class);
        Power.powers.put("arrow", PowerArrow.class);
        Power.powers.put("tntcannon", PowerTNTCannon.class);
        Power.powers.put("rainbow", PowerRainbow.class);
        Power.powers.put("flame", PowerFlame.class);
        Power.powers.put("lightning", PowerLightning.class);
        Power.powers.put("command", PowerCommand.class);
        Power.powers.put("potionhit", PowerPotionHit.class);
        Power.powers.put("teleport", PowerTeleport.class);
        Power.powers.put("fireball", PowerFireball.class);
        Power.powers.put("ice", PowerIce.class);
        Power.powers.put("knockup", PowerKnockup.class);
        Power.powers.put("rush", PowerRush.class);
        Power.powers.put("potionself", PowerPotionSelf.class);
        Power.powers.put("consume", PowerConsume.class);
        Power.powers.put("unbreakable", PowerUnbreakable.class);
        Power.powers.put("unbreaking", PowerUnbreaking.class);
        Power.powers.put("rescue", PowerRescue.class);
        Power.powers.put("rumble", PowerRumble.class);
        Power.powers.put("skyhook", PowerSkyHook.class);
        Power.powers.put("potiontick", PowerPotionTick.class);
        Power.powers.put("food", PowerFood.class);
        Power.powers.put("lifesteal", PowerLifeSteal.class);
        Power.powers.put("torch", PowerTorch.class);
        Power.powers.put("fire", PowerFire.class);
        Power.powers.put("projectile", PowerProjectile.class);
        Power.powers.put("deathcommand", PowerDeathCommand.class);
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
        getServer().getPluginManager().registerEvents(new Events(), this);
        ItemManager.load(this);
        Commands.register(new Handler());
        Commands.register(new PowerHandler());
        new PowerTicker().runTaskTimer(this, 0, 1);
    }

    public void startUpdater() {
        getLogger().info("The updater is currently under maintenance,");
        getServer().getConsoleSender().sendMessage("[RPGItems] Please check " + ChatColor.DARK_GRAY + ChatColor.ITALIC + ChatColor.BOLD + "www.github.com/NyaaCat/RPGitems-reloaded" + ChatColor.RESET + " for updates.");

        if (updater != null)
            return;
        //updater = new Updater(this, 70226, this.getFile(), Updater.UpdateType.DEFAULT, false);
    }

    public void updateConfig() {
        ConfigUpdater.updateConfig(getConfig());
        saveConfig();
    }

    @Override
    public void onDisable() {
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
