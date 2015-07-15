package think.rpgitems.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.Quality;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;

public class Update02To03 implements Updater {

    @SuppressWarnings("unchecked")
    @Override
    public void update(ConfigurationSection section) {
        Plugin plugin = Plugin.plugin;
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
                e.printStackTrace();
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
            int currentPos = itemStorage.getInt("pos", 0);
            ConfigurationSection itemSection = itemStorage.getConfigurationSection("items");
            if (itemSection != null) {
                for (String itemKey : itemSection.getKeys(false)) {
                    ConfigurationSection s = itemSection.getConfigurationSection(itemKey);
                    String name = s.getString("name");
                    int id = s.getInt("id");
                    String displayName = null, type = null, hand = null, lore = null;
                    try {
                        if (s.contains("display")) {
                            displayName = s.getString("display");
                        } else {
                            displayName = new String(byte[].class.cast(s.get("display_bin", "")), "UTF-8");
                        }
                        if (s.contains("type")) {
                            type = s.getString("type", Plugin.plugin.getConfig().getString("defaults.sword", "Sword"));
                        } else {
                            if (s.contains("type_bin")) {
                                type = new String(byte[].class.cast(s.get("type_bin", "")), "UTF-8");
                            } else {
                                type = Plugin.plugin.getConfig().getString("defaults.sword", "Sword");
                            }
                        }
                        if (s.contains("hand")) {
                            hand = s.getString("hand", Plugin.plugin.getConfig().getString("defaults.hand", "One handed"));
                        } else {
                            if (s.contains("hand_bin")) {
                                hand = new String(byte[].class.cast(s.get("hand_bin", "")), "UTF-8");
                            } else {
                                hand = Plugin.plugin.getConfig().getString("defaults.hand", "One handed");
                            }
                        }
                        if (s.contains("lore")) {
                            lore = s.getString("lore");
                        } else {
                            if (s.contains("lore_bin")) {
                                lore = new String(byte[].class.cast(s.get("lore_bin", "")), "UTF-8");
                            } else {
                                lore = "";
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    List<String> description = (List<String>) s.getList("description", new ArrayList<String>());
                    for (int i = 0; i < description.size(); i++) {
                        description.set(i, ChatColor.translateAlternateColorCodes('&', description.get(i)));
                    }
                    Quality quality = Quality.valueOf(s.getString("quality"));
                    int damageMin = s.getInt("damageMin");
                    int damageMax = s.getInt("damageMax");
                    int armour = s.getInt("armour", 0);
                    ItemStack item = new ItemStack(Material.valueOf(s.getString("item")));
                    ItemMeta meta = item.getItemMeta();
                    if (meta instanceof LeatherArmorMeta) {
                        ((LeatherArmorMeta) meta).setColor(Color.fromRGB(s.getInt("item_colour", 0)));
                    } else {
                        item.setDurability((short) s.getInt("item_data", 0));
                    }
                    boolean ignoreWorldGuard = s.getBoolean("ignoreWorldGuard", false);

                    ConfigurationSection powerSection = s.getConfigurationSection("powers");
                    ArrayList<Power> powers = new ArrayList<Power>();
                    if (powerSection != null) {
                        for (String key : powerSection.getKeys(false)) {
                            try {
                                if (!Power.powers.containsKey(key)) {
                                    // Invalid power
                                    continue;
                                }
                                Power pow = Power.powers.get(key).newInstance();
                                pow.init(powerSection.getConfigurationSection(key));
                                // pow.item = this;
                                powers.add(pow);
                            } catch (InstantiationException e) {
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    RPGItem newItem = new RPGItem(name, id);
                    newItem.setDisplay(displayName, false);
                    newItem.setType(type, false);
                    newItem.setHand(hand, false);
                    newItem.setLore(lore, false);
                    newItem.setItem(item.getType());
                    for (String locales : Locale.getLocales()) {
                        newItem.setLocaleMeta(locales, meta);
                    }
                    newItem.setItem(item.getType(), false);
                    newItem.setDataValue(item.getDurability(), false);
                    newItem.setArmour(armour, false);
                    newItem.setDamage(damageMin, damageMax);
                    newItem.setQuality(quality, false);
                    newItem.ignoreWorldGuard = ignoreWorldGuard;
                    newItem.description = description;

                    for (Power power : powers) {
                        newItem.addPower(power, false);
                    }
                    ItemManager.itemById.put(newItem.getID(), newItem);
                    ItemManager.itemByName.put(newItem.getName(), newItem);
                }
            }
            ItemManager.currentPos = currentPos;

            ItemManager.save(plugin);
            ItemManager.itemByName.clear();
            ItemManager.itemById.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        section.set("version", "0.3");
    }

}
