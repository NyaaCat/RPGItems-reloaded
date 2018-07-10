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

import cat.nyaa.nyaacore.Message;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.librazy.nyaautils_lang_checker.LangKey;
import org.librazy.nyaautils_lang_checker.LangKeyType;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Font;
import think.rpgitems.data.RPGMetadata;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerLoreFilter;
import think.rpgitems.power.PowerUnbreakable;
import think.rpgitems.power.types.*;
import think.rpgitems.support.WorldGuard;
import think.rpgitems.utils.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.bukkit.ChatColor.COLOR_CHAR;
import static org.bukkit.ChatColor.getByChar;

public class RPGItem {
    public static final int MC_ENCODED_ID_LENGTH = 16;
    static RPGItems plugin;
    public boolean ignoreWorldGuard = false;
    public List<String> description = new ArrayList<>();
    public boolean showPowerLore = true;
    public boolean showArmourLore = true;
    public Map<Enchantment, Integer> enchantMap = null;
    public ArrayList<ItemFlag> itemFlags = new ArrayList<>();
    public boolean customItemModel = false;
    // Powers
    public ArrayList<Power> powers = new ArrayList<>();
    // Recipes
    public int recipechance = 6;
    public boolean hasRecipe = false;
    public List<ItemStack> recipe = null;
    public NamespacedKey namespacedKey;
    // Drops
    public Map<String, Double> dropChances = new HashMap<>();
    public int defaultDurability;
    public int durabilityLowerBound;
    public int durabilityUpperBound;
    public int blockBreakingCost = 1;
    public int hittingCost = 1;
    public int hitCost = 1;
    public boolean hitCostByDamage = false;
    public DamageMode damageMode = DamageMode.FIXED;
    private ItemStack item;
    private ItemMeta localeMeta;
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
    private String type = RPGItems.plugin.getConfig().getString("defaults.sword", "Sword");
    private String hand = RPGItems.plugin.getConfig().getString("defaults.hand", "One handed");
    private ArrayList<PowerLeftClick> powerLeftClick = new ArrayList<>();
    private ArrayList<PowerRightClick> powerRightClick = new ArrayList<>();
    private ArrayList<PowerProjectileHit> powerProjectileHit = new ArrayList<>();
    private ArrayList<PowerHit> powerHit = new ArrayList<>();
    private ArrayList<PowerHitTaken> powerHitTaken = new ArrayList<>();
    private ArrayList<PowerHurt> powerHurt = new ArrayList<>();
    private ArrayList<PowerTick> powerTick = new ArrayList<>();
    private int tooltipWidth = 150;
    // Durability
    private int maxDurability;
    private boolean hasBar = false;
    private boolean forceBar = false;
    private int _loreMinLen = 0;

    public RPGItem(String name, int id) {
        this.name = name;
        this.id = id;
        encodedID = getMCEncodedID(id);
        item = new ItemStack(Material.WOOD_SWORD);
        hasBar = true;

        displayName = item.getType().toString();

        localeMeta = item.getItemMeta();
        itemFlags.add(ItemFlag.HIDE_ATTRIBUTES);
        rebuild();
    }

    public RPGItem(ConfigurationSection s) {

        name = s.getString("name");
        id = s.getInt("id");
        restore(s);
    }

    public RPGItem(ConfigurationSection s, String name, int id) {
        this.name = name;
        this.id = id;
        restore(s);
    }

    @SuppressWarnings("unchecked")
    private void restore(ConfigurationSection s) {
        setDisplay(s.getString("display"), false);
        setType(s.getString("type", RPGItems.plugin.getConfig().getString("defaults.sword", "Sword")), false);
        setHand(s.getString("hand", RPGItems.plugin.getConfig().getString("defaults.hand", "One handed")), false);
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
        localeMeta = meta.clone();
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
                    Power pow = Power.powers.get(section.getString("powerName")).getConstructor().newInstance();
                    pow.init(section);
                    pow.item = this;
                    addPower(pow, false);
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
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
            namespacedKey = new NamespacedKey(RPGItems.plugin, s.getString("namespacedKey", name + "_" + System.currentTimeMillis()));
        }

        ConfigurationSection drops = s.getConfigurationSection("dropChances");
        if (drops != null) {
            for (String key : drops.getKeys(false)) {
                double chance = drops.getDouble(key, 0.0);
                chance = Math.min(chance, 100.0);
                if (chance > 0) {
                    dropChances.put(key, chance);
                    if (!Events.drops.containsKey(key)) {
                        Events.drops.put(key, new HashSet<>());
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
        hitCost = s.getInt("hitCost", 1);
        hittingCost = s.getInt("hittingCost", 1);
        blockBreakingCost = s.getInt("blockBreakingCost", 1);
        hitCostByDamage = s.getBoolean("hitCostByDamage", false);
        maxDurability = s.getInt("maxDurability", item.getType().getMaxDurability());
        defaultDurability = s.getInt("defaultDurability", maxDurability > 0 ? maxDurability : -1);
        durabilityLowerBound = s.getInt("durabilityLowerBound", 0);
        durabilityUpperBound = s.getInt("durabilityUpperBound", item.getType().getMaxDurability());
        forceBar = s.getBoolean("forceBar", false);

        if (maxDurability == 0) {
            maxDurability = -1;
        }

        if (defaultDurability == 0) {
            defaultDurability = maxDurability > 0 ? maxDurability : -1;
        }

        showPowerLore = s.getBoolean("showPowerText", true);
        showArmourLore = s.getBoolean("showArmourLore", true);

        if (s.isConfigurationSection("enchantments")) {
            ConfigurationSection ench = s.getConfigurationSection("enchantments");
            enchantMap = new HashMap<>();
            for (String enchName : ench.getKeys(false)) {
                Enchantment tmp = Enchantment.getByName(enchName);
                if (tmp != null) {
                    enchantMap.put(tmp, ench.getInt(enchName));
                }
            }
        }
        itemFlags = new ArrayList<>();
        if (s.isList("itemFlags")) {
            List<String> flags = s.getStringList("itemFlags");
            for (String flagName : flags) {
                try {
                    itemFlags.add(ItemFlag.valueOf(flagName));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
        customItemModel = s.getBoolean("customItemModel", false);
        String damageModeStr = s.getString("damageMode", "FIXED");
        try {
            damageMode = DamageMode.valueOf(damageModeStr);
        } catch (IllegalArgumentException e) {
            damageMode = DamageMode.FIXED;
        }
        rebuild();
    }

    public static RPGMetadata getMetadata(ItemStack item) {
        // Check for broken item
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore() || item.getItemMeta().getLore().size() == 0) {
            // Broken item
            return new RPGMetadata();
        }
        return RPGMetadata.parseLoreline(item.getItemMeta().getLore().get(0));
    }

    public static void updateItem(ItemStack item) {
        updateItem(item, getMetadata(item));
    }

    public static void updateItem(ItemStack item, RPGMetadata rpgMeta) {
        RPGItem rItem = ItemManager.toRPGItem(item);
        if (rItem == null)
            return;
        List<String> reservedLores = filterLores(rItem, item);
        item.setType(rItem.item.getType());
        ItemMeta meta = rItem.getLocaleMeta();
        List<String> lore = meta.getLore();
        rItem.addExtra(rpgMeta, item, lore);
        lore.set(0, meta.getLore().get(0) + rpgMeta.toMCString());
        // Patch for mcMMO buff. See SkillUtils.java#removeAbilityBuff in mcMMO
        if (item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().contains("mcMMO Ability Tool"))
            lore.add("mcMMO Ability Tool");
        lore.addAll(reservedLores);
        meta.setLore(lore);
        Map<Enchantment, Integer> enchs = item.getEnchantments();
        if (enchs.size() > 0)
            for (Enchantment ench : enchs.keySet())
                meta.addEnchant(ench, enchs.get(ench), true);
        item.setItemMeta(meta);
    }

    private static List<String> filterLores(RPGItem r, ItemStack i) {
        List<String> ret = new ArrayList<>();
        List<Pattern> patterns = new ArrayList<>();
        for (Power p : r.powers) {
            if (p instanceof PowerLoreFilter && ((PowerLoreFilter) p).regex != null) {
                patterns.add(Pattern.compile(((PowerLoreFilter) p).regex));
            }
        }
        if (patterns.size() <= 0) return Collections.emptyList();
        if (!i.hasItemMeta() || !i.getItemMeta().hasLore()) return Collections.emptyList();
        for (String str : i.getItemMeta().getLore()) {
            for (Pattern p : patterns) {
                if (p.matcher(ChatColor.stripColor(str)).matches()) {
                    ret.add(str);
                    break;
                }
            }
        }
        return ret;
    }

    public static String getMCEncodedID(int id) {
        String hex = String.format("%08x", id);
        StringBuilder out = new StringBuilder();
        for (char h : hex.toCharArray()) {
            out.append(COLOR_CHAR);
            out.append(h);
        }
        String str = out.toString();
        if (str.length() != MC_ENCODED_ID_LENGTH)
            throw new RuntimeException("Bad RPGItem ID: " + str + " (" + id + ")");
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

    public static RPGItems getPlugin() {
        return plugin;
    }

    public void save(ConfigurationSection s) {
        s.set("name", name);
        s.set("id", id);
        s.set("haspermission", haspermission);
        s.set("permission", permission);
        s.set("display", displayName.replaceAll("" + COLOR_CHAR, "&"));
        s.set("quality", quality.toString());
        s.set("damageMin", damageMin);
        s.set("damageMax", damageMax);
        s.set("armour", armour);
        s.set("type", type.replaceAll("" + COLOR_CHAR, "&"));
        s.set("hand", hand.replaceAll("" + COLOR_CHAR, "&"));
        s.set("lore", loreText.replaceAll("" + COLOR_CHAR, "&"));
        ArrayList<String> descriptionConv = new ArrayList<>(description);
        for (int i = 0; i < descriptionConv.size(); i++) {
            descriptionConv.set(i, descriptionConv.get(i).replaceAll("" + COLOR_CHAR, "&"));
        }
        s.set("description", descriptionConv);
        s.set("item", item.getType().toString());
        s.set("ignoreWorldGuard", ignoreWorldGuard);

        ItemMeta meta = localeMeta;
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
            s.set("namespacedKey", namespacedKey.getKey());
        }

        ConfigurationSection drops = s.createSection("dropChances");
        for (String key : dropChances.keySet()) {
            drops.set(key, dropChances.get(key));
        }

        s.set("hitCost", hitCost);
        s.set("hittingCost", hittingCost);
        s.set("blockBreakingCost", blockBreakingCost);
        s.set("hitCostByDamage", hitCostByDamage);
        s.set("maxDurability", maxDurability);
        s.set("defaultDurability", defaultDurability);
        s.set("durabilityLowerBound", durabilityLowerBound);
        s.set("durabilityUpperBound", durabilityUpperBound);
        s.set("forceBar", forceBar);
        s.set("showPowerText", showPowerLore);
        s.set("showArmourLore", showArmourLore);
        s.set("damageMode", damageMode.name());

        if (enchantMap != null) {
            ConfigurationSection ench = s.createSection("enchantments");
            for (Enchantment e : enchantMap.keySet()) {
                ench.set(e.getName(), enchantMap.get(e));
            }
        } else {
            s.set("enchantments", null);
        }
        if (!itemFlags.isEmpty()) {
            List<String> tmp = new ArrayList<>();
            for (ItemFlag flag : itemFlags) {
                tmp.add(flag.name());
            }
            s.set("itemFlags", tmp);
        } else {
            s.set("itemFlags", null);
        }
        s.set("customItemModel", customItemModel);
    }

    public void resetRecipe(boolean removeOld) {
        boolean hasOldRecipe = false;
        if (removeOld) {
            Iterator<Recipe> it = Bukkit.recipeIterator();
            while (it.hasNext()) {
                Recipe recipe = it.next();
                RPGItem rpgitem = ItemManager.toRPGItem(recipe.getResult());
                if (rpgitem == null)
                    continue;
                if (rpgitem.getID() == getID()) {
                    hasOldRecipe = true;
                }
            }
        }
        if (hasRecipe) {
            if (namespacedKey == null || hasOldRecipe) {
                namespacedKey = new NamespacedKey(RPGItems.plugin, name + "_" + System.currentTimeMillis());
            }
            item.setItemMeta(localeMeta);
            ShapedRecipe shapedRecipe = new ShapedRecipe(namespacedKey, toItemStack());

            Map<ItemStack, Character> charMap = new HashMap<>();
            int i = 0;
            for (ItemStack s : recipe) {
                if (!charMap.containsKey(s)) {
                    charMap.put(s, (char) (65 + (i++)));
                }
            }

            StringBuilder shape = new StringBuilder();
            for (ItemStack m : recipe) {
                shape.append(charMap.get(m));
            }
            shapedRecipe.shape(shape.substring(0, 3), shape.substring(3, 6), shape.substring(6, 9));

            for (Entry<ItemStack, Character> e : charMap.entrySet()) {
                if (e.getKey() != null) {
                    shapedRecipe.setIngredient(e.getValue(), e.getKey().getData());
                }
            }
            Bukkit.addRecipe(shapedRecipe);
        }
    }

    public void leftClick(Player player, ItemStack i, Block block) {
        for (PowerLeftClick power : powerLeftClick) {
            if (!WorldGuard.canUsePowerNow(player, power)) continue;
            power.leftClick(player, i, block);
        }
        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void rightClick(Player player, ItemStack i, Block block) {
        for (PowerRightClick power : powerRightClick) {
            if (!WorldGuard.canUsePowerNow(player, power)) continue;
            power.rightClick(player, i, block);
        }
        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void projectileHit(Player player, ItemStack i, Projectile arrow) {
        for (PowerProjectileHit power : powerProjectileHit) {
            if (!WorldGuard.canUsePowerNow(player, power)) continue;
            power.projectileHit(player, i, arrow);
        }
        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void hit(Player damager, ItemStack i, LivingEntity target, double damage) {
        for (PowerHit power : powerHit) {
            if (!WorldGuard.canUsePowerNow(damager, power)) continue;
            power.hit(damager, i, target, damage);
        }
        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public double takeHit(Player target, ItemStack i, EntityDamageEvent ev) {
        double ret = Double.MAX_VALUE;
        for (PowerHitTaken power : powerHitTaken) {
            if (!WorldGuard.canUsePowerNow(target, power)) continue;
            double d = power.takeHit(target, i, ev);
            if (d < 0) continue;
            ret = d < ret ? d : ret;
        }
        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
        return ret == Double.MAX_VALUE ? -1 : ret;
    }

    public void hurt(Player target, ItemStack i, EntityDamageEvent ev) {
        for (PowerHurt power : powerHurt) {
            if (!WorldGuard.canUsePowerNow(target, power)) continue;
            power.hurt(target, i, ev);
        }
        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void tick(Player player, ItemStack i) {
        for (PowerTick power : powerTick) {
            if (!WorldGuard.canUsePowerNow(player, power)) continue;
            power.tick(player, i);
        }
    }

    public void rebuild() {
        hasBar = item.getType().getMaxDurability() != 0;
        List<String> lines = getTooltipLines();
        ItemMeta meta = getLocaleMeta();
        meta.setDisplayName(lines.get(0));
        lines.remove(0);
        if (lines.size() > 0) {
            lines.set(0, getMCEncodedID() + lines.get(0));
        } else {
            lines.add(0, getMCEncodedID());
        }
        meta.setLore(lines);
        meta.setUnbreakable(false);
        for (Power p : powers) {
            if (p instanceof PowerUnbreakable) {
                meta.setUnbreakable(true);
                break;
            }
        }
        if (customItemModel) {
            meta.setUnbreakable(true);
        }
        for (ItemFlag flag : meta.getItemFlags()) {
            meta.removeItemFlags(flag);
        }
        for (ItemFlag flag : itemFlags) {
            meta.addItemFlags(flag);
        }
        Set<Enchantment> enchs = meta.getEnchants().keySet();
        for (Enchantment e : enchs) {
            meta.removeEnchant(e);
        }
        if (enchantMap != null) {
            for (Enchantment e : enchantMap.keySet()) {
                meta.addEnchant(e, enchantMap.get(e), true);
            }
        }
        updateLocaleMeta(meta);

        for (Player player : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : player.getInventory()) {
                if (ItemManager.toRPGItem(item) != null)
                    updateItem(item);
            }
            for (ItemStack item : player.getInventory().getArmorContents()) {
                if (ItemManager.toRPGItem(item) != null)
                    updateItem(item);

            }
        }
        resetRecipe(true);
    }

    public ItemMeta getLocaleMeta() {
        return localeMeta.clone();
    }

    public void addExtra(RPGMetadata rpgMeta, ItemStack stack, List<String> lore) {
        if (maxDurability > 0) {
            if (!rpgMeta.containsKey(RPGMetadata.DURABILITY)) {
                rpgMeta.put(RPGMetadata.DURABILITY, defaultDurability);
            }
            int durability = ((Number) rpgMeta.get(RPGMetadata.DURABILITY)).intValue();

            if (!hasBar || forceBar || customItemModel) {
                StringBuilder out = new StringBuilder();
                char boxChar = '\u25A0';
                int boxCount = tooltipWidth / 4;
                int mid = (int) ((double) boxCount * ((double) durability / (double) maxDurability));
                for (int i = 0; i < boxCount; i++) {
                    out.append(i < mid ? ChatColor.GREEN : i == mid ? ChatColor.YELLOW : ChatColor.RED);
                    out.append(boxChar);
                }
                if (!lore.get(lore.size() - 1).contains(boxChar + ""))
                    lore.add(out.toString());
                else
                    lore.set(lore.size() - 1, out.toString());
            }
            if (customItemModel) {
                stack.setDurability(item.getDurability());
            } else {
                stack.setDurability((short) (stack.getType().getMaxDurability() - ((short) ((double) stack.getType().getMaxDurability() * ((double) durability / (double) maxDurability)))));
            }
        } else if (maxDurability <= 0) {
            if (customItemModel) {
                stack.setDurability(item.getDurability());
            } else {
                stack.setDurability(hasBar ? (short) 0 : item.getDurability());
            }
        }
    }

    public List<String> getTooltipLines() {
        ArrayList<String> output = new ArrayList<>();
        output.add(encodedID + quality.colour + ChatColor.BOLD + displayName);

        // add powerLores
        if (showPowerLore) {
            for (Power p : powers) {
                String txt;
                try {
                    txt = p.displayText();
                } catch (IllegalFormatConversionException ex) {
                    txt = "Power " + p.getName() + ": bad description";
                }
                if (txt != null && txt.length() > 0) {
                    output.add(txt);
                }
            }
        }

        // compute loreMinLen
        int loreIndex = output.size();
        if (loreText.length() > 0) {
            wrapLines(String.format("%s%s\"%s\"",
                    ChatColor.YELLOW, ChatColor.ITALIC,
                    ChatColor.translateAlternateColorCodes('&', loreText)), 0);
        } else {
            _loreMinLen = 0;
        }

        // add descriptions
        output.addAll(description);

        // compute width
        int width = 0;
        for (String str : output) {
            width = Math.max(width, getStringWidth(ChatColor.stripColor(str)));
        }

        // compute armorMinLen
        int armorMinLen = 0;
        String damageStr = null;
        if (showArmourLore) {
            armorMinLen = getStringWidth(ChatColor.stripColor(hand + "     " + type));

            if (armour != 0) {
                damageStr = armour + "% " + RPGItems.plugin.getConfig().getString("defaults.armour", "Armour");
            }
            if ((damageMin != 0 || damageMax != 0) && damageMode != DamageMode.VANILLA) {
                damageStr = damageStr == null ? "" : damageStr + " & ";
                if (damageMode == DamageMode.ADDITIONAL) {
                    damageStr += RPGItems.plugin.getConfig().getString("defaults.additionaldamage", "Additional ");
                } else if (damageMode == DamageMode.MULTIPLY) {
                    damageStr += RPGItems.plugin.getConfig().getString("defaults.multiplydamage", "Times ");
                }
                if (damageMin == damageMax) {
                    damageStr += damageMin + " " + RPGItems.plugin.getConfig().getString("defaults.damage", "Damage");
                } else {
                    damageStr += damageMin + "-" + damageMax + " " + RPGItems.plugin.getConfig().getString("defaults.damage", "Damage");
                }
            }
            if (damageStr != null) {
                armorMinLen = Math.max(armorMinLen, getStringWidth(ChatColor.stripColor(damageStr)));
            }
        }
        tooltipWidth = width = Math.max(width, Math.max(_loreMinLen, armorMinLen));

        if (loreText.length() > 0) {
            for (String str : wrapLines(String.format("%s%s\"%s\"", ChatColor.YELLOW, ChatColor.ITALIC,
                    ChatColor.translateAlternateColorCodes('&', loreText)), tooltipWidth)) {
                output.add(loreIndex++, str);
            }
        }

        if (showArmourLore) {
            if (damageStr != null) {
                output.add(1, ChatColor.WHITE + damageStr);
            }
            output.add(1, ChatColor.WHITE + hand + StringUtils.repeat(" ", (width - getStringWidth(ChatColor.stripColor(hand + type))) / 4) + type);
        }

        return output;
    }

    private List<String> wrapLines(String txt, int maxwidth) {
        List<String> words = new ArrayList<>();
        for (String word : txt.split(" ")) {
            if (word.length() > 0)
                words.add(word);
        }
        if (words.size() <= 0) return Collections.emptyList();

        for (String str : words) {
            int len = getStringWidth(ChatColor.stripColor(str));
            _loreMinLen = len;
            if (len > maxwidth) maxwidth = len;
        }

        List<String> ans = new ArrayList<>();
        int idx = 0, currlen = getStringWidth(ChatColor.stripColor(words.get(0)));
        ans.add(words.remove(0));
        while (words.size() > 0) {
            String tmp = words.remove(0);
            int word_len = getStringWidth(ChatColor.stripColor(tmp));
            if (currlen + 4 + word_len <= maxwidth) {
                currlen += 4 + word_len;
                ans.set(idx, ans.get(idx) + " " + tmp);
            } else {
                currlen = word_len;
                ans.add(tmp);
                idx++;
            }
        }
        for (int i = 1; i < ans.size(); i++) {
            ans.set(i, getLastFormat(ans.get(i - 1)) + ans.get(i));
        }
        return ans;
    }

    private String getLastFormat(String str) {
        String format = null;
        int length = str.length();

        for (int index = length - 2; index > -1; index--) {
            char chr = str.charAt(index);
            if (chr == COLOR_CHAR) {
                char c = str.charAt(index + 1);
                ChatColor style = getByChar(c);
                if (style == null) continue;
                if (style.isColor()) return style.toString() + (format == null ? "" : format);
                if (style.isFormat() && format == null) format = style.toString();
            }
        }

        return (format == null ? "" : format);
    }

    public ItemStack toItemStack() {
        ItemStack rStack = item.clone();
        RPGMetadata rpgMeta = new RPGMetadata();
        ItemMeta meta = getLocaleMeta();
        List<String> lore = meta.getLore();
        lore.set(0, meta.getLore().get(0) + rpgMeta.toMCString());
        addExtra(rpgMeta, rStack, lore);
        meta.setLore(lore);
        rStack.setItemMeta(meta);
        return rStack;
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

    public void print(CommandSender sender) {
        if (sender instanceof Player) {
            new Message("")
                    .append(I18n.format("message.item.print"), toItemStack())
                    .send(sender);
        } else {
            List<String> lines = getTooltipLines();
            for (int i = 0; i < lines.size(); i++) {
                sender.sendMessage(lines.get(i));
            }
        }
        sender.sendMessage(I18n.format("message.durability.info", getMaxDurability(), defaultDurability, durabilityLowerBound, durabilityUpperBound));
        if (customItemModel) {
            sender.sendMessage(I18n.format("message.print.customitemmodel", item.getType().name() + ":" + item.getDurability()));
        }
        if (!itemFlags.isEmpty()) {
            StringBuilder str = new StringBuilder();
            for (ItemFlag flag : itemFlags) {
                if (str.length() > 0) {
                    str.append(", ");
                }
                str.append(flag.name());
            }
            sender.sendMessage(I18n.format("message.print.itemflags") + str);
        }
    }

    public void setDisplay(String str, boolean update) {
        displayName = ChatColor.translateAlternateColorCodes('&', str);
        if (update)
            rebuild();
    }

    public String getDisplay() {
        return quality.colour + ChatColor.BOLD + displayName;
    }

    public void setDisplay(String str) {
        setDisplay(str, true);
    }

    public void setType(String str, boolean update) {
        type = ChatColor.translateAlternateColorCodes('&', str);
        if (update)
            rebuild();
    }

    public String getType() {
        return type;
    }

    public void setType(String str) {
        setType(str, true);
    }

    public void setHand(String h, boolean update) {
        hand = ChatColor.translateAlternateColorCodes('&', h);
        if (update)
            rebuild();
    }

    public String getHand() {
        return hand;
    }

    public void setHand(String h) {
        setHand(h, true);
    }

    public void setDamage(int min, int max) {
        setDamage(min, max, true);
    }

    public void setDamage(int min, int max, boolean update) {
        damageMin = min;
        damageMax = max;
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

    public void setPermission(String p) {
        setPermission(p, true);
    }

    public boolean getHasPermission() {
        return haspermission;
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

    public boolean checkPermission(Player p, boolean showWarn) {
        if (getHasPermission() && !p.hasPermission(getPermission())) {
            if (showWarn) p.sendMessage(I18n.format("message.error.permission", getDisplay()));
            return false;
        }
        return true;
    }

    public void setArmour(int a, boolean update) {
        armour = a;
        if (update)
            rebuild();
    }

    public int getArmour() {
        return armour;
    }

    public void setArmour(int a) {
        setArmour(a, true);
    }

    public void setLore(String str, boolean update) {
        loreText = ChatColor.translateAlternateColorCodes('&', str);
        if (update)
            rebuild();
    }

    public String getLore() {
        return loreText;
    }

    public void setLore(String str) {
        setLore(str, true);
    }

    public void setQuality(Quality q, boolean update) {
        quality = q;
        if (update)
            rebuild();
    }

    public Quality getQuality() {
        return quality;
    }

    public void setQuality(Quality q) {
        setQuality(q, true);
    }

    public void setItem(Material mat, boolean update) {
        if (maxDurability == item.getType().getMaxDurability()) {
            maxDurability = mat.getMaxDurability();
        }
        item.setType(mat);
        updateLocaleMeta(item.getItemMeta());
        if (update)
            rebuild();
    }

    public void setDataValue(short value, boolean update) {
        item.setDurability(value);
        if (update)
            rebuild();
    }

    public short getDataValue() {
        return item.getDurability();
    }

    public void setDataValue(short value) {
        item.setDurability(value);
    }

    public Material getItem() {
        return item.getType();
    }

    public void setItem(Material mat) {
        setItem(mat, true);
    }

    public void setMaxDurability(int newVal, boolean update) {
        maxDurability = newVal;
        if (update)
            rebuild();
    }

    public void setDefaultDurability(int newVal) {
        defaultDurability = newVal;
    }

    public void setDurabilityBound(int min, int max) {
        durabilityLowerBound = min;
        durabilityUpperBound = max;
    }

    public int getMaxDurability() {
        return maxDurability <= 0 ? -1 : maxDurability;
    }

    public void setMaxDurability(int newVal) {
        if (defaultDurability == 0){
            setDefaultDurability(newVal);
        }
        setMaxDurability(newVal, true);
    }

    public int getDurability(ItemStack item) {
        RPGMetadata meta = getMetadata(item);
        int durability = Integer.MAX_VALUE;
        if (getMaxDurability() != -1) {
            durability = meta.containsKey(RPGMetadata.DURABILITY) ? ((Number) meta.get(RPGMetadata.DURABILITY)).intValue() : getMaxDurability();
        }
        return durability;
    }

    public boolean consumeDurability(ItemStack item, int val) {
        if (val == 0) return true;
        RPGMetadata meta = getMetadata(item);
        int durability;
        if (getMaxDurability() != -1) {
            durability = meta.containsKey(RPGMetadata.DURABILITY) ? ((Number) meta.get(RPGMetadata.DURABILITY)).intValue() : defaultDurability;
            if ((val > 0 && durability < durabilityLowerBound)
                        || (val < 0 && durability > durabilityUpperBound)) {
                return false;
            }
            if (durability <= val
                        && getLocaleMeta().isUnbreakable()
                        && !customItemModel) {
                return false;
            }
            durability -= val;
            if (durability > getMaxDurability()) {
                durability = getMaxDurability();
            }
            meta.put(RPGMetadata.DURABILITY, durability);
        }
        updateItem(item, meta);

        return true;
    }

    public void give(Player player) {
        player.getInventory().addItem(toItemStack());
    }

    public boolean hasPower(Class<? extends Power> power) {
        return powers.stream().anyMatch(p -> p.getClass().equals(power));
    }

    public <T extends Power> List<T> getPower(Class<T> power) {
        return powers.stream().filter(p -> p.getClass().equals(power)).map(power::cast).collect(Collectors.toList());
    }

    public <T extends Power> List<T> getPower(Class<T> power, boolean subclass) {
        return subclass ? powers.stream().filter(power::isInstance).map(power::cast).collect(Collectors.toList()) : getPower(power);
    }

    public void addPower(Power power) {
        addPower(power, true);
    }

    public void addPower(Power power, boolean update) {
        powers.add(power);
        Power.powerUsage.add(power.getName());
        if (power instanceof PowerHit) {
            powerHit.add((PowerHit) power);
        }
        if (power instanceof PowerHitTaken) {
            powerHitTaken.add((PowerHitTaken) power);
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
        if (power instanceof PowerHurt) {
            powerHurt.add((PowerHurt) power);
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
                powerHit.remove(power);
            }
            if (power instanceof PowerHitTaken) {
                powerHitTaken.remove(power);
            }
            if (power instanceof PowerHurt) {
                powerHurt.remove(power);
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

    public void updateLocaleMeta(ItemMeta meta) {
        this.localeMeta = meta;
    }

    public BaseComponent getComponent() {
        BaseComponent msg = new TextComponent(getDisplay());
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/rpgitem " + getName()));
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                                                 new BaseComponent[]{new TextComponent(ReflectionUtil.convertItemStackToJson(toItemStack()))});
        msg.setHoverEvent(hover);
        return msg;
    }

    @LangKey(type = LangKeyType.SUFFIX)
    public enum DamageMode {
        FIXED,
        VANILLA,
        ADDITIONAL,
        MULTIPLY,
    }
}
