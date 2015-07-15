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

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import think.rpgitems.Plugin;

public class Update01To02 implements Updater {

    @Override
    public void update(ConfigurationSection section) {

        File iFile = new File(Plugin.plugin.getDataFolder(), "items.yml");
        YamlConfiguration itemStorage = YamlConfiguration.loadConfiguration(iFile);
        ConfigurationSection iSection = itemStorage.getConfigurationSection("items");

        if (iSection != null) {
            for (String key : iSection.getKeys(false)) {
                ConfigurationSection item = iSection.getConfigurationSection(key);
                if (item.contains("armour")) {
                    int dam = item.getInt("armour");
                    item.set("armour", (int) ((((double) dam) / 20d) * 100d));
                }
            }
        }

        section.set("version", "0.2");

        try {
            itemStorage.save(iFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Plugin.plugin.saveConfig();
    }

}
