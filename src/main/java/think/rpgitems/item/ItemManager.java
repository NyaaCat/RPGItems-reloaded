package think.rpgitems.item;

import cat.nyaa.nyaacore.Message;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.FileUtil;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.UnknownExtensionException;
import think.rpgitems.power.UnknownPowerException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import static think.rpgitems.item.RPGItem.updateItem;

public class ItemManager {
    public static HashMap<Integer, RPGItem> itemById = new HashMap<>();
    public static HashMap<String, RPGItem> itemByName = new HashMap<>();
    private static RPGItems plugin;

    public static void reload(RPGItems pl) {
        itemById = new HashMap<>();
        itemByName = new HashMap<>();
        load(pl);
    }

    public static void refreshItem() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : player.getInventory()) {
                RPGItem rpgItem = ItemManager.toRPGItem(item);
                if (rpgItem != null) {
                    updateItem(rpgItem, item);
                }
            }
            for (ItemStack item : player.getInventory().getArmorContents()) {
                RPGItem rpgItem = ItemManager.toRPGItem(item);
                if (rpgItem != null) {
                    updateItem(rpgItem, item);
                }
            }
        }
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

    @SuppressWarnings("deprecation")
    public static void addItem(RPGItem item) {
        if (item.getID() != 0) {
            itemById.put(item.getID(), item);
        }
        itemById.put(item.getUID(), item);
        itemByName.put(item.getName(), item);
    }

    private static void loadFromLegacyFile(File f) {
        try {
            FileInputStream in = null;
            YamlConfiguration itemStorage;
            try {
                in = new FileInputStream(f);
                byte[] data = new byte[(int) f.length()];
                int read = in.read(data);
                if (read < 0) throw new IllegalStateException();
                itemStorage = new YamlConfiguration();
                String str = new String(data, StandardCharsets.UTF_8);
                itemStorage.loadFromString(str);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
                return;
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
                try {
                    ConfigurationSection sec = (ConfigurationSection) obj;
                    RPGItem item = new RPGItem(sec);
                    addItem(item);
                    Message message = new Message("").append(I18n.format("message.update.success"), Collections.singletonMap("{item}", item.getComponent()));
                    // Bukkit.getOperators().forEach(message::send);
                    message.send(Bukkit.getConsoleSender());
                } catch (Exception e) {
                    e.printStackTrace();
                    Message message = new Message(I18n.format("message.update.fail", e.getLocalizedMessage()));
                    Bukkit.getOperators().forEach(message::send);
                    message.send(Bukkit.getConsoleSender());
                }
            }
        } catch (Exception e) {
            // Something went wrong
            e.printStackTrace();
            Message message = new Message(I18n.format("message.update.fail", e.getLocalizedMessage()));
            Bukkit.getOperators().forEach(message::send);
            plugin.getLogger().severe("Error loading items.yml. Creating backup");
            dump(e);
            throw new RuntimeException(e); //TODO
        }
    }

    private static void dump(Exception e) {
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
        File items = mkdir();
        File backup = mkbkdir();

        for (RPGItem item : itemByName.values()) {
            save(items, backup, item);
        }
    }

    public static void save(RPGItem item) {
        File items = mkdir();
        File backup = mkbkdir();
        save(items, backup, item);
    }

    private static File mkdir() {
        File items = new File(plugin.getDataFolder(), "items");
        if (!items.exists() || !items.isDirectory()) {
            if (!items.mkdir()) {
                throw new IllegalStateException();
            }
        }
        return items;
    }

    private static File mkbkdir() {
        File backup = new File(plugin.getDataFolder(), "backup");
        if (!backup.exists() || !backup.isDirectory()) {
            if (!backup.mkdir()) {
                throw new IllegalStateException();
            }
        }
        return backup;
    }

    private static void save(File items, File bkdir, RPGItem item) {
        String itemName = item.getName();
        String filename = getItemFilename(itemName);
        File itemFile = new File(items, filename + ".yml");
        try {
            File backup = new File(bkdir, filename + "." + System.currentTimeMillis() + ".bak");
            if (itemFile.exists()) {
                try {
                    if (!backup.createNewFile()) throw new IllegalStateException();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Cannot create backup for" + itemName + ".", e);
                }
                Files.move(itemFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            YamlConfiguration configuration = new YamlConfiguration();
            item.save(configuration);
            configuration.save(itemFile);

            try {
                String canonicalPath = itemFile.getCanonicalPath();
                YamlConfiguration test = new YamlConfiguration();
                test.load(canonicalPath);
                new RPGItem(test);
                if (backup.exists()) {
                    backup.deleteOnExit();
                }
                item.file = canonicalPath;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error verifying integrity for " + itemName + ".", e);
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static RPGItem toRPGItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return null;
        if (!item.hasItemMeta())
            return null;
        ItemMeta meta = item.getItemMeta();
        return toRPGItem(meta);
    }

    public static RPGItem toRPGItem(ItemMeta meta) {
        if (!meta.hasLore() || meta.getLore().size() <= 0)
            return null;
        try {
            int id = RPGItem.decodeId(meta.getLore().get(0));
            return ItemManager.getItemById(id);
        } catch (Exception e) {
            return null;
        }
    }

    public static RPGItem newItem(String name, CommandSender sender) {
        if (itemByName.containsKey(name))
            return null;
        int free = nextUid();
        RPGItem item = new RPGItem(name, free, sender);
        itemById.put(free, item);
        itemByName.put(name, item);
        return item;
    }

    static int nextUid() {
        int free;
        do {
            free = ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, 0);
        } while (itemById.containsKey(free));
        return free;
    }

    public static RPGItem cloneItem(RPGItem item, String name) {
        if (itemByName.containsKey(name))
            return null;
        int free = nextUid();
        ConfigurationSection section = new MemoryConfiguration();
        item.save(section);
        RPGItem newItem;
        try {
            newItem = new RPGItem(section, name, free);
        } catch (UnknownPowerException | UnknownExtensionException e) {
            throw new RuntimeException(e);
        }
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

    @SuppressWarnings("deprecation")
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

    public static String getItemFilename(String itemName) {
        // ensure Windows servers won't be blown up by CONs or NULs
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
