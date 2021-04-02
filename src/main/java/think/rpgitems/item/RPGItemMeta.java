package think.rpgitems.item;

import cat.nyaa.nyaacore.utils.HexColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import think.rpgitems.RPGItems;
import think.rpgitems.utils.MaterialUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.bukkit.ChatColor.COLOR_CHAR;

public class RPGItemMeta {
    boolean ignoreWorldGuard = false;
    boolean showPowerText = true;
    boolean showArmourLore = true;
    boolean customItemModel = false;
    Map<Enchantment, Integer> enchantMap = null;
    List<String> description = new ArrayList<>();
    List<ItemFlag> itemFlags = new ArrayList<>();
    NamespacedKey namespacedKey;
    Material item;
    int dataValue;

    boolean hasPermission;
    String permission;
    String displayName;
    int damageMin = 0;
    int damageMax = 3;
    RPGItem.EnchantMode enchantMode = RPGItem.EnchantMode.DISALLOW;
    RPGItem.DamageMode damageMode = RPGItem.DamageMode.FIXED;
    RPGItem.AttributeMode attributeMode = RPGItem.AttributeMode.PARTIAL_UPDATE;
    int armour = 0;
    String armourExpression = "";
    String damageType = "";
    boolean canBeOwned = false;
    boolean hasStackId = false;
    boolean alwaysAllowMelee = false;

    String author = RPGItems.plugin.cfg.defaultAuthor;
    String note = RPGItems.plugin.cfg.defaultNote;
    String license = RPGItems.plugin.cfg.defaultLicense;

    // Durability
    int maxDurability = -1;
    boolean hasDurabilityBar = RPGItem.plugin.cfg.forceBar;
    int defaultDurability;
    int durabilityLowerBound;
    int durabilityUpperBound;
    RPGItem.BarFormat barFormat = RPGItem.BarFormat.DEFAULT;

    int blockBreakingCost = 0;
    int hittingCost = 0;
    int hitCost = 0;
    boolean hitCostByDamage = false;
    String mcVersion;
    int pluginVersion;
    int pluginSerial;
    List<String> lore;
    int customModelData;
    String quality;
    String type = "item";

    public void save(ConfigurationSection s) {
        s.set("author", getAuthor());
        s.set("note", getNote());
        s.set("license", getLicense());

        s.set("mcVersion", getMcVersion());
        s.set("pluginSerial", getPluginSerial());

        s.set("haspermission", isHasPermission());
        s.set("permission", getPermission());
        s.set("display", getDisplayName().replaceAll("" + COLOR_CHAR, "&"));
        s.set("damageMin", getDamageMin());
        s.set("damageMax", getDamageMax());
        s.set("armour", getArmour());
        s.set("armourExpression", getArmourExpression());
        s.set("DamageType", getDamageType());
        s.set("attributemode", getAttributeMode().name());
        ArrayList<String> descriptionConv = new ArrayList<>(getDescription());
        for (int i = 0; i < descriptionConv.size(); i++) {
            descriptionConv.set(i, descriptionConv.get(i).replaceAll("" + COLOR_CHAR, "&"));
        }
        s.set("description", descriptionConv);
        s.set("item", getItem().toString());
        s.set("ignoreWorldGuard", isIgnoreWorldGuard());
        s.set("canBeOwned", isCanBeOwned());
        s.set("hasStackId", isHasStackId());

        ItemMeta itemMeta = Bukkit.getItemFactory().getItemMeta(getItem());

        if (itemMeta instanceof LeatherArmorMeta) {
            s.set("item_colour", getDataValue());
        } else if (itemMeta instanceof Damageable) {
            s.set("item_data", getDataValue());
        }




        s.set("hitCost", getHitCost());
        s.set("hittingCost", getHittingCost());
        s.set("blockBreakingCost", getBlockBreakingCost());
        s.set("hitCostByDamage", isHitCostByDamage());
        s.set("maxDurability", getMaxDurability());
        s.set("defaultDurability", getDefaultDurability());
        s.set("durabilityLowerBound", getDurabilityLowerBound());
        s.set("durabilityUpperBound", getDurabilityUpperBound());
        s.set("hasDurabilityBar", isHasDurabilityBar());
        s.set("showPowerText", isShowPowerText());
        s.set("showArmourLore", isShowArmourLore());
        s.set("damageMode", getDamageMode().name());
        s.set("customModelData", getCustomModelData());

        Map<Enchantment, Integer> enchantMap = getEnchantMap();
        if (enchantMap != null) {
            ConfigurationSection ench = s.createSection("enchantments");
            for (Enchantment e : enchantMap.keySet()) {
                ench.set(e.getKey().getKey(), enchantMap.get(e));
            }
        } else {
            s.set("enchantments", null);
        }
        s.set("enchantMode", enchantMode.name());
        List<ItemFlag> itemFlags = getItemFlags();
        if (!itemFlags.isEmpty()) {
            List<String> tmp = new ArrayList<>();
            for (ItemFlag flag : itemFlags) {
                tmp.add(flag.name());
            }
            s.set("itemFlags", tmp);
        } else {
            s.set("itemFlags", null);
        }
        s.set("customItemModel", isCustomItemModel());
        s.set("barFormat", getBarFormat().name());
        s.set("alwaysAllowMelee", isAlwaysAllowMelee());

        s.set("quality", getQuality());
        s.set("type", getType());
    }

    public void restore(ConfigurationSection s){
        setAuthor(s.getString("author", ""));
        setNote(s.getString("note", ""));
        setLicense(s.getString("license", ""));
        setPluginVersion(s.getInt("pluginVersion", 0));
        setPluginSerial(s.getInt("pluginSerial", 0));
        setMcVersion(s.getString("mcVersion", ""));

        String display = s.getString("display");

        setDisplayName(display);
        List<String> desc = s.getStringList("description");
        for (int i = 0; i < desc.size(); i++) {
            desc.set(i, HexColorUtils.hexColored(desc.get(i)));
        }
        setDescription(desc);
        setDamageMin(s.getInt("damageMin"));
        setDamageMax(s.getInt("damageMax"));
        setArmour(s.getInt("armour", 0));
        setArmourExpression(s.getString("armourExpression", ""));
        setDamageType(s.getString("DamageType", ""));
        setAttributeMode(RPGItem.AttributeMode.valueOf(s.getString("attributemode", "PARTIAL_UPDATE")));
        String materialName = s.getString("item");
        setItem(MaterialUtils.getMaterial(materialName, Bukkit.getConsoleSender()));
        ItemMeta itemMeta = Bukkit.getItemFactory().getItemMeta(getItem());
        if (itemMeta instanceof LeatherArmorMeta) {
            setDataValue(s.getInt("item_colour"));
        } else if (itemMeta instanceof Damageable) {
            setDataValue(s.getInt("item_data"));
        }
        setIgnoreWorldGuard(s.getBoolean("ignoreWorldGuard", false));
        setCanBeOwned(s.getBoolean("canBeOwned", false));
        setHasStackId(s.getBoolean("hasStackId", false));
    }

    public boolean isIgnoreWorldGuard() {
        return ignoreWorldGuard;
    }

    public void setIgnoreWorldGuard(boolean ignoreWorldGuard) {
        this.ignoreWorldGuard = ignoreWorldGuard;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public boolean isShowPowerText() {
        return showPowerText;
    }

    public void setShowPowerText(boolean showPowerText) {
        this.showPowerText = showPowerText;
    }

    public boolean isShowArmourLore() {
        return showArmourLore;
    }

    public void setShowArmourLore(boolean showArmourLore) {
        this.showArmourLore = showArmourLore;
    }

    public Map<Enchantment, Integer> getEnchantMap() {
        return enchantMap;
    }

    public void setEnchantMap(Map<Enchantment, Integer> enchantMap) {
        this.enchantMap = enchantMap;
    }

    public List<ItemFlag> getItemFlags() {
        return itemFlags;
    }

    public void setItemFlags(List<ItemFlag> itemFlags) {
        this.itemFlags = itemFlags;
    }

    public boolean isCustomItemModel() {
        return customItemModel;
    }

    public void setCustomItemModel(boolean customItemModel) {
        this.customItemModel = customItemModel;
    }

    public RPGItem.EnchantMode getEnchantMode() {
        return enchantMode;
    }

    public void setEnchantMode(RPGItem.EnchantMode enchantMode) {
        this.enchantMode = enchantMode;
    }

    public NamespacedKey getNamespacedKey() {
        return namespacedKey;
    }

    public void setNamespacedKey(NamespacedKey namespacedKey) {
        this.namespacedKey = namespacedKey;
    }

    public Material getItem() {
        return item;
    }

    public void setItem(Material item) {
        this.item = item;
    }

    public int getDataValue() {
        return dataValue;
    }

    public void setDataValue(int dataValue) {
        this.dataValue = dataValue;
    }

    public boolean isHasPermission() {
        return hasPermission;
    }

    public void setHasPermission(boolean hasPermission) {
        this.hasPermission = hasPermission;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getDamageMin() {
        return damageMin;
    }

    public void setDamageMin(int damageMin) {
        this.damageMin = damageMin;
    }

    public int getDamageMax() {
        return damageMax;
    }

    public void setDamageMax(int damageMax) {
        this.damageMax = damageMax;
    }

    public RPGItem.DamageMode getDamageMode() {
        return damageMode;
    }

    public void setDamageMode(RPGItem.DamageMode damageMode) {
        this.damageMode = damageMode;
    }

    public RPGItem.AttributeMode getAttributeMode() {
        return attributeMode;
    }

    public void setAttributeMode(RPGItem.AttributeMode attributeMode) {
        this.attributeMode = attributeMode;
    }

    public int getArmour() {
        return armour;
    }

    public void setArmour(int armour) {
        this.armour = armour;
    }

    public String getArmourExpression() {
        return armourExpression;
    }

    public void setArmourExpression(String armourExpression) {
        this.armourExpression = armourExpression;
    }

    public String getDamageType() {
        return damageType;
    }

    public void setDamageType(String damageType) {
        this.damageType = damageType;
    }

    public boolean isCanBeOwned() {
        return canBeOwned;
    }

    public void setCanBeOwned(boolean canBeOwned) {
        this.canBeOwned = canBeOwned;
    }

    public boolean isHasStackId() {
        return hasStackId;
    }

    public void setHasStackId(boolean hasStackId) {
        this.hasStackId = hasStackId;
    }

    public boolean isAlwaysAllowMelee() {
        return alwaysAllowMelee;
    }

    public void setAlwaysAllowMelee(boolean alwaysAllowMelee) {
        this.alwaysAllowMelee = alwaysAllowMelee;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public void setMaxDurability(int maxDurability) {
        this.maxDurability = maxDurability;
    }

    public boolean isHasDurabilityBar() {
        return hasDurabilityBar;
    }

    public void setHasDurabilityBar(boolean hasDurabilityBar) {
        this.hasDurabilityBar = hasDurabilityBar;
    }

    public int getDefaultDurability() {
        return defaultDurability;
    }

    public void setDefaultDurability(int defaultDurability) {
        this.defaultDurability = defaultDurability;
    }

    public int getDurabilityLowerBound() {
        return durabilityLowerBound;
    }

    public void setDurabilityLowerBound(int durabilityLowerBound) {
        this.durabilityLowerBound = durabilityLowerBound;
    }

    public int getDurabilityUpperBound() {
        return durabilityUpperBound;
    }

    public void setDurabilityUpperBound(int durabilityUpperBound) {
        this.durabilityUpperBound = durabilityUpperBound;
    }

    public RPGItem.BarFormat getBarFormat() {
        return barFormat;
    }

    public void setBarFormat(RPGItem.BarFormat barFormat) {
        this.barFormat = barFormat;
    }

    public int getBlockBreakingCost() {
        return blockBreakingCost;
    }

    public void setBlockBreakingCost(int blockBreakingCost) {
        this.blockBreakingCost = blockBreakingCost;
    }

    public int getHittingCost() {
        return hittingCost;
    }

    public void setHittingCost(int hittingCost) {
        this.hittingCost = hittingCost;
    }

    public int getHitCost() {
        return hitCost;
    }

    public void setHitCost(int hitCost) {
        this.hitCost = hitCost;
    }

    public boolean isHitCostByDamage() {
        return hitCostByDamage;
    }

    public void setHitCostByDamage(boolean hitCostByDamage) {
        this.hitCostByDamage = hitCostByDamage;
    }

    public String getMcVersion() {
        return mcVersion;
    }

    public void setMcVersion(String mcVersion) {
        this.mcVersion = mcVersion;
    }

    public int getPluginVersion() {
        return pluginVersion;
    }

    public void setPluginVersion(int pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public int getPluginSerial() {
        return pluginSerial;
    }

    public void setPluginSerial(int pluginSerial) {
        this.pluginSerial = pluginSerial;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
