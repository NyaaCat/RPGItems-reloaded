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

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.FileUtil;
import think.rpgitems.RPGItems;
import think.rpgitems.power.Power;

import java.io.*;
import java.util.HashMap;
import java.util.Map.Entry;

public class ItemManager {
    public static HashMap<Integer, RPGItem> itemById = new HashMap<>();
    public static HashMap<String, RPGItem> itemByName = new HashMap<>();
    public static HashMap<String, ItemGroup> groups = new HashMap<>();
    public static int currentPos = 0;
    private static RPGItems plugin;

    public static void reload() {
        itemById = new HashMap<>();
        itemByName = new HashMap<>();
        groups = new HashMap<>();
        currentPos = 0;
        load(plugin);
    }

    public static void load(RPGItems pl) {
        try {
            plugin = pl;
            RPGItem.plugin = pl;
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
            } catch (FileNotFoundException ignored) {
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
            ConfigurationSection section;
            try {
                currentPos = itemStorage.getInt("pos", 0);
                section = itemStorage.getConfigurationSection("items");
            } catch (NullPointerException e) {
                plugin.getLogger().severe("Error loading items.yml. Is this your first time to load RPGItems?");
                dump(e);
                return;
            }
            if (section == null)
                return;
            for (Object obj : section.getValues(false).values()) {
                ConfigurationSection sec = (ConfigurationSection) obj;
                RPGItem item = new RPGItem(sec);
                itemById.put(item.getID(), item);
                itemByName.put(item.getName(), item);
                for (Power power : item.powers) {
                    Power.powerUsage.add(power.getName());
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
            // Something went wrong
            e.printStackTrace();
            plugin.getLogger().severe("Error loading items.yml. Creating backup");
            dump(e);
        }
    }

    public static void dump(Exception e) {
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

    public static void save(RPGItems plugin) {

        YamlConfiguration itemStorage = new YamlConfiguration();

        itemStorage.set("items", null);
        itemStorage.set("pos", currentPos);
        ConfigurationSection newSection = itemStorage.createSection("items");
        for (RPGItem item : itemById.values()) {
            ConfigurationSection itemSection = newSection.getConfigurationSection(item.getName().replace(".", "_"));
            if (itemSection == null) {
                itemSection = newSection.createSection(item.getName().replace(".", "_"));
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
        if (item == null || item.getType() == Material.AIR)
            return null;
        if (!item.hasItemMeta())
            return null;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore() || meta.getLore().size() <= 0)
            return null;
        try {
            int id = ItemManager.decodeId(meta.getLore().get(0));
            if (id == -1) {
                return null;
            }
            return ItemManager.getItemById(id);
        } catch (Exception e) {
            return null;
        }
    }

    public static RPGItem newItem(String name) {
        if (itemByName.containsKey(name))
            return null;
        int free = 0;
        do {
            free = currentPos++;
        } while (itemById.containsKey(free));
        RPGItem item = new RPGItem(name, free);
        itemById.put(free, item);
        itemByName.put(name, item);
        return item;
    }

    public static RPGItem cloneItem(RPGItem item, String name) {
        if (itemByName.containsKey(name))
            return null;
        int free = 0;
        do {
            free = currentPos++;
        } while (itemById.containsKey(free));
        ConfigurationSection section = new MemoryConfiguration();
        item.save(section);
        RPGItem newItem = new RPGItem(section, name, free);
        itemById.put(free, newItem);
        itemByName.put(name, newItem);
        return newItem;
    }

    public static RPGItem getItemById(int id) {
        return itemById.get(id);
    }

    public static RPGItem getItemByName(String uid) {
        return itemByName.get(uid);
    }

    public static int decodeId(String str) throws NumberFormatException {
        if (str.length() < 16) {
            return -1;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (str.charAt(i) != ChatColor.COLOR_CHAR) {
                return -1;
            }
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
