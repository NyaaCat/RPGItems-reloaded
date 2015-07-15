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

import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import think.rpgitems.Events;
import think.rpgitems.Plugin;
import think.rpgitems.data.Font;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGMetadata;
import think.rpgitems.power.Power;
import think.rpgitems.power.types.PowerHit;
import think.rpgitems.power.types.PowerLeftClick;
import think.rpgitems.power.types.PowerProjectileHit;
import think.rpgitems.power.types.PowerRightClick;
import think.rpgitems.power.types.PowerTick;

public class RPGItem {
    private ItemStack item;

    private HashMap<String, ItemMeta> localeMeta = new HashMap<String, ItemMeta>();

    private int id;
    private String name;
    private String encodedID;
    
    private boolean haspermission;
    private String permission;
    private String displayName;
    private Quality quality = Quality.TRASH;
    private int damageMin = 0, damageMax = 3;
    private int armour = 0;
    private String loreText = "";
    private String type = Plugin.plugin.getConfig().getString("defaults.sword", "Sword");
    private String hand = Plugin.plugin.getConfig().getString("defaults.hand", "One handed");
    public boolean ignoreWorldGuard = false;
    public List<String> description = new ArrayList<String>();
    
    // Powers
    public ArrayList<Power> powers = new ArrayList<Power>();
    private ArrayList<PowerLeftClick> powerLeftClick = new ArrayList<PowerLeftClick>();
    private ArrayList<PowerRightClick> powerRightClick = new ArrayList<PowerRightClick>();
    private ArrayList<PowerProjectileHit> powerProjectileHit = new ArrayList<PowerProjectileHit>();
    private ArrayList<PowerHit> powerHit = new ArrayList<PowerHit>();
    private ArrayList<PowerTick> powerTick = new ArrayList<PowerTick>();

    // Recipes
    public int recipechance = 6;
    public boolean hasRecipe = false;
    public List<ItemStack> recipe = null;

    // Drops
    public TObjectDoubleHashMap<String> dropChances = new TObjectDoubleHashMap<String>();

    private int tooltipWidth = 150;

    // Durability
    private int maxDurability;
    private boolean hasBar = false;
    private boolean forceBar = false;

    public RPGItem(String name, int id) {
        this.name = name;
        this.id = id;
        encodedID = getMCEncodedID(id);
        item = new ItemStack(Material.WOOD_SWORD);

        displayName = item.getType().toString();
        
        localeMeta.put("en_GB", item.getItemMeta());
        
        rebuild();
    }

    @SuppressWarnings("unchecked")
    public RPGItem(ConfigurationSection s) {

        name = s.getString("name");
        id = s.getInt("id");
        setDisplay(s.getString("display"), false);
        setType(s.getString("type", Plugin.plugin.getConfig().getString("defaults.sword", "Sword")), false);
        setHand(s.getString("hand", Plugin.plugin.getConfig().getString("defaults.hand", "One handed")), false);
        setLore(s.getString("lore"), false);
        description = (List<String>) s.getList("description", new ArrayList<String>());
        for (int i = 0; i < description.size(); i++) {
            description.set(i, ChatColor.translateAlternateColorCodes('&', description.get(i)));
        }
        quality = Quality.valueOf(s.getString("quality"));
        damageMin = s.getInt("damageMin");
        damageMax = s.getInt("damageMax");
        armour = s.getInt("armour", 0);
        item = new ItemStack(Material.valueOf(s.getString("item")));
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(Color.fromRGB(s.getInt("item_colour", 0)));
        } else {
            item.setDurability((short) s.getInt("item_data", 0));
        }
        for (String locale : Locale.getLocales()) {
            localeMeta.put(locale, meta.clone());
        }
        ignoreWorldGuard = s.getBoolean("ignoreWorldGuard", false);

        // Powers
        ConfigurationSection powerList = s.getConfigurationSection("powers");
        if (powerList != null) {
            for (String sectionKey : powerList.getKeys(false)) {
                ConfigurationSection section = powerList.getConfigurationSection(sectionKey);
                try {
                    if (!Power.powers.containsKey(section.getString("powerName"))) {
                        // Invalid power
                        continue;
                    }
                    Power pow = Power.powers.get(section.getString("powerName")).newInstance();
                    pow.init(section);
                    pow.item = this;
                    addPower(pow, false);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
 
        
        encodedID = getMCEncodedID(id);
        haspermission = s.getBoolean("haspermission", false);
        permission = s.getString("permission", "a.default.permission");
        // Recipes
        recipechance = s.getInt("recipechance", 6);
        hasRecipe = s.getBoolean("hasRecipe", false);
        if (hasRecipe) {
            recipe = (List<ItemStack>) s.getList("recipe");
        }

        ConfigurationSection drops = s.getConfigurationSection("dropChances");
        if (drops != null) {
            for (String key : drops.getKeys(false)) {
                double chance = drops.getDouble(key, 0.0);
                chance = Math.min(chance, 100.0);
                if (chance > 0) {
                    dropChances.put(key, chance);
                    if (!Events.drops.containsKey(key)) {
                        Events.drops.put(key, new HashSet<Integer>());
                    }
                    Set<Integer> set = Events.drops.get(key);
                    set.add(getID());
                } else {
                    dropChances.remove(key);
                    if (Events.drops.containsKey(key)) {
                        Set<Integer> set = Events.drops.get(key);
                        set.remove(getID());
                    }
                }
                dropChances.put(key, chance);
            }
        }
        if (item.getType().getMaxDurability() != 0) {
            hasBar = true;
        }
        maxDurability = s.getInt("maxDurability", item.getType().getMaxDurability());
        forceBar = s.getBoolean("forceBar", false);

        if (maxDurability == 0) {
            maxDurability = -1;
        }
        
        rebuild();
        }
        

    public void save(ConfigurationSection s) {
        s.set("name", name);
        s.set("id", id);
        s.set("haspermission", haspermission);
        s.set("permission", permission);
        s.set("display", displayName.replaceAll("" + ChatColor.COLOR_CHAR, "&"));
        s.set("quality", quality.toString());
        s.set("damageMin", damageMin);
        s.set("damageMax", damageMax);
        s.set("armour", armour);
        s.set("type", type.replaceAll("" + ChatColor.COLOR_CHAR, "&"));
        s.set("hand", hand.replaceAll("" + ChatColor.COLOR_CHAR, "&"));
        s.set("lore", loreText.replaceAll("" + ChatColor.COLOR_CHAR, "&"));
        ArrayList<String> descriptionConv = new ArrayList<String>(description);
        for (int i = 0; i < descriptionConv.size(); i++) {
            descriptionConv.set(i, descriptionConv.get(i).replaceAll("" + ChatColor.COLOR_CHAR, "&"));
        }
        s.set("description", descriptionConv);
        s.set("item", item.getType().toString());
        s.set("ignoreWorldGuard", ignoreWorldGuard);

        ItemMeta meta = localeMeta.get("en_GB");
        if (meta instanceof LeatherArmorMeta) {
            s.set("item_colour", ((LeatherArmorMeta) meta).getColor().asRGB());
        } else {
            s.set("item_data", item.getDurability());
        }
        ConfigurationSection powerConfigs = s.createSection("powers");
        int i = 0;
        for (Power p : powers) {
            MemoryConfiguration pConfig = new MemoryConfiguration();
            pConfig.set("powerName", p.getName());
            p.save(pConfig);
            powerConfigs.set(Integer.toString(i), pConfig);
            i++;
        }

        // Recipes
        s.set("recipechance", recipechance);
        s.set("hasRecipe", hasRecipe);
        if (hasRecipe) {
            s.set("recipe", recipe);
        }

        ConfigurationSection drops = s.createSection("dropChances");
        for (String key : dropChances.keySet()) {
            drops.set(key, dropChances.get(key));
        }

        s.set("maxDurability", maxDurability);
        s.set("forceBar", forceBar);
    }

    public void resetRecipe(boolean removeOld) {
        if (removeOld) {
            Iterator<Recipe> it = Bukkit.recipeIterator();
            while (it.hasNext()) {
                Recipe recipe = it.next();
                RPGItem rpgitem = ItemManager.toRPGItem(recipe.getResult());
                if (rpgitem == null)
                    continue;
                if (rpgitem.getID() == getID()) {
                    it.remove();
                }
            }
        }
        if (hasRecipe) {
            Set<ItemStack> iSet = new HashSet<ItemStack>();
            for (ItemStack m : recipe) {
                iSet.add(m);
            }
            ItemStack[] iList = iSet.toArray(new ItemStack[iSet.size()]);
            item.setItemMeta(getLocaleMeta("en_GB"));
            ShapedRecipe shapedRecipe = new ShapedRecipe(item);
            int i = 0;
            Map<ItemStack, Character> iMap = new HashMap<ItemStack, Character>();
            for (ItemStack m : iList) {
                iMap.put(m, (char) (65 + i));
                i++;
            }
            iMap.put(null, ' ');
            StringBuilder out = new StringBuilder();
            for (ItemStack m : recipe) {
                out.append(iMap.get(m));
            }
            String shape = out.toString();
            shapedRecipe.shape(new String[] { shape.substring(0, 3), shape.substring(3, 6), shape.substring(6, 9) });
            for (Entry<ItemStack, Character> e : iMap.entrySet()) {
                if (e.getKey() != null) {
                    shapedRecipe.setIngredient(e.getValue(), e.getKey().getType(), e.getKey().getDurability());
                }
            }
            Bukkit.addRecipe(shapedRecipe);
        }
    }

    public void leftClick(Player player) {
        for (PowerLeftClick power : powerLeftClick) {
            power.leftClick(player);
        }
    }

    public void rightClick(Player player) {
        for (PowerRightClick power : powerRightClick) {
            power.rightClick(player);
        }
    }

    public void projectileHit(Player player, Projectile arrow) {
        for (PowerProjectileHit power : powerProjectileHit) {
            power.projectileHit(player, arrow);
        }
    }

    public void hit(Player player, LivingEntity e, double d) {
        for (PowerHit power : powerHit) {
            power.hit(player, e, d);
        }
    }

    public void tick(Player player) {
        for (PowerTick power : powerTick) {
            power.tick(player);
        }
    }

    public void rebuild() {
        for (String locale : Locale.getLocales()) {
            if (!localeMeta.containsKey(locale))
                localeMeta.put(locale, getLocaleMeta("en_GB"));
        }
        for (String locale : Locale.getLocales()) {
            List<String> lines = getTooltipLines(locale);
            ItemMeta meta = getLocaleMeta(locale);
            meta.setDisplayName(lines.get(0));
            lines.remove(0);
            meta.setLore(lines);
            setLocaleMeta(locale, meta);
            // item.setItemMeta(meta);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Iterator<ItemStack> it = player.getInventory().iterator();
            String locale = Locale.getPlayerLocale(player);
            while (it.hasNext()) {
                ItemStack item = it.next();
                if (ItemManager.toRPGItem(item) != null)
                    updateItem(item, locale, false);
            }
            for (ItemStack item : player.getInventory().getArmorContents()) {
                if (ItemManager.toRPGItem(item) != null)
                    updateItem(item, locale, false);

            }
        }
        resetRecipe(true);
    }

    public static RPGMetadata getMetadata(ItemStack item) {
        //Check for broken item
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore() || item.getItemMeta().getLore().size() == 0) {
            //Broken item
            return new RPGMetadata();
        }
        return RPGMetadata.parseLoreline(item.getItemMeta().getLore().get(0));
    }

    public static void updateItem(ItemStack item, String locale) {
        updateItem(item, locale, getMetadata(item));
    }

    public static void updateItem(ItemStack item, String locale, RPGMetadata rpgMeta) {
        updateItem(item, locale, rpgMeta, false);
    }

    public static void updateItem(ItemStack item, String locale, boolean updateDurability) {
        updateItem(item, locale, getMetadata(item), false);
    }

    public static void updateItem(ItemStack item, String locale, RPGMetadata rpgMeta, boolean updateDurability) {
        RPGItem rItem = ItemManager.toRPGItem(item);
        if (rItem == null)
            return;
        item.setType(rItem.item.getType());
        ItemMeta meta = rItem.getLocaleMeta(locale);
        if (!(meta instanceof LeatherArmorMeta) && updateDurability) {
            item.setDurability(rItem.item.getDurability());
        }
        List<String> lore = meta.getLore();
        rItem.addExtra(rpgMeta, item, lore);
        lore.set(0, meta.getLore().get(0) + rpgMeta.toMCString());
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public void addExtra(RPGMetadata rpgMeta, ItemStack item, List<String> lore) {
        if (maxDurability > 0) {
            if (!rpgMeta.containsKey(RPGMetadata.DURABILITY)) {
                rpgMeta.put(RPGMetadata.DURABILITY, Integer.valueOf(maxDurability));
            }
            int durability = ((Number) rpgMeta.get(RPGMetadata.DURABILITY)).intValue();

            if (!hasBar || forceBar) {
                StringBuilder out = new StringBuilder();
                char boxChar = '\u25A0';
                int boxCount = tooltipWidth / 4;
                int mid = (int) ((double) boxCount * ((double) durability / (double) maxDurability));
                for (int i = 0; i < boxCount; i++) {
                    out.append(i < mid ? ChatColor.GREEN : i == mid ? ChatColor.YELLOW : ChatColor.RED);
                    out.append(boxChar);
                }
                lore.add(out.toString());
            }
            if (hasBar) {
                item.setDurability((short) (item.getType().getMaxDurability() - ((short) ((double) item.getType().getMaxDurability() * ((double) durability / (double) maxDurability)))));
            }
        } else if (maxDurability <= 0) {
            item.setDurability(hasBar ? (short)0 : this.item.getDurability());
        }
    }

    public List<String> getTooltipLines(String locale) {
        ArrayList<String> output = new ArrayList<String>();
        int width = 150;
        output.add(encodedID + quality.colour + ChatColor.BOLD + displayName);
        int dWidth = getStringWidthBold(ChatColor.stripColor(displayName));
        if (dWidth > width)
            width = dWidth;

        dWidth = getStringWidth(ChatColor.stripColor(hand + "     " + type));
        if (dWidth > width)
            width = dWidth;
        String damageStr = null;
        if (damageMin == 0 && damageMax == 0 && armour != 0) {
            damageStr = armour + "% " + Plugin.plugin.getConfig().getString("defaults.armour", "Armour");
        } else if (armour == 0 && damageMin == 0 && damageMax == 0) {
            damageStr = null;
        } else if (damageMin == damageMax) {
            damageStr = damageMin + " " + Plugin.plugin.getConfig().getString("defaults.damage", "Damage");
        } else {
            damageStr = damageMin + "-" + damageMax + " " + Plugin.plugin.getConfig().getString("defaults.damage", "Damage");
        }
        if (damageMin != 0 || damageMax != 0 || armour != 0) {
            dWidth = getStringWidth(damageStr);
            if (dWidth > width)
                width = dWidth;
        }

        for (Power p : powers) {
            dWidth = getStringWidth(ChatColor.stripColor(p.displayText(locale)));
            if (dWidth > width)
                width = dWidth;
        }

        for (String s : description) {
            dWidth = getStringWidth(ChatColor.stripColor(s));
            if (dWidth > width)
                width = dWidth;
        }

        tooltipWidth = width;

        output.add(ChatColor.WHITE + hand + StringUtils.repeat(" ", (width - getStringWidth(ChatColor.stripColor(hand + type))) / 4) + type);
        if (damageStr != null) {
            output.add(ChatColor.WHITE + damageStr);
        }

        for (Power p : powers) {
            output.add(p.displayText(locale));
        }
        if (loreText.length() != 0) {
            int cWidth = 0;
            int tWidth = 0;
            StringBuilder out = new StringBuilder();
            StringBuilder temp = new StringBuilder();
            out.append(ChatColor.YELLOW);
            out.append(ChatColor.ITALIC);
            String currentColour = ChatColor.YELLOW.toString();
            String dMsg = "\"" + loreText + "\"";
            for (int i = 0; i < dMsg.length(); i++) {
                char c = dMsg.charAt(i);
                temp.append(c);
                if (c == ChatColor.COLOR_CHAR || c == '&') {
                    i += 1;
                    temp.append(dMsg.charAt(i));
                    currentColour = ChatColor.COLOR_CHAR + "" + dMsg.charAt(i);
                    continue;
                }
                if (c == ' ')
                    tWidth += 4;
                else
                    tWidth += Font.widths[c] + 1;
                if (c == ' ' || i == dMsg.length() - 1) {
                    if (cWidth + tWidth > width) {
                        cWidth = 0;
                        cWidth += tWidth;
                        tWidth = 0;
                        output.add(out.toString());
                        out = new StringBuilder();
                        out.append(currentColour);
                        out.append(ChatColor.ITALIC);
                        out.append(temp);
                        temp = new StringBuilder();
                    } else {
                        out.append(temp);
                        temp = new StringBuilder();
                        cWidth += tWidth;
                        tWidth = 0;
                    }
                }
            }
            out.append(temp);
            output.add(out.toString());
        }

        for (String s : description) {
            output.add(s);
        }
        return output;
    }

    public ItemStack toItemStack(String locale) {
        ItemStack rStack = item.clone();
        RPGMetadata rpgMeta = new RPGMetadata();
        ItemMeta meta = getLocaleMeta(locale);
        List<String> lore = meta.getLore();
        lore.set(0, meta.getLore().get(0) + rpgMeta.toMCString());
        addExtra(rpgMeta, rStack, lore);
        meta.setLore(lore);
        rStack.setItemMeta(meta);
        return rStack;
    }

    public ItemMeta getLocaleMeta(String locale) {
        ItemMeta meta = localeMeta.get(locale);
        if (meta == null)
            meta = localeMeta.get("en_GB");
        return meta.clone();
    }

    public void setLocaleMeta(String locale, ItemMeta meta) {
        localeMeta.put(locale, meta);
    }

    public String getName() {
        return name;
    }

    public int getID() {
        return id;
    }

    public String getMCEncodedID() {
        return encodedID;
    }

    public static String getMCEncodedID(int id) {
        String hex = String.format("%08x", id);
        StringBuilder out = new StringBuilder();
        for (char h : hex.toCharArray()) {
            out.append(ChatColor.COLOR_CHAR);
            out.append(h);
        }
        return out.toString();
    }

    private static int getStringWidth(String str) {
        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            width += Font.widths[c] + 1;
        }
        return width;
    }

    private static int getStringWidthBold(String str) {
        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            width += Font.widths[c] + 2;
        }
        return width;
    }

    public void print(CommandSender sender) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        List<String> lines = getTooltipLines(locale);
        for (String s : lines) {
            sender.sendMessage(s);
        }
        sender.sendMessage(String.format(Locale.get("message.print.durability", locale), maxDurability));
    }

    public void setDisplay(String str) {
        setDisplay(str, true);
    }

    public void setDisplay(String str, boolean update) {
        displayName = ChatColor.translateAlternateColorCodes('&', str);
        if (update)
            rebuild();
    }

    public String getDisplay() {
        return quality.colour + ChatColor.BOLD + displayName;
    }

    public void setType(String str) {
        setType(str, true);
    }

    public void setType(String str, boolean update) {
        type = ChatColor.translateAlternateColorCodes('&', str);
        if (update)
            rebuild();
    }

    public String getType() {
        return type;
    }

    public void setHand(String h) {
        setHand(h, true);
    }

    public void setHand(String h, boolean update) {
        hand = ChatColor.translateAlternateColorCodes('&', h);
        if (update)
            rebuild();
    }

    public String getHand() {
        return hand;
    }

    public void setDamage(int min, int max) {
        setDamage(min, max, true);
    }

    public void setDamage(int min, int max, boolean update) {
        damageMin = min;
        damageMax = max;
        armour = 0;
        if (update)
            rebuild();
    }

    public int getDamageMin() {
        return damageMin;
    }

    public int getDamageMax() {
        return damageMax;
    }
    
    public int getRecipeChance() {
        return recipechance;
    } 
 
    public void setRecipeChance(int p) {
        setRecipeChance(p, true);
    }

    public void setRecipeChance(int p, boolean update) {
        recipechance = p;
        if (update)
            rebuild();
    }
    
    public String getPermission() {
        return permission;
    } 
    
    public boolean getHasPermission() {
        return haspermission;
    }
    
    public void setPermission(String p) {
        setPermission(p, true);
    }

    public void setPermission(String p, boolean update) {
        permission = p;
        if (update)
            rebuild();
    }

    public void setHaspermission(boolean b) {
        setHaspermission(b, true);
    }

    public void setHaspermission(boolean b, boolean update) {
        haspermission = b;
        if (update)
            rebuild();
    }

    public void setArmour(int a) {
        setArmour(a, true);
    }

    public void setArmour(int a, boolean update) {
        armour = a;
        damageMin = damageMax = 0;
        if (update)
            rebuild();
    }

    public int getArmour() {
        return armour;
    }

    public void setLore(String str) {
        setLore(str, true);
    }

    public void setLore(String str, boolean update) {
        loreText = ChatColor.translateAlternateColorCodes('&', str);
        if (update)
            rebuild();
    }

    public String getLore() {
        return loreText;
    }

    public void setQuality(Quality q) {
        setQuality(q, true);
    }

    public void setQuality(Quality q, boolean update) {
        quality = q;
        if (update)
            rebuild();
    }

    public Quality getQuality() {
        return quality;
    }

    public void setItem(Material mat) {
        setItem(mat, true);
    }

    public void setItem(Material mat, boolean update) {
        if (maxDurability == item.getType().getMaxDurability()) {
            maxDurability = mat.getMaxDurability();
        }
        item.setType(mat);
        if (update)
            rebuild();
    }

    public void setDataValue(short value, boolean update) {
        item.setDurability(value);
        if (update)
            rebuild();
    }

    public void setDataValue(short value) {
        item.setDurability(value);
    }

    public short getDataValue() {
        return item.getDurability();
    }

    public Material getItem() {
        return item.getType();
    }

    public void setMaxDurability(int newVal) {
        setMaxDurability(newVal, true);
    }

    public void setMaxDurability(int newVal, boolean update) {
        maxDurability = newVal;
        if (update)
            rebuild();
    }

    public int getMaxDurability() {
        return maxDurability <= 0 ? -1 : maxDurability;
    }

    public void give(Player player) {
        player.getInventory().addItem(toItemStack(Locale.getPlayerLocale(player)));
    }

    public void addPower(Power power) {
        addPower(power, true);
    }

    public void addPower(Power power, boolean update) {
        powers.add(power);
        Power.powerUsage.put(power.getName(), Power.powerUsage.get(power.getName()) + 1);
        if (power instanceof PowerHit) {
            powerHit.add((PowerHit) power);
        }
        if (power instanceof PowerLeftClick) {
            powerLeftClick.add((PowerLeftClick) power);
        }
        if (power instanceof PowerRightClick) {
            powerRightClick.add((PowerRightClick) power);
        }
        if (power instanceof PowerProjectileHit) {
            powerProjectileHit.add((PowerProjectileHit) power);
        }
        if (power instanceof PowerTick) {
            powerTick.add((PowerTick) power);
        }
        if (update)
            rebuild();
    }

    public boolean removePower(String pow) {
        Iterator<Power> it = powers.iterator();
        Power power = null;
        while (it.hasNext()) {
            Power p = it.next();
            if (p.getName().equalsIgnoreCase(pow)) {
                it.remove();
                power = p;
                rebuild();
                break;
            }
        }
        if (power != null) {
            if (power instanceof PowerHit) {
                powerHit.remove((PowerHit) power);
            }
            if (power instanceof PowerLeftClick) {
                powerLeftClick.remove(power);
            }
            if (power instanceof PowerRightClick) {
                powerRightClick.remove(power);
            }
            if (power instanceof PowerProjectileHit) {
                powerProjectileHit.remove(power);
            }
            if (power instanceof PowerTick) {
                powerTick.remove(power);
            }
        }
        return power != null;
    }

    public void addDescription(String str) {
        addDescription(str, true);
    }

    public void addDescription(String str, boolean update) {
        description.add(ChatColor.translateAlternateColorCodes('&', str));
        if (update)
            rebuild();
    }

    public void toggleBar() {
        forceBar = !forceBar;
        rebuild();
    }

}
