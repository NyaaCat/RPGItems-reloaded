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
package think.rpgitems.item;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.FileUtil;

import think.rpgitems.Plugin;
import think.rpgitems.power.Power;

public class ItemManager {
    public static TIntObjectHashMap<RPGItem> itemById = new TIntObjectHashMap<RPGItem>();
    public static HashMap<String, RPGItem> itemByName = new HashMap<String, RPGItem>();
    public static HashMap<String, ItemGroup> groups = new HashMap<String, ItemGroup>();
    public static int currentPos = 0;

    public static void load(Plugin plugin) {
        try {
            FileInputStream in = null;
            YamlConfiguration itemStorage = null;
            try {
                File f = new File(plugin.getDataFolder(), "items.yml");
                in = new FileInputStream(f);
                byte[] data = new byte[(int) f.length()];
                in.read(data);
                itemStorage = new YamlConfiguration();
                String str = new String(data, "UTF-8");
                itemStorage.loadFromString(str);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null)
                        in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            currentPos = itemStorage.getInt("pos", 0);
            ConfigurationSection section = itemStorage.getConfigurationSection("items");
            if (section == null) return;
            for (String key : section.getKeys(false)) {
                RPGItem item = new RPGItem(section.getConfigurationSection(key));
                itemById.put(item.getID(), item);
                itemByName.put(item.getName(), item);
                for (Power power : item.powers) {
                    Power.powerUsage.put(power.getName(), Power.powerUsage.get(power.getName()) + 1);
                }
            }
            
            if (itemStorage.contains("groups")) {
                ConfigurationSection gSection = itemStorage.getConfigurationSection("groups");
                for (String key : gSection.getKeys(false)) {
                    ItemGroup group = new ItemGroup(gSection.getConfigurationSection(key));
                    groups.put(group.getName(), group);
                }
            }
        } catch (Exception e) {
            //Something went wrong
            plugin.getLogger().severe("Error loading items.yml. Creating backup");
            File file = new File(plugin.getDataFolder(), "items.yml");
            long time = System.currentTimeMillis();
            File backup = new File(plugin.getDataFolder(), time + "-items.yml");
            FileUtil.copy(file, backup);
            File log = new File(plugin.getDataFolder(), time + "-log.txt");
            PrintStream ps = null;
            try {
                ps = new PrintStream(log);
                ps.printf("RPGItems (%s) ItemManager.load\r\n", plugin.getDescription().getVersion());
                e.printStackTrace(ps);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } finally {
                ps.close();
            }
        }
    }

    public static void save(Plugin plugin) {

        YamlConfiguration itemStorage = new YamlConfiguration();

        itemStorage.set("items", null);
        itemStorage.set("pos", currentPos);
        ConfigurationSection newSection = itemStorage.createSection("items");
        for (RPGItem item : itemById.valueCollection()) {
            ConfigurationSection itemSection = newSection.getConfigurationSection(item.getName());
            if (itemSection == null) {
                itemSection = newSection.createSection(item.getName());
            }
            item.save(itemSection);
        }

        ConfigurationSection groupsSection = itemStorage.createSection("groups");
        
        for (Entry<String, ItemGroup> group : groups.entrySet()) {
            ConfigurationSection groupSection = groupsSection.createSection(group.getKey());
            group.getValue().save(groupSection);
        }
        
        FileOutputStream out = null;
        try {
            File f = new File(plugin.getDataFolder(), "items.yml");
            out = new FileOutputStream(f);
            out.write(itemStorage.saveToString().getBytes("UTF-8"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static RPGItem toRPGItem(ItemStack item) {
        if (item == null)
            return null;
        if (!item.hasItemMeta())
            return null;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName())
            return null;
        try {
            int id = ItemManager.decodeId(meta.getDisplayName());
            RPGItem rItem = ItemManager.getItemById(id);
            return rItem;
        } catch (Exception e) {
            return null;
        }
    }

    public static RPGItem newItem(String name) {
        if (itemByName.containsKey(name))
            return null;
        int free = 0;
        while (true) {
            free = currentPos++;
            if (!itemById.containsKey(free))
                break;
        }
        RPGItem item = new RPGItem(name, free);
        itemById.put(free, item);
        itemByName.put(name, item);
        return item;
    }

    public static RPGItem getItemById(int id) {
        return itemById.get(id);
    }

    public static RPGItem getItemByName(String uid) {
        return itemByName.get(uid);
    }

    public static int decodeId(String str) throws Exception {
        if (str.length() < 16) {
            throw new Exception();
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (str.charAt(i) != ChatColor.COLOR_CHAR)
                throw new Exception();
            i++;
            out.append(str.charAt(i));
        }
        return Integer.parseInt(out.toString(), 16);
    }

    public static void remove(RPGItem item) {
        itemByName.remove(item.getName());
        itemById.remove(item.getID());
    }

}
