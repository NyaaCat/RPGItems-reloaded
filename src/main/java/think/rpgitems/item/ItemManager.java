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

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.FileUtil;
import think.rpgitems.RPGItems;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class ItemManager {
    public static HashMap<Integer, RPGItem> itemById = new HashMap<>();
    public static HashMap<String, RPGItem> itemByName = new HashMap<>();
    private static RPGItems plugin;

    public static void reload(RPGItems pl) {
        itemById = new HashMap<>();
        itemByName = new HashMap<>();
        load(pl);
    }

    public static void load(RPGItems pl) {
        plugin = pl;
        RPGItem.plugin = pl;
        File items = new File(plugin.getDataFolder(), "items");
        if (!items.exists() || !items.isDirectory()) {
            File f = new File(plugin.getDataFolder(), "items.yml");
            if (!f.exists()) {
                return;
            }
            plugin.getLogger().warning("loading items from legacy items.yml");
            loadFromLegacyFile(f);
            plugin.getLogger().warning("moving items to directory based storage");
            save();
            Path bak = f.toPath().resolveSibling("items.bak");
            try {
                Files.move(f.toPath(), bak);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        File[] files = items.listFiles((d, n) -> n.endsWith("yml"));
        for (File file : Objects.requireNonNull(files)) {
            load(file);
        }
    }

    private static void load(File file) {
        try {
            YamlConfiguration itemStorage = new YamlConfiguration();
            itemStorage.load(file);
            RPGItem item = new RPGItem(itemStorage);
            item.file = file.getCanonicalPath();
            addItem(item);
        } catch (Exception e) {
            // Something went wrong
            e.printStackTrace();
            plugin.getLogger().severe("Error loading " + file + ".");
        }
    }

    public static void addItem(RPGItem item) {
        if (item.getID() != 0) {
            itemById.put(item.getID(), item);
        }
        itemById.put(item.getUID(), item);
        itemByName.put(item.getName(), item);
    }

    public static void loadFromLegacyFile(File f) {
        try {
            FileInputStream in = null;
            YamlConfiguration itemStorage = null;
            try {
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
                addItem(item);
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
        try (PrintStream ps = new PrintStream(log)) {
            ps.printf("RPGItems (%s) ItemManager.loadFromLegacyFile\r\n", plugin.getDescription().getVersion());
            e.printStackTrace(ps);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
    }

    public static void save() {
        File items = new File(plugin.getDataFolder(), "items");
        if (!items.exists() || !items.isDirectory()) {
            if (!items.mkdir()) {
                throw new IllegalStateException();
            }
        }

        for (RPGItem item : itemByName.values()) {
            save(items, item);
        }
    }

    private static void save(File items, RPGItem item) {
        String itemName = item.getName();
        String filename = getItemFilename(itemName);
        File itemFile = new File(items, filename + ".yml");
        File backup = new File(items, filename + "." + System.currentTimeMillis() + ".bak");
        try {
            if (!backup.createNewFile()) throw new IllegalStateException();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Cannot create backup for" + itemName + ".", e);
        }
        try {
            if (itemFile.exists()) {
                Files.move(itemFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            YamlConfiguration configuration = new YamlConfiguration();
            item.save(configuration);
            configuration.save(itemFile);

            String canonicalPath = itemFile.getCanonicalPath();
            YamlConfiguration test = new YamlConfiguration();
            test.load(canonicalPath);
            new RPGItem(test);
            backup.deleteOnExit();
            item.hashcode = Math.abs(configuration.saveToString().hashCode());
            item.file = canonicalPath;
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
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
            int id = RPGItem.decodeId(meta.getLore().get(0));
            return ItemManager.getItemById(id);
        } catch (Exception e) {
            return null;
        }
    }

    public static RPGItem newItem(String name) {
        if (itemByName.containsKey(name))
            return null;
        int free = nextUid();
        RPGItem item = new RPGItem(name, free);
        itemById.put(free, item);
        itemByName.put(name, item);
        return item;
    }

    public static int nextUid() {
        int free;
        do {
            free = -ThreadLocalRandom.current().nextInt();
        } while (itemById.containsKey(free));
        return free;
    }

    public static RPGItem cloneItem(RPGItem item, String name) {
        if (itemByName.containsKey(name))
            return null;
        int free;
        do {
            free = ThreadLocalRandom.current().nextInt(65536, Integer.MAX_VALUE) + 1;
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

    public static RPGItem getItemByName(String name) {
        return itemByName.get(name);
    }

    public static void remove(RPGItem item) {
        itemByName.remove(item.getName());
        itemById.remove(item.getID());
        itemById.remove(item.getUID());
        File file = new File(item.file);
        Path path = file.toPath();
        Path bak = path.resolveSibling(getItemFilename(item.getName()) + ".bak");
        try {
            File bakFile = Files.move(path, bak, StandardCopyOption.REPLACE_EXISTING).toFile();
            bakFile.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getItemFilename(String itemName) {
        // ensure Windows server don't blow up by CONs or NULs
        // and escape some character that don't fit into a file name
        return "item-" +
                       itemName
                               .replace("_", "__")
                               .replace("/", "_f")
                               .replace("\\", "_b")
                               .replace("*", "_a")
                               .replace("\"", "_o")
                               .replace("\'", "_i")
                               .replace("?", "_q")
                               .replace("<", "_l")
                               .replace(">", "_g")
                               .replace("|", "_p")
                               .replace(":", "_c")
                               .replace(".", "_d");
    }
}
