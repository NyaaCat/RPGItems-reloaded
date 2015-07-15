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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import think.rpgitems.commands.Commands;
import think.rpgitems.config.ConfigUpdater;
import think.rpgitems.data.Font;
import think.rpgitems.data.Locale;
import think.rpgitems.item.ItemManager;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerArrow;
import think.rpgitems.power.PowerCommand;
import think.rpgitems.power.PowerConsume;
import think.rpgitems.power.PowerFireball;
import think.rpgitems.power.PowerFlame;
import think.rpgitems.power.PowerFood;
import think.rpgitems.power.PowerIce;
import think.rpgitems.power.PowerKnockup;
import think.rpgitems.power.PowerLifeSteal;
import think.rpgitems.power.PowerLightning;
import think.rpgitems.power.PowerPotionHit;
import think.rpgitems.power.PowerPotionSelf;
import think.rpgitems.power.PowerPotionTick;
import think.rpgitems.power.PowerRainbow;
import think.rpgitems.power.PowerRumble;
import think.rpgitems.power.PowerRush;
import think.rpgitems.power.PowerSkyHook;
import think.rpgitems.power.PowerTNTCannon;
import think.rpgitems.power.PowerTeleport;
import think.rpgitems.power.PowerTicker;
import think.rpgitems.power.PowerUnbreakable;
import think.rpgitems.power.PowerUnbreaking;
import think.rpgitems.support.WorldGuard;

@SuppressWarnings("deprecation")
public class Plugin extends JavaPlugin {

    public static Logger logger = Logger.getLogger("RPGItems");
	
    public static Plugin plugin;

    @Override
    public void onLoad() {
        plugin = this;
        reloadConfig();
        Font.load();
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
        Power.powers.put("rumble", PowerRumble.class);
        Power.powers.put("skyhook", PowerSkyHook.class);
        Power.powers.put("potiontick", PowerPotionTick.class);
        Power.powers.put("food", PowerFood.class);
        Power.powers.put("lifesteal", PowerLifeSteal.class);
    }

    @Override
    public void onEnable() {
        Locale.init(this);
        updateConfig();
        WorldGuard.init(this);
        ConfigurationSection conf = getConfig();
        if (conf.getBoolean("autoupdate", true)) {
            new Updater(this, 70226, this.getFile(), Updater.UpdateType.DEFAULT, false);
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

    @Override
    public void saveConfig() {
        FileConfiguration config = getConfig();
        FileOutputStream out = null;
        try {
            File f = new File(getDataFolder(), "config.yml");
            if (!f.exists())
                f.createNewFile();
            out = new FileOutputStream(f);
            out.write(config.saveToString().getBytes("UTF-8"));
        } catch (FileNotFoundException e) {
        } catch (UnsupportedEncodingException e) {
        } catch (IOException e) {
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static FileConfiguration config;

    @Override
    public void reloadConfig() {
        FileInputStream in = null;
        config = new YamlConfiguration();
        try {
            File f = new File(getDataFolder(), "config.yml");
            in = new FileInputStream(f);
            byte[] data = new byte[(int) f.length()];
            in.read(data);
            String str = new String(data, "UTF-8");
            config.loadFromString(str);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } catch (InvalidConfigurationException e) {
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void updateConfig() {
        ConfigUpdater.updateConfig(getConfig());
        saveConfig();
    }

    @Override
    public void onDisable() {
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
