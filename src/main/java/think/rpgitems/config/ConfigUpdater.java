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
package think.rpgitems.config;

import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;

import think.rpgitems.Plugin;

public class ConfigUpdater {

    final static String CONFIG_VERSION = "0.5";

    static HashMap<String, Updater> updates;
    static {
        updates = new HashMap<String, Updater>();
        updates.put("0.1", new Update01To02());
        updates.put("0.2", new Update02To03());
        updates.put("0.3", new Update03To04());
        updates.put("0.4", new Update04To05());
    }

    public static void updateConfig(ConfigurationSection conf) {
        while (!conf.getString("version", "0.0").equals(CONFIG_VERSION)) {
            if (!conf.contains("version")) {
                if (!conf.contains("autoupdate")) {
                    conf.set("autoupdate", true);
                }
                if (!conf.contains("defaults.hand")) {
                    conf.set("defaults.hand", "One handed");
                }
                if (!conf.contains("defaults.sword")) {
                    conf.set("defaults.sword", "Sword");
                }
                if (!conf.contains("defaults.damage")) {
                    conf.set("defaults.damage", "Damage");
                }
                if (!conf.contains("defaults.armour")) {
                    conf.set("defaults.armour", "Armour");
                }
                if (!conf.contains("support.worldguard")) {
                    conf.set("support.worldguard", false);
                }
                conf.set("version", "0.1");
                Plugin.plugin.saveConfig();
            } else {
                if (updates.containsKey(conf.get("version"))) {
                    updates.get(conf.get("version")).update(conf);
                } else {
                    break;
                }
            }
        }
        updates.clear();
    }
  }

