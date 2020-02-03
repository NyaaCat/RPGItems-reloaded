package think.rpgitems.item;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Multimap;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.AdminCommands;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Context;
import think.rpgitems.power.*;
import think.rpgitems.power.cond.SlotCondition;
import think.rpgitems.power.marker.*;
import think.rpgitems.power.propertymodifier.Modifier;
import think.rpgitems.power.trigger.BaseTriggers;
import think.rpgitems.power.trigger.Trigger;
import think.rpgitems.utils.MaterialUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.bukkit.ChatColor.COLOR_CHAR;
import static think.rpgitems.utils.ItemTagUtils.*;

public class RPGItem {
    @Deprecated
    public static final int MC_ENCODED_ID_LENGTH = 16;
    public static final NamespacedKey TAG_META = new NamespacedKey(RPGItems.plugin, "meta");
    public static final NamespacedKey TAG_ITEM_UID = new NamespacedKey(RPGItems.plugin, "item_uid");
    public static final NamespacedKey TAG_IS_MODEL = new NamespacedKey(RPGItems.plugin, "is_model");
    public static final NamespacedKey TAG_DURABILITY = new NamespacedKey(RPGItems.plugin, "durability");
    public static final NamespacedKey TAG_OWNER = new NamespacedKey(RPGItems.plugin, "owner");
    public static final NamespacedKey TAG_STACK_ID = new NamespacedKey(RPGItems.plugin, "stack_id");
    public static final NamespacedKey TAG_MODIFIER = new NamespacedKey(RPGItems.plugin, "property_modifier");
    public static final NamespacedKey TAG_VERSION = new NamespacedKey(RPGItems.plugin, "version");
    public static final String DAMAGE_TYPE = "RGI_DAMAGE_TYPE";

    private static final Cache<UUID, List<Modifier>> modifierCache = CacheBuilder.newBuilder().concurrencyLevel(1).expireAfterAccess(1, TimeUnit.MINUTES).build();

    static RPGItems plugin;
    private boolean ignoreWorldGuard = false;
    private List<String> description = new ArrayList<>();
    private boolean showPowerText = true;
    private boolean showArmourLore = true;
    private Map<Enchantment, Integer> enchantMap = null;
    private List<ItemFlag> itemFlags = new ArrayList<>();
    private boolean customItemModel = false;
    private EnchantMode enchantMode = EnchantMode.DISALLOW;

    // Powers
    private List<Power> powers = new ArrayList<>();
    private List<Condition<?>> conditions = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();
    @SuppressWarnings("rawtypes")
    private Map<String, Trigger> triggers = new HashMap<>();
    private HashMap<PropertyHolder, NamespacedKey> keys = new HashMap<>();
    private File file;

    private NamespacedKey namespacedKey;
    private Material item;
    private int dataValue;
    private int id;
    private int uid;
    private String name;
    private boolean hasPermission;
    private String permission;
    private String displayName;
    private int damageMin = 0;
    private int damageMax = 3;
    private DamageMode damageMode = DamageMode.FIXED;
    private AttributeMode attributeMode = AttributeMode.PARTIAL_UPDATE;
    private int armour = 0;
    private String armourExpression = "";
    private String damageType = "";
    private boolean canBeOwned = false;
    private boolean hasStackId = false;
    private boolean alwaysAllowMelee = false;

    private String author = plugin.cfg.defaultAuthor;
    private String note = plugin.cfg.defaultNote;
    private String license = plugin.cfg.defaultLicense;

    private int tooltipWidth = 150;
    // Durability
    private int maxDurability = -1;
    private boolean hasDurabilityBar = plugin.cfg.forceBar;
    private int defaultDurability;
    private int durabilityLowerBound;
    private int durabilityUpperBound;
    private BarFormat barFormat = BarFormat.DEFAULT;

    private int blockBreakingCost = 0;
    private int hittingCost = 0;
    private int hitCost = 0;
    private boolean hitCostByDamage = false;
    private String mcVersion;
    private int pluginVersion;
    private int pluginSerial;
    private List<String> lore;

    public RPGItem(String name, int uid, CommandSender author) {
        this.name = name;
        this.uid = uid;
        this.setAuthor(author instanceof Player ? ((Player) author).getUniqueId().toString() : plugin.cfg.defaultAuthor);
        setEnchantMode(plugin.cfg.defaultEnchantMode);
        setItem(Material.WOODEN_SWORD);
        setDisplayName(getItem().toString());
        getItemFlags().add(ItemFlag.HIDE_ATTRIBUTES);
        setMcVersion(RPGItems.getServerMCVersion());
        setPluginSerial(RPGItems.getSerial());
        setPluginVersion(RPGItems.getVersion());
        rebuild();
    }

    public RPGItem(ConfigurationSection s, File f) throws UnknownPowerException, UnknownExtensionException {
        setFile(f);
        name = s.getString("name");
        id = s.getInt("id");
        uid = s.getInt("uid");

        if (uid == 0) {
            uid = ItemManager.nextUid();
        }
        restore(s);
    }

    public RPGItem(ConfigurationSection s, String name, int uid) throws UnknownPowerException, UnknownExtensionException {
        if (uid >= 0) throw new IllegalArgumentException();
        this.name = name;
        this.uid = uid;
        restore(s);
    }

    public static void updateItemStack(ItemStack item) {
        Optional<RPGItem> rItem = ItemManager.toRPGItem(item);
        rItem.ifPresent(r -> r.updateItem(item, false));
    }

    private void restore(ConfigurationSection s) throws UnknownPowerException, UnknownExtensionException {
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
            desc.set(i, ChatColor.translateAlternateColorCodes('&', desc.get(i)));
        }
        setDescription(desc);
        setDamageMin(s.getInt("damageMin"));
        setDamageMax(s.getInt("damageMax"));
        setArmour(s.getInt("armour", 0), false);
        setArmourExpression(s.getString("armourExpression", ""));
        setDamageType(s.getString("DamageType", ""));
        setAttributeMode(AttributeMode.valueOf(s.getString("attributemode", "PARTIAL_UPDATE")));
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
        // Powers
        ConfigurationSection powerList = s.getConfigurationSection("powers");
        if (powerList != null) {
            for (String sectionKey : powerList.getKeys(false)) {
                ConfigurationSection section = powerList.getConfigurationSection(sectionKey);
                String powerName = Objects.requireNonNull(section).getString("powerName");
                // 3.7 -> 3.8 Migration
                if (Objects.requireNonNull(powerName).endsWith("condition")) {
                    loadCondition(section, powerName);
                } else if (Stream.of("attributemodifier", "lorefilter", "ranged", "rangedonly", "selector", "unbreakable").anyMatch(powerName::endsWith)) {
                    loadMarker(section, powerName);
                } else {
                    loadPower(section, powerName);
                }
            }
        }
        // Conditions
        ConfigurationSection conditionList = s.getConfigurationSection("conditions");
        if (conditionList != null) {
            for (String sectionKey : conditionList.getKeys(false)) {
                ConfigurationSection section = Objects.requireNonNull(conditionList).getConfigurationSection(sectionKey);
                String conditionName = Objects.requireNonNull(Objects.requireNonNull(section).getString("conditionName"));
                loadCondition(section, conditionName);
            }
        }
        // Markers
        ConfigurationSection markerList = s.getConfigurationSection("markers");
        if (markerList != null) {
            for (String sectionKey : markerList.getKeys(false)) {
                ConfigurationSection section = Objects.requireNonNull(markerList).getConfigurationSection(sectionKey);
                String markerName = Objects.requireNonNull(Objects.requireNonNull(section).getString("markerName"));
                loadMarker(section, markerName);
            }
        }
        // Triggers
        ConfigurationSection triggerList = s.getConfigurationSection("triggers");
        if (triggerList != null) {
            for (String sectionKey : triggerList.getKeys(false)) {
                ConfigurationSection section = Objects.requireNonNull(triggerList).getConfigurationSection(sectionKey);
                loadTrigger(section, sectionKey);
            }
        }

        setHasPermission(s.getBoolean("haspermission", false));
        setPermission(s.getString("permission", "rpgitem.item." + name));
        setCustomItemModel(s.getBoolean("customItemModel", false));

        setHitCost(s.getInt("hitCost", 1));
        setHittingCost(s.getInt("hittingCost", 1));
        setBlockBreakingCost(s.getInt("blockBreakingCost", 1));
        setHitCostByDamage(s.getBoolean("hitCostByDamage", false));
        setMaxDurability(s.getInt("maxDurability", getItem().getMaxDurability()));
        setDefaultDurability(s.getInt("defaultDurability", getMaxDurability()));
        if (getDefaultDurability() <= 0) {
            setDefaultDurability(getMaxDurability());
        }
        setDurabilityLowerBound(s.getInt("durabilityLowerBound", 0));
        setDurabilityUpperBound(s.getInt("durabilityUpperBound", getItem().getMaxDurability()));
        if (s.isBoolean("forceBar")) {
            setHasDurabilityBar(getItem().getMaxDurability() == 0 || s.getBoolean("forceBar") || isCustomItemModel());
        }
        setHasDurabilityBar(s.getBoolean("hasDurabilityBar", isHasDurabilityBar()));

        setShowPowerText(s.getBoolean("showPowerText", true));
        setShowArmourLore(s.getBoolean("showArmourLore", true));

        if (s.isConfigurationSection("enchantments")) {
            ConfigurationSection enchConf = s.getConfigurationSection("enchantments");
            setEnchantMap(new HashMap<>());
            for (String enchName : Objects.requireNonNull(enchConf).getKeys(false)) {
                Enchantment ench;
                try {
                    ench = Enchantment.getByKey(NamespacedKey.minecraft(enchName));
                } catch (IllegalArgumentException e) {
                    @SuppressWarnings("deprecation")
                    Enchantment old = Enchantment.getByName(enchName);
                    if (old == null) {
                        throw new IllegalArgumentException("Unknown enchantment " + enchName);
                    }
                    ench = old;
                }
                if (ench != null) {
                    getEnchantMap().put(ench, enchConf.getInt(enchName));
                }
            }
        }
        String enchantModeStr = s.getString("enchantMode", plugin.cfg.defaultEnchantMode.name());
        try {
            setEnchantMode(EnchantMode.valueOf(enchantModeStr));
        } catch (IllegalArgumentException e) {
            setEnchantMode(EnchantMode.DISALLOW);
        }
        setItemFlags(new ArrayList<>());
        if (s.isList("itemFlags")) {
            List<String> flags = s.getStringList("itemFlags");
            for (String flagName : flags) {
                try {
                    getItemFlags().add(ItemFlag.valueOf(flagName));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING, "Ignoring unknown item flags", e);
                }
            }
        }
        if (s.isBoolean("numericBar")) {
            setBarFormat(s.getBoolean("numericBar") ? BarFormat.NUMERIC : BarFormat.DEFAULT);
        }
        if (s.isString("barFormat")) {
            setBarFormat(BarFormat.valueOf(s.getString("barFormat")));
        }
        String damageModeStr = s.getString("damageMode", "FIXED");
        try {
            setDamageMode(DamageMode.valueOf(damageModeStr));
        } catch (IllegalArgumentException e) {
            setDamageMode(DamageMode.FIXED);
        }
        setAlwaysAllowMelee(s.getBoolean("alwaysAllowMelee", false));
        rebuild();
    }

    public void setArmourExpression(String armour) {
        this.armourExpression = armour;
    }

    private void loadPower(ConfigurationSection section, String powerName) throws UnknownPowerException {
        NamespacedKey key = PowerManager.parseKey(powerName);
        Class<? extends Power> power = PowerManager.getPower(key);
        if (power == null) {
            plugin.getLogger().warning("Unknown power:" + key + " on item " + this.name);
            throw new UnknownPowerException(key);
        }
        Power pow = PowerManager.instantiate(power);
        pow.setItem(this);
        pow.init(section);
        addPower(key, pow, false);
    }

    private void loadCondition(ConfigurationSection section, String powerName) throws UnknownPowerException {
        NamespacedKey key = PowerManager.parseKey(powerName);
        Class<? extends Condition<?>> condition = PowerManager.getCondition(key);
        if (condition == null) {
            plugin.getLogger().warning("Unknown condition:" + key + " on item " + this.name);
            throw new UnknownPowerException(key);
        }
        Condition<?> pow = PowerManager.instantiate(condition);
        pow.setItem(this);
        pow.init(section);
        addCondition(key, pow, false);
    }

    private void loadMarker(ConfigurationSection section, String powerName) throws UnknownPowerException {
        NamespacedKey key = PowerManager.parseKey(powerName);
        Class<? extends Marker> marker = PowerManager.getMarker(key);
        if (marker == null) {
            plugin.getLogger().warning("Unknown marker:" + key + " on item " + this.name);
            throw new UnknownPowerException(key);
        }
        Marker pow = PowerManager.instantiate(marker);
        pow.setItem(this);
        pow.init(section);
        addMarker(key, pow, false);
    }

    @SuppressWarnings("rawtypes")
    private void loadTrigger(ConfigurationSection section, String triggerName) throws UnknownPowerException {
        String baseTrigger = section.getString("base");
        if (baseTrigger == null) {
            throw new IllegalArgumentException();
        }
        Trigger base = Trigger.get(baseTrigger);
        if (base == null) {
            plugin.getLogger().warning("Unknown base trigger:" + baseTrigger + " on item " + this.name);
            throw new UnknownPowerException(new NamespacedKey(RPGItems.plugin, baseTrigger));
        }

        Trigger newTrigger = base.copy(triggerName);

        newTrigger.setItem(this);
        newTrigger.init(section);
        triggers.put(triggerName, newTrigger);
    }


    @SuppressWarnings("rawtypes")
    public void save(ConfigurationSection s) {
        s.set("name", name);
        if (id != 0) {
            s.set("id", id);
        }
        s.set("uid", uid);

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
        s.set("attributemode", attributeMode.name());
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
        ConfigurationSection powerConfigs = s.createSection("powers");
        int i = 0;
        for (Power p : powers) {
            MemoryConfiguration pConfig = new MemoryConfiguration();
            pConfig.set("powerName", getPropertyHolderKey(p).toString());
            p.save(pConfig);
            powerConfigs.set(Integer.toString(i), pConfig);
            i++;
        }
        ConfigurationSection conditionConfigs = s.createSection("conditions");
        i = 0;
        for (Condition<?> p : conditions) {
            MemoryConfiguration pConfig = new MemoryConfiguration();
            pConfig.set("conditionName", p.getNamespacedKey().toString());
            p.save(pConfig);
            conditionConfigs.set(Integer.toString(i), pConfig);
            i++;
        }
        ConfigurationSection markerConfigs = s.createSection("markers");
        i = 0;
        for (Marker p : markers) {
            MemoryConfiguration pConfig = new MemoryConfiguration();
            pConfig.set("markerName", p.getNamespacedKey().toString());
            p.save(pConfig);
            markerConfigs.set(Integer.toString(i), pConfig);
            i++;
        }
        ConfigurationSection triggerConfigs = s.createSection("triggers");
        for (Entry<String, Trigger> p : triggers.entrySet()) {
            MemoryConfiguration pConfig = new MemoryConfiguration();
            p.getValue().save(pConfig);
            pConfig.set("base", p.getValue().getBase());
            triggerConfigs.set(p.getKey(), pConfig);
            i++;
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
    }

    public String getDamageType() {
        return this.damageType;
    }

    public void setDamageType(String damageType) {
        this.damageType = damageType;
    }

    public String getArmourExpression() {
        return armourExpression;
    }

    public void updateItem(ItemStack item) {
        updateItem(item, false);
    }

    public void updateItem(ItemStack item, boolean loreOnly) {
        List<String> reservedLores = this.filterLores(item);
        item.setType(getItem());
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>(getLore());
        PersistentDataContainer itemTagContainer = Objects.requireNonNull(meta).getPersistentDataContainer();
        SubItemTagContainer rpgitemsTagContainer = makeTag(itemTagContainer, TAG_META);
        set(rpgitemsTagContainer, TAG_ITEM_UID, getUid());
        addDurabilityBar(rpgitemsTagContainer, lore);
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(Color.fromRGB(getDataValue()));
        }
        Damageable damageable = (Damageable) meta;
        if (getMaxDurability() > 0) {
            int durability = computeIfAbsent(rpgitemsTagContainer, TAG_DURABILITY, PersistentDataType.INTEGER, this::getDefaultDurability);
            if (isCustomItemModel()) {
                damageable.setDamage(getDataValue());
            } else {
                damageable.setDamage((getItem().getMaxDurability() - ((short) ((double) getItem().getMaxDurability() * ((double) durability / (double) getMaxDurability())))));
            }
        } else {
            if (isCustomItemModel()) {
                damageable.setDamage(getDataValue());
            } else {
                damageable.setDamage(getItem().getMaxDurability() != 0 ? 0 : getDataValue());
            }
        }
        // Patch for mcMMO buff. See SkillUtils.java#removeAbilityBuff in mcMMO
        if (item.hasItemMeta() && Objects.requireNonNull(item.getItemMeta()).hasLore() && Objects.requireNonNull(item.getItemMeta().getLore()).contains("mcMMO Ability Tool"))
            lore.add("mcMMO Ability Tool");
        lore.addAll(reservedLores);
        meta.setLore(lore);

        if (loreOnly) {
            rpgitemsTagContainer.commit();
            item.setItemMeta(meta);
            return;
        }

        if (isCustomItemModel() || hasMarker(Unbreakable.class)) {
            meta.setUnbreakable(true);
        } else {
            meta.setUnbreakable(false);
        }
        meta.removeItemFlags(meta.getItemFlags().toArray(new ItemFlag[0]));

        for (ItemFlag flag : getItemFlags()) {
            meta.addItemFlags(flag);
        }
        if (getEnchantMode() == EnchantMode.DISALLOW) {
            Set<Enchantment> enchs = meta.getEnchants().keySet();
            for (Enchantment e : enchs) {
                meta.removeEnchant(e);
            }
        }
        Map<Enchantment, Integer> enchantMap = getEnchantMap();
        if (enchantMap != null) {
            for (Entry<Enchantment, Integer> e : enchantMap.entrySet()) {
                meta.addEnchant(e.getKey(), Math.max(meta.getEnchantLevel(e.getKey()), e.getValue()), true);
            }
        }
        checkAndMakeUnique(rpgitemsTagContainer);
        rpgitemsTagContainer.commit();
        item.setItemMeta(refreshAttributeModifiers(meta));
    }

    private final static NamespacedKey RGI_UNIQUE_MARK = new NamespacedKey(RPGItems.plugin, "RGI_UNIQUE_MARK");
    private final static NamespacedKey RGI_UNIQUE_ID = new NamespacedKey(RPGItems.plugin, "RGI_UNIQUE_ID");

    private void checkAndMakeUnique(SubItemTagContainer meta) {
        List<Unique> markers = getMarker(Unique.class);
        List<SlotCondition> conditions = getConditions(SlotCondition.class);

        if (!markers.isEmpty() ) {
            Unique unique = markers.get(0);
            if(unique.enabled){
                if (!meta.has(RGI_UNIQUE_MARK, PersistentDataType.BYTE)) {
                    meta.set(RGI_UNIQUE_MARK, PersistentDataType.BYTE, (byte) 0);
                }
                meta.set(RGI_UNIQUE_ID, PersistentDataType.STRING, UUID.randomUUID().toString());
            }else {
                meta.remove(RGI_UNIQUE_MARK);
                meta.remove(RGI_UNIQUE_ID);
            }
        }
        if(!conditions.isEmpty()){
            if (!meta.has(RGI_UNIQUE_MARK, PersistentDataType.BYTE)) {
                meta.set(RGI_UNIQUE_MARK, PersistentDataType.BYTE, (byte) 0);
            }
            meta.set(RGI_UNIQUE_ID, PersistentDataType.STRING, UUID.randomUUID().toString());
        }
    }

    private void addDurabilityBar(PersistentDataContainer meta, List<String> lore) {
        int maxDurability = getMaxDurability();
        if (maxDurability > 0) {
            int durability = computeIfAbsent(meta, TAG_DURABILITY, PersistentDataType.INTEGER, this::getDefaultDurability);
            if (isHasDurabilityBar()) {
                StringBuilder out = new StringBuilder();
                char boxChar = '\u25A0';
                double ratio = (double) durability / (double) maxDurability;
                BarFormat barFormat = getBarFormat();
                switch (barFormat) {
                    case NUMERIC_BIN:
                    case NUMERIC_BIN_MINUS_ONE:
                    case NUMERIC_HEX:
                    case NUMERIC_HEX_MINUS_ONE:
                    case NUMERIC:
                    case NUMERIC_MINUS_ONE: {
                        out.append(ChatColor.GREEN.toString()).append(boxChar).append(" ");
                        out.append(ratio < 0.1 ? ChatColor.RED : ratio < 0.3 ? ChatColor.YELLOW : ChatColor.GREEN);
                        out.append(formatBar(durability, maxDurability, barFormat));
                        out.append(ChatColor.RESET).append(" / ").append(ChatColor.AQUA);
                        out.append(formatBar(maxDurability, maxDurability, barFormat));
                        out.append(ChatColor.GREEN).append(boxChar);
                        break;
                    }
                    case DEFAULT: {
                        int boxCount = tooltipWidth / 7;
                        int mid = (int) ((double) boxCount * (ratio));
                        for (int i = 0; i < boxCount; i++) {
                            out.append(i < mid ? ChatColor.GREEN : i == mid ? ChatColor.YELLOW : ChatColor.RED);
                            out.append(boxChar);
                        }
                        break;
                    }
                }
                if (lore.isEmpty() || !lore.get(lore.size() - 1).contains(boxChar + ""))
                    lore.add(out.toString());
                else
                    lore.set(lore.size() - 1, out.toString());
            }
        }
    }

    private String formatBar(int durability, int maxDurability, BarFormat barFormat) {
        switch (barFormat) {
            case NUMERIC:
                return String.valueOf(durability);
            case NUMERIC_MINUS_ONE:
                return String.valueOf(durability - 1);
            case NUMERIC_HEX:
                int hexLen = String.format("%X", maxDurability).length();
                return String.format(String.format("0x%%0%dX", hexLen), durability);
            case NUMERIC_HEX_MINUS_ONE:
                int hexLenM1 = String.format("%X", maxDurability - 1).length();
                return String.format(String.format("0x%%0%dX", hexLenM1), durability - 1);
            case NUMERIC_BIN:
                int binLen = Integer.toBinaryString(maxDurability).length();
                return String.format(String.format("0b%%%ds", binLen), Integer.toBinaryString(durability)).replace(' ', '0');
            case NUMERIC_BIN_MINUS_ONE:
                int binLenM1 = Integer.toBinaryString(maxDurability - 1).length();
                return String.format(String.format("0b%%%ds", binLenM1), Integer.toBinaryString(durability - 1)).replace(' ', '0');

        }
        throw new UnsupportedOperationException();
    }

    private List<String> filterLores(ItemStack i) {
        List<String> ret = new ArrayList<>();
        List<LoreFilter> patterns = getMarker(LoreFilter.class).stream()
                                                               .filter(p -> !Strings.isNullOrEmpty(p.regex))
                                                               .map(LoreFilter::compile)
                                                               .collect(Collectors.toList());
        if (patterns.isEmpty()) return Collections.emptyList();
        if (!i.hasItemMeta() || !Objects.requireNonNull(i.getItemMeta()).hasLore()) return Collections.emptyList();
        for (String str : Objects.requireNonNull(i.getItemMeta().getLore())) {
            for (LoreFilter p : patterns) {
                Matcher matcher = p.pattern().matcher(ChatColor.stripColor(str));
                if (p.find ? matcher.find() : matcher.matches()) {
                    ret.add(str);
                    break;
                }
            }
        }
        return ret;
    }

    private ItemMeta refreshAttributeModifiers(ItemMeta itemMeta) {
        List<AttributeModifier> attributeModifiers = getMarker(AttributeModifier.class);
        Multimap<Attribute, org.bukkit.attribute.AttributeModifier> old = itemMeta.getAttributeModifiers();
        if (attributeMode.equals(AttributeMode.FULL_UPDATE)) {
            if (old != null && !old.isEmpty()) {
                old.forEach(itemMeta::removeAttributeModifier);
            }
        }
        if (!attributeModifiers.isEmpty()) {
            for (AttributeModifier attributeModifier : attributeModifiers) {
                Attribute attribute = attributeModifier.attribute;
                UUID uuid = new UUID(attributeModifier.uuidMost, attributeModifier.uuidLeast);
                org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                        uuid,
                        attributeModifier.name,
                        attributeModifier.amount,
                        attributeModifier.operation,
                        attributeModifier.slot
                );
                if (old != null) {
                    old.entries().stream().filter(m -> m.getValue().getUniqueId().equals(uuid)).findAny().ifPresent(
                            e -> itemMeta.removeAttributeModifier(e.getKey(), e.getValue())
                    );
                }
                itemMeta.addAttributeModifier(attribute, modifier);
            }
        }
        return itemMeta;
    }

    public boolean canDoMeleeTo(ItemStack item, Entity entity) {
        if (hasMarker(RangedOnly.class)) {
            return false;
        }
        if (item.getType() == Material.BOW || item.getType() == Material.SNOWBALL || item.getType() == Material.EGG || item.getType() == Material.POTION) {
            return isAlwaysAllowMelee();
        }
        return true;
    }

    public boolean canDoProjectileTo(ItemStack item, double distance, Entity entity) {
        List<Ranged> ranged = getMarker(Ranged.class, true);
        if (!ranged.isEmpty()) {
            return !(ranged.get(0).rm > distance) && !(distance > ranged.get(0).r);
        }
        return true;
    }

    /**
     * Event-type independent melee damage event
     *
     * @param p            Player who launched the damager
     * @param originDamage Origin damage value
     * @param stack        ItemStack of this item
     * @param entity       Victim of this damage event
     * @return Final damage or -1 if should cancel this event
     */
    public double meleeDamage(Player p, double originDamage, ItemStack stack, Entity entity) {
        double damage = originDamage;
        if (!canDoMeleeTo(stack, entity) || ItemManager.canUse(p, this) == Event.Result.DENY) {
            return -1;
        }
        boolean can = consumeDurability(stack, getHittingCost());
        if (!can) {
            return -1;
        }
        switch (getDamageMode()) {
            case MULTIPLY:
            case FIXED:
            case ADDITIONAL:
                damage = getDamageMin() != getDamageMax() ? (getDamageMin() + ThreadLocalRandom.current().nextInt(getDamageMax() - getDamageMin() + 1)) : getDamageMin();

                if (getDamageMode() == DamageMode.MULTIPLY) {
                    damage *= originDamage;
                    break;
                }

                Collection<PotionEffect> potionEffects = p.getActivePotionEffects();
                double strength = 0, weak = 0;
                for (PotionEffect pe : potionEffects) {
                    if (pe.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
                        strength = 3 * (pe.getAmplifier() + 1);//MC 1.9+
                    }
                    if (pe.getType().equals(PotionEffectType.WEAKNESS)) {
                        weak = 4 * (pe.getAmplifier() + 1);//MC 1.9+
                    }
                }
                damage = damage + strength - weak;

                if (getDamageMode() == DamageMode.ADDITIONAL) {
                    damage += originDamage;
                }
                if (damage < 0) damage = 0;
                break;
            case VANILLA:
                //no-op
                break;
        }
        return damage;
    }

    /**
     * Event-type independent projectile damage event
     *
     * @param p            Player who launched the damager
     * @param originDamage Origin damage value
     * @param stack        ItemStack of this item
     * @param damager      Projectile of this damage event
     * @param entity       Victim of this damage event
     * @return Final damage or -1 if should cancel this event
     */
    public double projectileDamage(Player p, double originDamage, ItemStack stack, Entity damager, Entity entity) {
        double damage = originDamage;
        if (ItemManager.canUse(p, this) == Event.Result.DENY) {
            return -1;
        }

        double distance = p.getLocation().distance(entity.getLocation());
        if (!canDoProjectileTo(stack, distance, entity)) {
            return -1;
        }

        switch (getDamageMode()) {
            case FIXED:
            case ADDITIONAL:
            case MULTIPLY:
                damage = getDamageMin() != getDamageMax() ? (getDamageMin() + ThreadLocalRandom.current().nextInt(getDamageMax() - getDamageMin() + 1)) : getDamageMin();

                if (getDamageMode() == DamageMode.MULTIPLY) {
                    damage *= originDamage;
                    break;
                }

                //Apply force adjustments
                if (damager.hasMetadata("RPGItems.Force")) {
                    damage *= damager.getMetadata("RPGItems.Force").get(0).asFloat();
                }
                if (getDamageMode() == DamageMode.ADDITIONAL) {
                    damage += originDamage;
                }
                break;
            case VANILLA:
                //no-op
                break;
        }
        return damage;
    }

    /**
     * Event-type independent take damage event
     *
     * @param p            Player taking damage
     * @param originDamage Origin damage value
     * @param stack        ItemStack of this item
     * @param damager      Cause of this damage. May be null
     * @return Final damage or -1 if should cancel this event
     */
    public double takeDamage(Player p, double originDamage, ItemStack stack, Entity damager) {
        if (ItemManager.canUse(p, this) == Event.Result.DENY) {
            return originDamage;
        }
        boolean can;
        if (!isHitCostByDamage()) {
            can = consumeDurability(stack, getHitCost());
        } else {
            can = consumeDurability(stack, (int) (getHitCost() * originDamage / 100d));
        }
        if (can && getArmour() > 0) {
            originDamage -= Math.round(originDamage * (((double) getArmour()) / 100d));
        }
        return originDamage;
    }

    /**
     * Event-type independent take damage event
     *
     * @param p     Player taking damage
     * @param stack ItemStack of this item
     * @param block Block
     * @return If should process this event
     */
    public boolean breakBlock(Player p, ItemStack stack, Block block) {
        return consumeDurability(stack, getBlockBreakingCost());
    }

    public static List<Modifier> getModifiers(ItemStack stack) {
        SubItemTagContainer tag = makeTag(Objects.requireNonNull(stack.getItemMeta()).getPersistentDataContainer(), TAG_MODIFIER);
        return getModifiers(tag);
    }

    public static List<Modifier> getModifiers(Player player) {
        SubItemTagContainer tag = makeTag(player.getPersistentDataContainer(), TAG_MODIFIER);
        return getModifiers(tag);
    }

    private static List<Modifier> getModifiers(SubItemTagContainer tag) {
        Optional<UUID> uuid = optUUID(tag, TAG_VERSION);
        if (!uuid.isPresent()) {
            uuid = Optional.of(UUID.randomUUID());
            set(tag, TAG_VERSION, uuid.get());
        }

        try {
            return modifierCache.get(uuid.get(), () -> getModifiersUncached(tag));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            tag.tryDispose();
        }
    }

    private static List<Modifier> getModifiersUncached(SubItemTagContainer tag) {
        List<Modifier> ret = new ArrayList<>();
        int i = 0;
        try {
            for (NamespacedKey key = PowerManager.parseKey(String.valueOf(i)); tag.has(key, PersistentDataType.TAG_CONTAINER); key = PowerManager.parseKey(String.valueOf(++i))) {
                PersistentDataContainer container = getTag(tag, key);
                String modifierName = getString(container, "modifier_name");
                Class<? extends Modifier> modifierClass = PowerManager.getModifier(PowerManager.parseKey(modifierName));
                Modifier modifier = PowerManager.instantiate(modifierClass);
                modifier.init(container);
                ret.add(modifier);
            }
            return ret;
        } finally {
            tag.commit();
        }
    }

    private <TEvent extends Event, TPower extends Pimpl, TResult, TReturn> boolean triggerPreCheck(Player player, ItemStack i, TEvent event, Trigger<TEvent, TPower, TResult, TReturn> trigger, List<TPower> powers) {
        if (i.getType().equals(Material.AIR)) return false;
        if (powers.isEmpty()) return false;
        if (checkPermission(player, true) == Event.Result.DENY) return false;

        RPGItemsPowersPreFireEvent<TEvent, TPower, TResult, TReturn> preFire = new RPGItemsPowersPreFireEvent<>(player, i, event, this, trigger, powers);
        Bukkit.getServer().getPluginManager().callEvent(preFire);
        return !preFire.isCancelled();
    }

    private <T> PowerResult<T> checkConditions(Player player, ItemStack i, Pimpl pimpl, List<Condition<?>> conds, Map<PropertyHolder, PowerResult<?>> context) {
        Set<String> ids = pimpl.getPower().getConditions();
        List<Condition<?>> conditions = conds.stream().filter(p -> ids.contains(p.id())).collect(Collectors.toList());
        List<Condition<?>> failed = conditions.stream().filter(p -> p.isStatic() ? !context.get(p).isOK() : !p.check(player, i, context).isOK()).collect(Collectors.toList());
        if (failed.isEmpty()) return null;
        return failed.stream().anyMatch(Condition::isCritical) ? PowerResult.abort() : PowerResult.condition();
    }

    private Map<Condition<?>, PowerResult<?>> checkStaticCondition(Player player, ItemStack i, List<Condition<?>> conds) {
        Set<String> ids = powers.stream().flatMap(p -> p.getConditions().stream()).collect(Collectors.toSet());
        List<Condition<?>> statics = conds.stream().filter(Condition::isStatic).filter(p -> ids.contains(p.id())).collect(Collectors.toList());
        Map<Condition<?>, PowerResult<?>> result = new LinkedHashMap<>();
        for (Condition<?> c : statics) {
            result.put(c, c.check(player, i, Collections.unmodifiableMap(result)));
        }
        return result;
    }

    public <TEvent extends Event, TPower extends Pimpl, TResult, TReturn> TReturn power(Player player, ItemStack i, TEvent event, Trigger<TEvent, TPower, TResult, TReturn> trigger, Object context) {
        powerCustomTrigger(player, i, event, trigger, context);

        List<TPower> powers = this.getPower(trigger, player, i);
        TReturn ret = trigger.def(player, i, event);
        if (!triggerPreCheck(player, i, event, trigger, powers)) return ret;
        try {
            List<Condition<?>> conds = getConditions();
            Map<Condition<?>, PowerResult<?>> staticCond = checkStaticCondition(player, i, conds);
            Map<PropertyHolder, PowerResult<?>> resultMap = new LinkedHashMap<>(staticCond);
            for (TPower power : powers) {
                PowerResult<TResult> result = checkConditions(player, i, power, conds, resultMap);
                if (result != null) {
                    resultMap.put(power.getPower(), result);
                } else {
                    if (power.getPower().requiredContext() != null) {
                        result = handleContext(player, i, event, trigger, power);
                    } else {
                        result = trigger.run(power, player, i, event, context);
                    }
                    resultMap.put(power.getPower(), result);
                }
                ret = trigger.next(ret, result);
                if (result.isAbort()) break;
            }
            triggerPostFire(player, i, event, trigger, resultMap, ret);
            return ret;
        } finally {
            Context.instance().cleanTemp(player.getUniqueId());
        }
    }

    @SuppressWarnings("unchecked")
    public <TEvent extends Event, TPower extends Pimpl, TResult, TReturn> void powerCustomTrigger(Player player, ItemStack i, TEvent event, Trigger<TEvent, TPower, TResult, TReturn> trigger, Object context) {
        this.triggers.entrySet()
                     .parallelStream()
                     .filter(e -> trigger.getClass().isInstance(e.getValue()))
                     .sorted(Comparator.comparing(en -> en.getValue().getPriority()))
                     .filter(e -> e.getValue().check(player, i, event)).forEach(e -> this.power(player, i, event, e.getValue(), context));
    }

    public <TEvent extends Event, TPower extends Pimpl, TResult, TReturn> TReturn power(Player player, ItemStack i, TEvent event, Trigger<TEvent, TPower, TResult, TReturn> trigger) {
        return power(player, i, event, trigger, null);
    }

    public <TEvent extends Event, TPower extends Pimpl, TResult, TReturn> PowerResult<TResult> handleContext(Player player, ItemStack i, TEvent event, Trigger<TEvent, TPower, TResult, TReturn> trigger, TPower power) {
        PowerResult<TResult> result;
        String contextKey = power.getPower().requiredContext();
        Object context = Context.instance().get(player.getUniqueId(), contextKey);
        if (context == null) {
            return PowerResult.context();
        }
        if (context instanceof Location) {
            if (power instanceof PowerLocation) {
                PowerResult<Void> overrideResult = BaseTriggers.LOCATION.run((PowerLocation) power, player, i, event, context);
                result = trigger.warpResult(overrideResult, power, player, i, event);
            } else {
                throw new IllegalStateException();
            }
        } else if (context instanceof Pair) {
            Object key = ((Pair) context).getKey();
            if (key instanceof LivingEntity) {
                PowerResult<Void> overrideResult = BaseTriggers.LIVINGENTITY.run((PowerLivingEntity) power, player, i, event, context);
                result = trigger.warpResult(overrideResult, power, player, i, event);
            } else {
                throw new IllegalStateException();
            }
        } else {
            throw new IllegalStateException();
        }
        return result;
    }

    private <TEvent extends Event, TPower extends Pimpl, TResult, TReturn> void triggerPostFire(Player player, ItemStack itemStack, TEvent event, Trigger<TEvent, TPower, TResult, TReturn> trigger, Map<PropertyHolder, PowerResult<?>> resultMap, TReturn ret) {
        RPGItemsPowersPostFireEvent<TEvent, TPower, TResult, TReturn> postFire = new RPGItemsPowersPostFireEvent<>(player, itemStack, event, this, trigger, resultMap, ret);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getItemStackDurability(itemStack).map(d -> d <= 0).orElse(false)) {
            itemStack.setAmount(0);
            itemStack.setType(Material.AIR);
        }
    }

    public void rebuild() {
        List<String> lines = getTooltipLines();
        lines.remove(0);
        setLore(lines);
    }

    @SuppressWarnings("deprecation")
    public List<String> getTooltipLines() {
        ArrayList<String> output = new ArrayList<>();
        output.add(getDisplayName());

        // add powerLores
        if (isShowPowerText()) {
            for (Power p : getPowers()) {
                String txt = p.displayText();
                if (txt != null && txt.length() > 0) {
                    output.add(txt);
                }
            }
        }

        // add descriptions
        output.addAll(getDescription());

        // compute width
        int width = 0;
        for (String str : output) {
            width = Math.max(width, Utils.getStringWidth(ChatColor.stripColor(str)));
        }

        // compute armorMinLen
        int armorMinLen = 0;
        String damageStr = null;
        if (isShowArmourLore()) {
            if (getArmour() != 0) {
                damageStr = getArmour() + "% " + I18n.formatDefault("item.armour");
            }
            if ((getDamageMin() != 0 || getDamageMax() != 0) && getDamageMode() != DamageMode.VANILLA) {
                damageStr = damageStr == null ? "" : damageStr + " & ";
                if (getDamageMode() == DamageMode.ADDITIONAL) {
                    damageStr += I18n.formatDefault("item.additionaldamage", getDamageMin() == getDamageMax() ? String.valueOf(getDamageMin()) : getDamageMin() + "-" + getDamageMax());
                } else if (getDamageMode() == DamageMode.MULTIPLY) {
                    damageStr += I18n.formatDefault("item.multiplydamage", getDamageMin() == getDamageMax() ? String.valueOf(getDamageMin()) : getDamageMin() + "-" + getDamageMax());
                } else {
                    damageStr += I18n.formatDefault("item.damage", getDamageMin() == getDamageMax() ? String.valueOf(getDamageMin()) : getDamageMin() + "-" + getDamageMax());
                }
            }
            if (damageStr != null) {
                armorMinLen = Math.max(armorMinLen, Utils.getStringWidth(ChatColor.stripColor(damageStr)));
            }
        }
        tooltipWidth = width = Math.max(width, armorMinLen);

        if (isShowArmourLore()) {
            if (damageStr != null) {
                output.add(1, ChatColor.WHITE + damageStr);
            }
        }

        return output;
    }

    public ItemStack toItemStack() {
        ItemStack rStack = new ItemStack(getItem());
        ItemMeta meta = rStack.getItemMeta();
        PersistentDataContainer itemTagContainer = Objects.requireNonNull(meta).getPersistentDataContainer();
        SubItemTagContainer rpgitemsTagContainer = makeTag(itemTagContainer, TAG_META);
        set(rpgitemsTagContainer, TAG_ITEM_UID, getUid());
        if (isHasStackId()) {
            set(rpgitemsTagContainer, TAG_STACK_ID, UUID.randomUUID());
        }
        rpgitemsTagContainer.commit();
        meta.setDisplayName(getDisplayName());
        rStack.setItemMeta(meta);

        updateItem(rStack, false);
        return rStack;
    }

    public void toModel(ItemStack itemStack) {
        updateItem(itemStack);
        ItemMeta itemMeta = itemStack.getItemMeta();
        SubItemTagContainer meta = makeTag(Objects.requireNonNull(itemMeta).getPersistentDataContainer(), TAG_META);
        meta.remove(TAG_OWNER);
        meta.remove(TAG_STACK_ID);
        set(meta, TAG_IS_MODEL, true);
        meta.commit();
        itemMeta.setDisplayName(getDisplayName());
        itemStack.setItemMeta(itemMeta);
    }

    public void unModel(ItemStack itemStack, Player owner) {
        updateItem(itemStack);
        ItemMeta itemMeta = itemStack.getItemMeta();
        SubItemTagContainer meta = makeTag(Objects.requireNonNull(itemMeta).getPersistentDataContainer(), TAG_META);
        if (isCanBeOwned()) {
            set(meta, TAG_OWNER, owner);
        }
        if (isHasStackId()) {
            set(meta, TAG_STACK_ID, UUID.randomUUID());
        }
        meta.remove(TAG_IS_MODEL);
        meta.commit();
        itemMeta.setDisplayName(getDisplayName());
        itemStack.setItemMeta(itemMeta);
    }

    public Event.Result checkPermission(Player p, boolean showWarn) {
        if (isHasPermission() && !p.hasPermission(getPermission())) {
            if (showWarn)
                p.sendMessage(I18n.getInstance(p.getLocale()).format("message.error.permission", getDisplayName()));
            return Event.Result.DENY;
        }
        return Event.Result.ALLOW;
    }

    public void print(CommandSender sender) {
        print(sender, true);
    }

    public void print(CommandSender sender, boolean advance) {
        String author = this.getAuthor();
        BaseComponent authorComponent = new TextComponent(author);
        try {
            UUID uuid = UUID.fromString(this.getAuthor());
            OfflinePlayer authorPlayer = Bukkit.getOfflinePlayer(uuid);
            author = authorPlayer.getName();
            authorComponent = AdminCommands.getAuthorComponent(authorPlayer, author);
        } catch (IllegalArgumentException ignored) {
        }

        String locale = RPGItems.plugin.cfg.language;
        if (sender instanceof Player) {
            locale = ((Player) sender).getLocale();
            new Message("")
                    .append(I18n.getInstance(((Player) sender).getLocale()).format("message.item.print"), toItemStack())
                    .send(sender);
        } else {
            List<String> lines = getTooltipLines();
            for (String line : lines) {
                sender.sendMessage(line);
            }
        }
        I18n i18n = I18n.getInstance(locale);

        new Message("").append(I18n.formatDefault("message.print.author"), Collections.singletonMap("{author}", authorComponent)).send(sender);
        if (!advance) {
            return;
        }

        new Message(I18n.formatDefault("message.print.license", getLicense())).send(sender);
        new Message(I18n.formatDefault("message.print.note", getNote())).send(sender);

        sender.sendMessage(I18n.formatDefault("message.durability.info", getMaxDurability(), getDefaultDurability(), getDurabilityLowerBound(), getDurabilityUpperBound()));
        if (isCustomItemModel()) {
            sender.sendMessage(I18n.formatDefault("message.print.customitemmodel", getItem().name() + ":" + getDataValue()));
        }
        if (!getItemFlags().isEmpty()) {
            StringBuilder str = new StringBuilder();
            for (ItemFlag flag : getItemFlags()) {
                if (str.length() > 0) {
                    str.append(", ");
                }
                str.append(flag.name());
            }
            sender.sendMessage(I18n.formatDefault("message.print.itemflags") + str);
        }
    }

    public void setItemStackDurability(ItemStack item, int val) {
        ItemMeta itemMeta = item.getItemMeta();
        SubItemTagContainer tagContainer = makeTag(Objects.requireNonNull(itemMeta), TAG_META);
        if (getMaxDurability() != -1) {
            set(tagContainer, TAG_DURABILITY, val);
        }
        tagContainer.commit();
        item.setItemMeta(itemMeta);
        this.updateItem(item, true);
    }

    public Optional<Integer> getItemStackDurability(ItemStack item) {
        if (getMaxDurability() == -1) {
            return Optional.empty();
        }
        ItemMeta itemMeta = item.getItemMeta();
        //Power Consume will make this null in triggerPostFire().
        if(itemMeta == null){
            return Optional.empty();
        }
        SubItemTagContainer tagContainer = makeTag(itemMeta, TAG_META);
        int durability = computeIfAbsent(tagContainer, TAG_DURABILITY, PersistentDataType.INTEGER, this::getDefaultDurability);
        tagContainer.commit();
        item.setItemMeta(itemMeta);
        return Optional.of(durability);
    }

    public boolean consumeDurability(ItemStack item, int val) {
        return consumeDurability(item, val, true);
    }

    public boolean consumeDurability(ItemStack item, int val, boolean checkbound) {
        if (val == 0) return true;
        int durability;
        ItemMeta itemMeta = item.getItemMeta();
        if (getMaxDurability() != -1) {
            SubItemTagContainer tagContainer = makeTag(Objects.requireNonNull(itemMeta), TAG_META);
            durability = computeIfAbsent(tagContainer, TAG_DURABILITY, PersistentDataType.INTEGER, this::getDefaultDurability);
            if (checkbound && (
                    (val > 0 && durability < getDurabilityLowerBound()) ||
                            (val < 0 && durability > getDurabilityUpperBound())
            )) {
                tagContainer.commit();
                item.setItemMeta(itemMeta);
                return false;
            }
            if (durability <= val
                        && hasMarker(Unbreakable.class)
                        && !isCustomItemModel()) {
                tagContainer.commit();
                item.setItemMeta(itemMeta);
                return false;
            }
            durability -= val;
            if (durability > getMaxDurability()) {
                durability = getMaxDurability();
            }
            set(tagContainer, TAG_DURABILITY, durability);
            tagContainer.commit();
            item.setItemMeta(itemMeta);
            this.updateItem(item, true);
        }
        return true;
    }

    public void give(Player player, int count, boolean wear) {
        ItemStack itemStack = toItemStack();
        itemStack.setAmount(count);
        if (wear) {
            if (
                    item.equals(Material.CHAINMAIL_HELMET) ||
                            item.equals(Material.DIAMOND_HELMET) ||
                            item.equals(Material.GOLDEN_HELMET) ||
                            item.equals(Material.IRON_HELMET) ||
                            item.equals(Material.LEATHER_HELMET) ||
                            item.equals(Material.TURTLE_HELMET)
            ) {
                if (player.getInventory().getHelmet() == null) {
                    player.getInventory().setHelmet(itemStack);
                    return;
                }
            } else if (
                           item.equals(Material.CHAINMAIL_CHESTPLATE) ||
                                   item.equals(Material.DIAMOND_CHESTPLATE) ||
                                   item.equals(Material.GOLDEN_CHESTPLATE) ||
                                   item.equals(Material.IRON_CHESTPLATE) ||
                                   item.equals(Material.LEATHER_CHESTPLATE)
            ) {
                if (player.getInventory().getChestplate() == null) {
                    player.getInventory().setChestplate(itemStack);
                    return;
                }
            } else if (
                           item.equals(Material.CHAINMAIL_LEGGINGS) ||
                                   item.equals(Material.DIAMOND_LEGGINGS) ||
                                   item.equals(Material.GOLDEN_LEGGINGS) ||
                                   item.equals(Material.IRON_LEGGINGS) ||
                                   item.equals(Material.LEATHER_LEGGINGS)
            ) {
                if (player.getInventory().getLeggings() == null) {
                    player.getInventory().setLeggings(itemStack);
                    return;
                }
            } else if (
                           item.equals(Material.CHAINMAIL_BOOTS) ||
                                   item.equals(Material.DIAMOND_BOOTS) ||
                                   item.equals(Material.GOLDEN_BOOTS) ||
                                   item.equals(Material.IRON_BOOTS) ||
                                   item.equals(Material.LEATHER_BOOTS)
            ) {
                if (player.getInventory().getBoots() == null) {
                    player.getInventory().setBoots(itemStack);
                    return;
                }
            }
        }
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(itemStack);
        for (ItemStack o : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), o);
        }
    }

    public boolean hasMarker(Class<? extends Marker> marker) {
        return markers.stream().anyMatch(p -> p.getClass().equals(marker));
    }

    public <T extends Marker> List<T> getMarker(Class<T> marker) {
        return markers.stream().filter(p -> p.getClass().equals(marker)).map(marker::cast).collect(Collectors.toList());
    }

    public <T extends Condition<?>> List<T> getConditions(Class<T> condition) {
        return conditions.stream().filter(p -> p.getClass().equals(condition)).map(condition::cast).collect(Collectors.toList());
    }

    public <T extends Marker> List<T> getMarker(Class<T> marker, boolean subclass) {
        return subclass ? markers.stream().filter(marker::isInstance).map(marker::cast).collect(Collectors.toList()) : getMarker(marker);
    }

    public <T extends Marker> List<T> getMarker(NamespacedKey key, Class<T> marker) {
        return markers.stream().filter(p -> p.getClass().equals(marker) && getPropertyHolderKey(p).equals(key)).map(marker::cast).collect(Collectors.toList());
    }

    public <T extends Power> List<T> getPower(NamespacedKey key, Class<T> power) {
        return powers.stream().filter(p -> p.getClass().equals(power) && getPropertyHolderKey(p).equals(key)).map(power::cast).collect(Collectors.toList());
    }

    public <T extends Condition<?>> List<T> getCondition(NamespacedKey key, Class<T> condition) {
        return powers.stream().filter(p -> p.getClass().equals(condition) && getPropertyHolderKey(p).equals(key)).map(condition::cast).collect(Collectors.toList());
    }

    public Condition<?> getCondition(String id) {
        return conditions.stream().filter(c -> c.id().equals(id)).findAny().orElse(null);
    }

    public void addPower(NamespacedKey key, Power power) {
        addPower(key, power, true);
    }

    private void addPower(NamespacedKey key, Power power, boolean update) {
        powers.add(power);
        keys.put(power, key);
        if (update) {
            rebuild();
        }
    }

    public void removePower(Power power) {
        powers.remove(power);
        keys.remove(power);
        power.deinit();
        rebuild();
    }

    public void addCondition(NamespacedKey key, Condition<?> condition) {
        addCondition(key, condition, true);
    }

    private void addCondition(NamespacedKey key, Condition<?> condition, boolean update) {
        conditions.add(condition);
        keys.put(condition, key);
        if (update) {
            rebuild();
        }
    }

    public void removeCondition(Condition<?> condition) {
        conditions.remove(condition);
        keys.remove(condition);
        rebuild();
    }

    public void addMarker(NamespacedKey key, Marker marker) {
        addMarker(key, marker, true);
    }

    private void addMarker(NamespacedKey key, Marker marker, boolean update) {
        markers.add(marker);
        keys.put(marker, key);
        if (update) {
            rebuild();
        }
    }

    public void removeMarker(Marker marker) {
        markers.remove(marker);
        keys.remove(marker);
        rebuild();
    }


    public void addDescription(String str) {
        getDescription().add(ChatColor.translateAlternateColorCodes('&', str));
        rebuild();
    }

    public void toggleBar() {
        setHasDurabilityBar(!isHasDurabilityBar());
        rebuild();
    }

    public BaseComponent getComponent(CommandSender sender) {
        String locale = RPGItems.plugin.cfg.language;
        if (sender instanceof Player) {
            locale = ((Player) sender).getLocale();
        }
        return getComponent(locale);
    }

    public BaseComponent getComponent(String locale) {
        BaseComponent msg = new TextComponent(getDisplayName());
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/rpgitem " + getName()));
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                new BaseComponent[]{new TextComponent(ItemStackUtils.itemToJson(toItemStack()))});
        msg.setHoverEvent(hover);
        return msg;
    }

    private <TEvent extends Event, T extends Pimpl, TResult, TReturn> List<T> getPower(Trigger<TEvent, T, TResult, TReturn> trigger, Player player, ItemStack stack) {
        return powers.stream()
                     .filter(p -> p.getTriggers().contains(trigger))
                     .map(p -> {
                         Class<? extends Power> cls = p.getClass();
                         Power proxy = DynamicMethodInterceptor.create(p, player, cls, stack, trigger);
                         return PowerManager.createImpl(cls, proxy);
                     })
                     .map(p -> p.cast(trigger.getPowerClass()))
                     .collect(Collectors.toList());
    }

    @SuppressWarnings("rawtypes")
    public static class DynamicMethodInterceptor implements MethodInterceptor {
        private static WeakHashMap<Player, WeakHashMap<ItemStack, WeakHashMap<Power, Power>>> cache = new WeakHashMap<>();

        private static Power makeProxy(Power orig, Player player, Class<? extends Power> cls, ItemStack stack, Trigger trigger) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(cls);
            enhancer.setInterfaces(new Class[]{trigger.getPowerClass()});
            enhancer.setCallback(new DynamicMethodInterceptor(orig, player));
            return (Power) enhancer.create();
        }

        protected static Power create(Power orig, Player player, Class<? extends Power> cls, ItemStack stack, Trigger trigger) {
            return cache.computeIfAbsent(player, (k) -> new WeakHashMap<>())
                        .computeIfAbsent(stack, (p) -> new WeakHashMap<>())
                        .computeIfAbsent(orig, (s) -> makeProxy(orig, player, cls, stack, trigger));
        }

        private final Power orig;
        private final Player player;
        private final Map<Method, PropertyInstance> getters;

        protected DynamicMethodInterceptor(Power orig, Player player) {
            this.orig = orig;
            this.player = player;
            this.getters = PowerManager.getProperties(orig.getClass())
                                       .entrySet()
                                       .stream()
                                       .collect(Collectors.toMap(e -> e.getValue().getKey(), e -> e.getValue().getValue()));
        }

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy)
                throws Throwable {
            if (getters.containsKey(method)) {
                PropertyInstance propertyInstance = getters.get(method);
                Class<?> type = propertyInstance.field().getType();
                // Numeric modifiers
                if (type == int.class || type == Integer.class || type == float.class || type == Float.class || type == double.class || type == Double.class) {
                    List<Modifier> playerModifiers = getModifiers(player);
                    @SuppressWarnings("unchecked") List<Modifier<Double>> numberModifiers = playerModifiers.stream().filter(m -> (m.getModifierTargetType() == Double.class) && m.match(orig, propertyInstance)).map(m -> (Modifier<Double>) m).collect(Collectors.toList());
                    Number value = (Number) methodProxy.invoke(orig, args);
                    double origValue = value.doubleValue();
                    for (Modifier<Double> numberModifier : numberModifiers) {
                        origValue = numberModifier.apply(origValue);
                    }
                    if (int.class.equals(type) || Integer.class.equals(type)) {
                        return (int) origValue;
                    } else if (float.class.equals(type) || Float.class.equals(type)) {
                        return (float) origValue;
                    } else {
                        return origValue;
                    }
                }
            }
            return methodProxy.invoke(orig, args);
        }
    }

    public void addTrigger(String name, Trigger trigger) {
        triggers.put(name, trigger);
    }

    public List<Condition<?>> getConditions() {
        return conditions;
    }

    public void deinit() {
        powers.forEach(Power::deinit);
    }

    public int getArmour() {
        return armour;
    }

    public boolean isAlwaysAllowMelee() {
        return alwaysAllowMelee;
    }

    public boolean isCanBeOwned() {
        return canBeOwned;
    }

    public boolean isHasStackId() {
        return hasStackId;
    }

    public void setAlwaysAllowMelee(boolean alwaysAllowMelee) {
        this.alwaysAllowMelee = alwaysAllowMelee;
    }

    public void setArmour(int a) {
        setArmour(a, true);
    }

    public void setArmour(int a, boolean update) {
        armour = a;
        if (update) {
            rebuild();
        }
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getBlockBreakingCost() {
        return blockBreakingCost;
    }

    public void setBlockBreakingCost(int blockBreakingCost) {
        this.blockBreakingCost = blockBreakingCost;
    }

    public int getDamageMax() {
        return damageMax;
    }

    public void setCanBeOwned(boolean canBeOwned) {
        this.canBeOwned = canBeOwned;
    }

    private void setDamageMax(int damageMax) {
        this.damageMax = damageMax;
    }

    public int getDamageMin() {
        return damageMin;
    }

    private void setDamageMin(int damageMin) {
        this.damageMin = damageMin;
    }

    public void setDamage(int min, int max) {
        setDamageMin(min);
        setDamageMax(max);
        rebuild();
    }

    public DamageMode getDamageMode() {
        return damageMode;
    }

    public void setDamageMode(DamageMode damageMode) {
        this.damageMode = damageMode;
    }

    public int getDataValue() {
        return dataValue;
    }

    public void setDataValue(int dataValue) {
        this.dataValue = dataValue;
    }

    public int getDefaultDurability() {
        return defaultDurability;
    }

    public void setDefaultDurability(int newVal) {
        defaultDurability = newVal;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = ChatColor.translateAlternateColorCodes('&', displayName);
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

    public void setDurabilityBound(int min, int max) {
        setDurabilityLowerBound(min);
        setDurabilityUpperBound(max);
    }

    public Map<Enchantment, Integer> getEnchantMap() {
        return enchantMap;
    }

    public void setEnchantMap(Map<Enchantment, Integer> enchantMap) {
        this.enchantMap = enchantMap;
    }

    public File getFile() {
        return file;
    }

    public EnchantMode getEnchantMode() {
        return enchantMode;
    }

    public void setEnchantMode(EnchantMode enchantMode) {
        this.enchantMode = enchantMode;
    }

    void setFile(File itemFile) {
        file = itemFile;
    }

    public int getHitCost() {
        return hitCost;
    }

    public void setHasStackId(boolean hasStackId) {
        this.hasStackId = hasStackId;
    }

    public void setHitCost(int hitCost) {
        this.hitCost = hitCost;
    }

    public int getHittingCost() {
        return hittingCost;
    }

    public void setHittingCost(int hittingCost) {
        this.hittingCost = hittingCost;
    }

    public Material getItem() {
        return item;
    }

    public void setItem(Material material) {
        item = material;
    }

    public List<ItemFlag> getItemFlags() {
        return itemFlags;
    }

    public void setItemFlags(List<ItemFlag> itemFlags) {
        this.itemFlags = itemFlags;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    private void setLore(List<String> lore) {
        this.lore = lore;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public void setMaxDurability(int newVal) {
        maxDurability = newVal <= 0 ? -1 : newVal;
        setDefaultDurability(maxDurability);
    }

    public String getMcVersion() {
        return mcVersion;
    }

    public void setMcVersion(String mcVersion) {
        this.mcVersion = mcVersion;
    }

    public String getName() {
        return name;
    }

    public NamespacedKey getNamespacedKey() {
        return namespacedKey;
    }

    public void setNamespacedKey(NamespacedKey namespacedKey) {
        this.namespacedKey = namespacedKey;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPermission() {
        return Strings.isNullOrEmpty(permission) ? "rpgitems.item.use." + getName() : permission;
    }

    public void setPermission(String p) {
        permission = p;
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

    public List<Power> getPowers() {
        return powers;
    }

    public List<Marker> getMarkers() {
        return markers;
    }

    public Map<String, Trigger> getTriggers() {
        return triggers;
    }

    public NamespacedKey getPropertyHolderKey(PropertyHolder power) {
        return Objects.requireNonNull(keys.get(power));
    }

    public NamespacedKey removePropertyHolderKey(PropertyHolder power) {
        return Objects.requireNonNull(keys.remove(power));
    }

    public int getUid() {
        return uid;
    }

    public boolean isCustomItemModel() {
        return customItemModel;
    }

    public void setCustomItemModel(boolean customItemModel) {
        this.customItemModel = customItemModel;
    }

    public boolean isHasDurabilityBar() {
        return hasDurabilityBar;
    }

    public void setHasDurabilityBar(boolean hasDurabilityBar) {
        this.hasDurabilityBar = hasDurabilityBar;
    }

    public boolean isHasPermission() {
        return hasPermission;
    }

    public void setHasPermission(boolean b) {
        hasPermission = b;
    }

    public boolean isHitCostByDamage() {
        return hitCostByDamage;
    }

    public void setHitCostByDamage(boolean hitCostByDamage) {
        this.hitCostByDamage = hitCostByDamage;
    }

    public boolean isIgnoreWorldGuard() {
        return ignoreWorldGuard;
    }

    public void setIgnoreWorldGuard(boolean ignoreWorldGuard) {
        this.ignoreWorldGuard = ignoreWorldGuard;
    }

    public BarFormat getBarFormat() {
        return barFormat;
    }

    public void setBarFormat(BarFormat barFormat) {
        this.barFormat = barFormat;
    }

    public boolean isShowArmourLore() {
        return showArmourLore;
    }

    public void setShowArmourLore(boolean showArmourLore) {
        this.showArmourLore = showArmourLore;
    }

    public boolean isShowPowerText() {
        return showPowerText;
    }

    public void setShowPowerText(boolean showPowerText) {
        this.showPowerText = showPowerText;
    }

    public void setAttributeMode(AttributeMode attributeMode) {
        this.attributeMode = attributeMode;
    }

    public AttributeMode getAttributeMode() {
        return attributeMode;
    }

    public enum DamageMode {
        FIXED,
        VANILLA,
        ADDITIONAL,
        MULTIPLY,
    }

    public enum EnchantMode {
        DISALLOW,
        PERMISSION,
        ALLOW
    }

    public enum AttributeMode {
        FULL_UPDATE, PARTIAL_UPDATE;
    }

    public enum BarFormat {
        DEFAULT,
        NUMERIC,
        NUMERIC_MINUS_ONE,
        NUMERIC_HEX,
        NUMERIC_HEX_MINUS_ONE,
        NUMERIC_BIN,
        NUMERIC_BIN_MINUS_ONE,
    }
}
