package think.rpgitems.item;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.utils.HexColorUtils;
import cat.nyaa.nyaacore.utils.ItemTagUtils;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Multimap;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.*;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.TagKey;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.banner.PatternType;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.trim.TrimMaterial;
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
import think.rpgitems.power.marker.Unbreakable;
import think.rpgitems.power.propertymodifier.Modifier;
import think.rpgitems.power.proxy.Interceptor;
import think.rpgitems.power.trigger.BaseTriggers;
import think.rpgitems.power.trigger.Trigger;
import think.rpgitems.support.PlaceholderAPISupport;
import think.rpgitems.utils.ComponentUtil;
import think.rpgitems.utils.MaterialUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.bukkit.ChatColor.COLOR_CHAR;
import static think.rpgitems.utils.ItemTagUtils.*;

public class RPGItem {
    public static final NamespacedKey TAG_META = new NamespacedKey(RPGItems.plugin, "meta");
    public static final NamespacedKey TAG_ITEM_UID = new NamespacedKey(RPGItems.plugin, "item_uid");
    public static final NamespacedKey TAG_IS_MODEL = new NamespacedKey(RPGItems.plugin, "is_model");
    public static final NamespacedKey TAG_DURABILITY = new NamespacedKey(RPGItems.plugin, "durability");
    public static final NamespacedKey TAG_OWNER = new NamespacedKey(RPGItems.plugin, "owner");
    public static final NamespacedKey TAG_STACK_ID = new NamespacedKey(RPGItems.plugin, "stack_id");
    public static final NamespacedKey TAG_MODIFIER = new NamespacedKey(RPGItems.plugin, "property_modifier");
    public static final NamespacedKey TAG_VERSION = new NamespacedKey(RPGItems.plugin, "version");
    public static final String DAMAGE_TYPE = "RGI_DAMAGE_TYPE";
    public static final String NBT_UID = "rpgitem_uid";
    public static final String NBT_ITEM_UUID = "rpgitem_item_uuid";
    public static final String NBT_IS_MODEL = "rpgitem_is_model";

    private static final Cache<UUID, List<Modifier>> modifierCache = CacheBuilder.newBuilder().concurrencyLevel(1).expireAfterAccess(1, TimeUnit.MINUTES).build();
    private final static NamespacedKey RGI_UNIQUE_MARK = new NamespacedKey(RPGItems.plugin, "RGI_UNIQUE_MARK");
    private final static NamespacedKey RGI_UNIQUE_ID = new NamespacedKey(RPGItems.plugin, "RGI_UNIQUE_ID");
    static RPGItems plugin;
    private boolean ignoreWorldGuard = false;
    private List<String> description = new ArrayList<>();
    private boolean showPowerText = true;
    private boolean showArmourLore = true;
    private Map<Enchantment, Integer> enchantMap = null;
    private List<ItemFlag> itemFlags = new ArrayList<>();
    private boolean customItemModel = false;
    private EnchantMode enchantMode = EnchantMode.DISALLOW;
    //Data Components
    private List<Map<DataComponentType, Object>> components = new ArrayList<>();
    // Powers
    private List<Power> powers = new ArrayList<>();
    private List<Condition<?>> conditions = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();
    private final Map<String, PlaceholderHolder> placeholders = new HashMap<>();
    @SuppressWarnings("rawtypes")
    private final Map<String, Trigger> triggers = new HashMap<>();
    private final HashMap<PropertyHolder, NamespacedKey> keys = new HashMap<>();
    private File file;
    private NamespacedKey namespacedKey;
    private Material item;
    private int dataValue;
    private int id;
    private int uid;
    private final String name;
    private boolean hasPermission;
    private String permission;
    private String displayName;
    private int damageMin = 0;
    private int damageMax = 3;
    private DamageMode damageMode = DamageMode.FIXED;
    private UpdateMode updateMode = UpdateMode.FULL_UPDATE;
    private AttributeMode attributeMode = AttributeMode.PARTIAL_UPDATE;
    private int armour = 0;
    private String armourExpression = "";
    private String playerArmourExpression = "";
    private String damageType = "";
    private boolean canBeOwned = false;
    private boolean canUse = false;
    private boolean canPlace = false;
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
    //general
    private String mcVersion;
    private int pluginVersion;
    private int pluginSerial;
    private List<String> lore;
    private CustomModelData.Builder customModelData;
    private boolean isTemplate;
    private Set<String> templates = new HashSet<>();
    private final Set<String> templatePlaceholders = new HashSet<>();
    private String quality;
    private String type = "item";
    private NamespacedKey itemModel;

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

    public static void updateItemStack(ItemStack item, @Nullable Player player) {
        Optional<RPGItem> rItem = ItemManager.toRPGItem(item);
        rItem.ifPresent(r -> r.updateItem(item, false, player));
    }

    public static List<Modifier> getModifiers(ItemStack stack) {
        Optional<String> opt = ItemTagUtils.getString(stack, NBT_ITEM_UUID);
        if (opt.isEmpty()) {
            // Items without UUID have no modifiers - UUID should be set by PlayerRPGInventoryCache
            // Avoid calling updateItem() here as it's too expensive for the tick path
            return Collections.emptyList();
        }

        UUID key = UUID.fromString(opt.get());
        List<Modifier> modifiers = modifierCache.getIfPresent(key);
        if (modifiers == null) {
            ItemMeta itemMeta = stack.getItemMeta();
            if (itemMeta == null) return new ArrayList<>();
            SubItemTagContainer tag = makeTag(Objects.requireNonNull(itemMeta).getPersistentDataContainer(), TAG_MODIFIER);
            modifiers = getModifiers(tag, key);
        }
        return modifiers;
    }

    public static List<Modifier> getModifiers(Player player) {
        UUID key = player.getUniqueId();
        List<Modifier> modifiers = modifierCache.getIfPresent(key);
        if (modifiers == null) {
            SubItemTagContainer tag = makeTag(player.getPersistentDataContainer(), TAG_MODIFIER);
            modifiers = getModifiers(tag, key);
        }
        return modifiers;
    }

    public static List<Modifier> getModifiers(SubItemTagContainer tag) {
        return getModifiers(tag, null);
    }

    public static void invalidateModifierCache() {
        modifierCache.invalidateAll();
    }

    public boolean isCanPlace() {
        return this.canPlace;
    }

    public boolean isCanUse() {
        return this.canUse;
    }

    public void setCanUse(boolean canUse) {
        this.canUse = canUse;
    }

    public void setCanPlace(boolean canPlace) {
        this.canPlace = canPlace;
    }

    public static List<Modifier> getModifiers(SubItemTagContainer tag, UUID key) {
        Optional<UUID> uuid = Optional.ofNullable(key);
        if (!uuid.isPresent()) {
            uuid = Optional.of(UUID.randomUUID());
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
        desc.replaceAll(HexColorUtils::hexColored);
        setDescription(desc);
        setDamageMin(s.getInt("damageMin"));
        setDamageMax(s.getInt("damageMax"));
        setArmour(s.getInt("armour", 0), false);
        setArmourExpression(s.getString("armourExpression", ""));
        setPlayerArmourExpression(s.getString("playerArmourExpression", ""));
        setDamageType(s.getString("DamageType", ""));
        setUpdateMode(UpdateMode.valueOf(s.getString("updatemode", "FULL_UPDATE")));
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
        setCanPlace(s.getBoolean("canPlace", false));
        setCanUse(s.getBoolean("canUse", false));
        setHasStackId(s.getBoolean("hasStackId", false));
        placeholders.clear();
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

        Map<String, List<PlaceholderHolder>> duplicatePlaceholderIds = checkDuplicatePlaceholderIds();
        if (!duplicatePlaceholderIds.isEmpty()) {
            Logger logger = RPGItems.plugin.getLogger();
            String duplicateMsg = getDuplicatePlaceholderMsg(duplicatePlaceholderIds);
            logger.log(Level.WARNING, duplicateMsg);
        }
        setHasPermission(s.getBoolean("haspermission", false));
        setPermission(s.getString("permission", "rpgitem.item." + name));
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
            setHasDurabilityBar(getItem().getMaxDurability() == 0 || s.getBoolean("forceBar"));
        }
        setHasDurabilityBar(s.getBoolean("hasDurabilityBar", isHasDurabilityBar()));

        setShowPowerText(s.getBoolean("showPowerText", true));
        setShowArmourLore(s.getBoolean("showArmourLore", true));
        if (s.isConfigurationSection("customModelData")) {
            ConfigurationSection modelSection = s.getConfigurationSection("customModelData");
            CustomModelData.Builder customData = CustomModelData.customModelData();
            if (s.isInt("customModelData")) {
                customData.addFloat(s.getInt("customModelData"));
                setCustomModelData(customData);
            } else {
                for (String sectionKey : modelSection.getKeys(false)) {
                    switch (sectionKey) {
                        case "floats":
                            if (modelSection.isList("floats")) {
                                for (double value : modelSection.getDoubleList("floats")) {
                                    customData.addFloat((float) value);
                                }
                            }
                            break;
                        case "strings":
                            if (modelSection.isList("strings")) {
                                for (String value : modelSection.getStringList("strings")) {
                                    customData.addString(value);
                                }
                            }
                            break;
                        case "colors":
                            if (modelSection.isList("colors")) {
                                for (String value : modelSection.getStringList("colors")) {
                                    String[] parts = value.split(",");
                                    if (parts.length == 3) {
                                        int r = Integer.parseInt(parts[0]);
                                        int g = Integer.parseInt(parts[1]);
                                        int b = Integer.parseInt(parts[2]);
                                        customData.addColor(Color.fromRGB(r, g, b));
                                    } else {
                                        throw new IllegalArgumentException("Invalid color format (expected R,G,B color format): " + value);
                                    }
                                }
                            }
                            break;
                        case "flags":
                            if (modelSection.isList("flags")) {
                                for (boolean value : modelSection.getBooleanList("flags")) {
                                    customData.addFlag(value);
                                }
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown section key: " + sectionKey);
                    }
                }
                setCustomModelData(customData);
            }
        }
        setQuality(s.getString("quality", null));
        setType(s.getString("item", "item"));
        setItemModel(NamespacedKey.fromString(s.getString("item_model", "")));

        if (s.isConfigurationSection("enchantments")) {
            ConfigurationSection enchConf = s.getConfigurationSection("enchantments");
            setEnchantMap(new HashMap<>());
            for (String enchName : Objects.requireNonNull(enchConf).getKeys(false)) {
                Enchantment ench;
                try {
                    ench = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(Key.key(enchName));
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
        this.setIsTemplate(s.getBoolean("isTemplate", false));
        templates.clear();
        ConfigurationSection templatesList = s.getConfigurationSection("templates");
        if (templatesList != null) {
            for (String sectionKey : templatesList.getKeys(false)) {
                String tmp = (String) templatesList.get(sectionKey);
                templates.add(tmp);
            }
        }
        ConfigurationSection componentsList = s.getConfigurationSection("components");
        if (componentsList != null) {
            setComponents(ComponentUtil.getComponents(componentsList, this));
        }
        ConfigurationSection templatePlaceholdersList = s.getConfigurationSection("templatePlaceholders");
        if (templatePlaceholdersList != null) {
            for (String sectionKey : templatePlaceholdersList.getKeys(false)) {
                String tmp = (String) templatePlaceholdersList.get(sectionKey);
                templatePlaceholders.add(tmp);
            }
        }
        rebuild();
    }

    public String getDuplicatePlaceholderMsg(Map<String, List<PlaceholderHolder>> duplicatePlaceholderIds) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("duplicate placeholder key found in item: ")
                .append(getName()).append("\n");
        duplicatePlaceholderIds.forEach((k, v) -> {
            stringBuilder.append("key: ")
                    .append(k)
                    .append(", values: [");
            v.forEach((ph) -> {
                stringBuilder.append(ph.toString());
                stringBuilder.append(",");
            });
            stringBuilder.append("]");
        });
        return stringBuilder.toString();
    }

    public CustomModelData.Builder getCustomModelData() {
        return this.customModelData;
    }

    public void setCustomModelData(CustomModelData.Builder customModelData) {
        this.customModelData = customModelData;
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
        s.set("playerArmourExpression", getPlayerArmourExpression());
        s.set("DamageType", getDamageType());
        s.set("updatemode", updateMode.name());
        s.set("attributemode", attributeMode.name());
        ArrayList<String> descriptionConv = new ArrayList<>(getDescription());
        descriptionConv.replaceAll(string -> string.replaceAll("" + COLOR_CHAR, "&"));
        s.set("description", descriptionConv);
        s.set("item", getItem().toString());
        s.set("ignoreWorldGuard", isIgnoreWorldGuard());
        s.set("canBeOwned", isCanBeOwned());
        s.set("canUse", isCanUse());
        s.set("canPlace", isCanPlace());
        s.set("hasStackId", isHasStackId());

        ItemMeta itemMeta = Bukkit.getItemFactory().getItemMeta(getItem());

        if (itemMeta instanceof LeatherArmorMeta) {
            s.set("item_colour", getDataValue());
        } else if (itemMeta instanceof Damageable) {
            s.set("item_data", getDataValue());
        }
        ConfigurationSection components = s.createSection("components");
        ComponentUtil.toConfigSection(getComponents(), components);
        int i = 0;
        ConfigurationSection powerConfigs = s.createSection("powers");
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

        CustomModelData.Builder builder = getCustomModelData();
        if (builder != null) {
            CustomModelData data = builder.build();
            if (!data.floats().isEmpty()) {
                s.set("customModelData.floats", data.floats());
            }

            if (!data.strings().isEmpty()) {
                s.set("customModelData.strings", data.strings());
            }

            if (!data.flags().isEmpty()) {
                s.set("customModelData.flags", data.flags());
            }

            if (!data.colors().isEmpty()) {
                List<String> colors = data.colors().stream()
                        .map(color -> String.format("%d,%d,%d",
                                color.getRed(),
                                color.getGreen(), color.getBlue()))
                        .collect(Collectors.toList());
                s.set("customModelData.colors", colors);
            }
        } else {
            s.set("customModelData", "");
        }

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
        s.set("barFormat", getBarFormat().name());
        s.set("alwaysAllowMelee", isAlwaysAllowMelee());

        s.set("isTemplate", isTemplate());
        s.set("quality", getQuality());
        s.set("type", getType());
        s.set("item_model", getItemModel() == null ? "" : getItemModel().asString());
        ConfigurationSection templatesConfigs = s.createSection("templates");
        Set<String> templates = getTemplates();
        Iterator<String> it = templates.iterator();
        for (i = 0; i < templates.size(); i++) {
            String next = it.next();
            templatesConfigs.set(String.valueOf(i), next);
        }
        ConfigurationSection templatePlaceHolderConfigs = s.createSection("templatePlaceholders");
        Set<String> templatePlaceHolders = getTemplatePlaceHolders();
        Iterator<String> it1 = templatePlaceHolders.iterator();
        for (i = 0; i < templatePlaceHolders.size(); i++) {
            String next = it1.next();
            templatePlaceHolderConfigs.set(String.valueOf(i), next);
        }
    }

    public String getDamageType() {
        return this.damageType;
    }

    public void setDamageType(String damageType) {
        this.damageType = damageType;
    }

    public String getPlayerArmourExpression() {
        return playerArmourExpression;
    }

    public String getArmourExpression() {
        return armourExpression;
    }

    public void setPlayerArmourExpression(String armour) {
        this.playerArmourExpression = armour;
    }

    public void setArmourExpression(String armour) {
        this.armourExpression = armour;
    }

    public void updateItem(ItemStack item, @Nullable Player player) {
        updateItem(item, false, player);
    }

    public void updateItem(ItemStack item, boolean loreOnly, @Nullable Player player) {
        if (getUpdateMode() == UpdateMode.LORE_ONLY) {
            loreOnly = true;
        }
        List<String> reservedLores = this.filterLores(item);
        item.setType(getItem());
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>(getLore());
        PersistentDataContainer itemTagContainer = Objects.requireNonNull(meta).getPersistentDataContainer();
        SubItemTagContainer rpgitemsTagContainer = makeTag(itemTagContainer, TAG_META);
        set(rpgitemsTagContainer, TAG_ITEM_UID, getUid());
        addDurabilityBar(rpgitemsTagContainer, lore);
        int durability = 0;
        if (getMaxDurability() > 0) {
            durability = computeIfAbsent(rpgitemsTagContainer, TAG_DURABILITY, PersistentDataType.INTEGER, this::getDefaultDurability);
        }
        // Patch for mcMMO buff. See SkillUtils.java#removeAbilityBuff in mcMMO
        if (item.hasItemMeta() && Objects.requireNonNull(item.getItemMeta()).hasLore() && Objects.requireNonNull(item.getItemMeta().getLore()).contains("mcMMO Ability Tool"))
            lore.add("mcMMO Ability Tool");
        lore.addAll(reservedLores);
        if (player != null) {
            if (PlaceholderAPISupport.hasSupport()) {
                lore = PlaceholderAPI.setPlaceholders(player, lore);
            }
        }
        if (getUpdateMode() != UpdateMode.NO_UPDATE && getUpdateMode() != UpdateMode.NO_LORE && getUpdateMode() != UpdateMode.DISPLAY_ONLY && getUpdateMode() != UpdateMode.ENCHANT_ONLY) {
            List<Component> loreComponents = new ArrayList<>();
            for (String lore1 : lore) {
                loreComponents.add(MiniMessage.miniMessage().deserialize("<!i>" + I18n.replaceLegacyColorCodes(lore1)));
            }
            meta.lore(loreComponents);
        }

        meta.setItemModel(getItemModel());

        if (loreOnly) {
            rpgitemsTagContainer.commit();
            item.setItemMeta(meta);
            return;
        }
        String qualityPrefix = plugin.cfg.qualityPrefixes.get(getQuality());
        String finalDisplay = getDisplayName();
        boolean needsQualityPrefix = qualityPrefix != null;

        if (PlaceholderAPISupport.hasSupport()) {
            finalDisplay = PlaceholderAPI.setPlaceholders(player, finalDisplay);
        }

        if (needsQualityPrefix && !finalDisplay.startsWith(qualityPrefix)) {
            finalDisplay = qualityPrefix + finalDisplay;
        }

        UpdateMode updateMode = getUpdateMode();
        boolean shouldUpdateDisplayName = updateMode != UpdateMode.NO_UPDATE &&
                updateMode != UpdateMode.NO_DISPLAY &&
                updateMode != UpdateMode.LORE_ONLY &&
                updateMode != UpdateMode.ENCHANT_ONLY;

        if (shouldUpdateDisplayName) {
            String metaDisplay = meta.hasDisplayName() ? meta.getDisplayName() : "";

            if (!metaDisplay.equals(finalDisplay)) {
                meta.displayName(MiniMessage.miniMessage().deserialize("<!i>" + I18n.replaceLegacyColorCodes(finalDisplay)));
            }
        }

        meta.setUnbreakable(hasMarker(Unbreakable.class));
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
        if (getUpdateMode() != UpdateMode.NO_UPDATE && getUpdateMode() != UpdateMode.DISPLAY_ONLY && getUpdateMode() != UpdateMode.LORE_ONLY && getUpdateMode() != UpdateMode.NO_ENCHANT) {
            if (enchantMap != null) {
                for (Entry<Enchantment, Integer> e : enchantMap.entrySet()) {
                    meta.addEnchant(e.getKey(), Math.max(meta.getEnchantLevel(e.getKey()), e.getValue()), true);
                }
            }
        }
        checkAndMakeUnique(rpgitemsTagContainer);
        rpgitemsTagContainer.commit();
        item.setItemMeta(refreshAttributeModifiers(meta));
        try {
            if (RPGItems.plugin.cfg.itemStackUuid) {
                if (ItemTagUtils.getString(item, NBT_ITEM_UUID).isEmpty()) {
                    UUID uuid = UUID.randomUUID();
                    ItemTagUtils.setString(item, NBT_ITEM_UUID, uuid.toString());
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        if (getCustomModelData() != null) {
            item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, getCustomModelData());
        }
        item.resetData(DataComponentTypes.BANNER_PATTERNS);
        item.resetData(DataComponentTypes.CAN_BREAK);
        item.resetData(DataComponentTypes.CAN_PLACE_ON);
        item.resetData(DataComponentTypes.CONSUMABLE);
        item.resetData(DataComponentTypes.DAMAGE_RESISTANT);
        item.resetData(DataComponentTypes.DEATH_PROTECTION);
        item.resetData(DataComponentTypes.DYED_COLOR);
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(Color.fromRGB(getDataValue()));
        }
        item.resetData(DataComponentTypes.ENCHANTABLE);
        item.resetData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        item.resetData(DataComponentTypes.EQUIPPABLE);
        item.resetData(DataComponentTypes.PROFILE);
        item.resetData(DataComponentTypes.FOOD);
        item.resetData(DataComponentTypes.GLIDER);
        item.resetData(DataComponentTypes.TOOLTIP_DISPLAY);
        item.resetData(DataComponentTypes.MAX_DAMAGE);
        item.resetData(DataComponentTypes.MAX_STACK_SIZE);
        item.resetData(DataComponentTypes.PROVIDES_BANNER_PATTERNS);
        item.resetData(DataComponentTypes.PROVIDES_TRIM_MATERIAL);
        item.resetData(DataComponentTypes.POTION_DURATION_SCALE);
        item.resetData(DataComponentTypes.RARITY);
        item.resetData(DataComponentTypes.TOOL);
        item.resetData(DataComponentTypes.TOOLTIP_STYLE);
        item.resetData(DataComponentTypes.TRIM);
        item.resetData(DataComponentTypes.USE_COOLDOWN);
        item.resetData(DataComponentTypes.USE_REMAINDER);
        item.resetData(DataComponentTypes.WEAPON);
        if (getComponents() != null) {
            for (Map<DataComponentType, Object> component : getComponents()) {
                for (Map.Entry<DataComponentType, Object> entry : component.entrySet()) {
                    DataComponentType key = entry.getKey();
                    Object value = entry.getValue();
                    if (value == ComponentUtil.ComponentStatus.UNSET) {
                        item.unsetData(key);
                    } else if (value == ComponentUtil.ComponentStatus.NON_VALUED) {
                        item.setData((DataComponentType.NonValued) key);
                    } else {
                        if (key == DataComponentTypes.BANNER_PATTERNS) {
                            item.setData(DataComponentTypes.BANNER_PATTERNS, (BannerPatternLayers.Builder) value);
                        } else if (key == DataComponentTypes.BLOCKS_ATTACKS) {
                            item.setData(DataComponentTypes.BLOCKS_ATTACKS, (BlocksAttacks.Builder) value);
                        } else if (key == DataComponentTypes.BREAK_SOUND) {
                            item.setData(DataComponentTypes.BREAK_SOUND, (Key) value);
                        } else if (key == DataComponentTypes.CAN_BREAK) {
                            item.setData(DataComponentTypes.CAN_BREAK, (ItemAdventurePredicate.Builder) value);
                        } else if (key == DataComponentTypes.CAN_PLACE_ON) {
                            item.setData(DataComponentTypes.CAN_PLACE_ON, (ItemAdventurePredicate.Builder) value);
                        } else if (key == DataComponentTypes.CONSUMABLE) {
                            item.setData(DataComponentTypes.CONSUMABLE, (Consumable.Builder) value);
                        } else if (key == DataComponentTypes.DAMAGE_RESISTANT) {
                            item.setData(DataComponentTypes.DAMAGE_RESISTANT, (DamageResistant) value);
                        } else if (key == DataComponentTypes.DEATH_PROTECTION) {
                            item.setData(DataComponentTypes.DEATH_PROTECTION, (DeathProtection.Builder) value);
                        } else if (key == DataComponentTypes.DYED_COLOR) {
                            item.setData(DataComponentTypes.DYED_COLOR, (DyedItemColor.Builder) value);
                        } else if (key == DataComponentTypes.ENCHANTABLE) {
                            item.setData(DataComponentTypes.ENCHANTABLE, (Enchantable) value);
                        } else if (key == DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE) {
                            item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, (boolean) value);
                        } else if (key == DataComponentTypes.EQUIPPABLE) {
                            item.setData(DataComponentTypes.EQUIPPABLE, (Equippable.Builder) value);
                        } else if (key == DataComponentTypes.FOOD) {
                            item.setData(DataComponentTypes.FOOD, (FoodProperties.Builder) value);
                        } else if (key == DataComponentTypes.MAX_DAMAGE) {
                            item.setData(DataComponentTypes.MAX_DAMAGE, (int) value);
                        } else if (key == DataComponentTypes.MAX_STACK_SIZE) {
                            item.setData(DataComponentTypes.MAX_STACK_SIZE, (int) value);
                        } else if (key == DataComponentTypes.PROFILE) {
                            item.setData(DataComponentTypes.PROFILE, (ResolvableProfile.Builder) value);
                        } else if (key == DataComponentTypes.PROVIDES_BANNER_PATTERNS) {
                            item.setData(DataComponentTypes.PROVIDES_BANNER_PATTERNS, (TagKey<PatternType>) value);
                        } else if (key == DataComponentTypes.PROVIDES_TRIM_MATERIAL) {
                            item.setData(DataComponentTypes.PROVIDES_TRIM_MATERIAL, (TrimMaterial) value);
                        } else if (key == DataComponentTypes.POTION_DURATION_SCALE) {
                            item.setData(DataComponentTypes.POTION_DURATION_SCALE, (float) value);
                        } else if (key == DataComponentTypes.RARITY) {
                            item.setData(DataComponentTypes.RARITY, (ItemRarity) value);
                        } else if (key == DataComponentTypes.TOOL) {
                            item.setData(DataComponentTypes.TOOL, (Tool.Builder) value);
                        } else if (key == DataComponentTypes.TOOLTIP_DISPLAY) {
                            item.setData(DataComponentTypes.TOOLTIP_DISPLAY, (TooltipDisplay.Builder) value);
                        } else if (key == DataComponentTypes.TOOLTIP_STYLE) {
                            item.setData(DataComponentTypes.TOOLTIP_STYLE, (Key) value);
                        } else if (key == DataComponentTypes.TRIM) {
                            item.setData(DataComponentTypes.TRIM, (ItemArmorTrim.Builder) value);
                        } else if (key == DataComponentTypes.USE_COOLDOWN) {
                            item.setData(DataComponentTypes.USE_COOLDOWN, (UseCooldown.Builder) value);
                        } else if (key == DataComponentTypes.USE_REMAINDER) {
                            item.setData(DataComponentTypes.USE_REMAINDER, (UseRemainder) value);
                        } else if (key == DataComponentTypes.WEAPON) {
                            item.setData(DataComponentTypes.WEAPON, (Weapon.Builder) value);
                        }
                    }
                }
            }
        }
        if (item.hasData(DataComponentTypes.MAX_DAMAGE) || item.getType().hasDefaultData(DataComponentTypes.MAX_DAMAGE)) {
            if (getMaxDurability() > 0) {
                int damage = item.getData(DataComponentTypes.MAX_DAMAGE) - ((short) ((double) item.getData(DataComponentTypes.MAX_DAMAGE) * ((double) durability / (double) getMaxDurability())));
                item.setData(DataComponentTypes.DAMAGE, Math.max(damage, 0));
            } else {
                item.setData(DataComponentTypes.DAMAGE, item.getData(DataComponentTypes.MAX_DAMAGE) != 0 ? 0 : getDataValue());
            }
        }
    }

    private void checkAndMakeUnique(SubItemTagContainer meta) {
        List<Unique> markers = getMarker(Unique.class);
        List<SlotCondition> conditions = getConditions(SlotCondition.class);

        if (!markers.isEmpty()) {
            Unique unique = markers.get(0);
            if (unique.enabled) {
                if (!meta.has(RGI_UNIQUE_MARK, PersistentDataType.BYTE)) {
                    meta.set(RGI_UNIQUE_MARK, PersistentDataType.BYTE, (byte) 0);
                }
                meta.set(RGI_UNIQUE_ID, PersistentDataType.STRING, UUID.randomUUID().toString());
            } else {
                meta.remove(RGI_UNIQUE_MARK);
                meta.remove(RGI_UNIQUE_ID);
            }
        }
        if (!conditions.isEmpty()) {
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
                        out.append(ChatColor.GREEN).append(boxChar).append(" ");
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

    private List<String> filterLores(ItemStack item) {
        List<LoreFilter> patterns = getMarker(LoreFilter.class).stream()
                .filter(p -> !Strings.isNullOrEmpty(p.regex))
                .map(LoreFilter::compile)
                .collect(Collectors.toList());
        if (patterns.isEmpty() || !item.hasItemMeta() ||
                !Objects.requireNonNull(item.getItemMeta()).hasLore()) {
            return Collections.emptyList();
        }

        return Objects.requireNonNull(item.getItemMeta().getLore()).stream()
                .filter(str -> patterns.stream().anyMatch(p -> {
                    Matcher matcher = p.pattern().matcher(ChatColor.stripColor(str));
                    return p.find ? matcher.find() : matcher.matches();
                }))
                .collect(Collectors.toList());
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
                NamespacedKey uuid = NamespacedKey.fromString(attributeModifier.namespacedKey);
                EquipmentSlotGroup slot = attributeModifier.slot;
                org.bukkit.attribute.AttributeModifier modifier;
                if (slot != null) {
                    modifier = new org.bukkit.attribute.AttributeModifier(
                            uuid,
                            attributeModifier.amount,
                            attributeModifier.operation,
                            attributeModifier.slot
                    );
                } else {
                    modifier = new org.bukkit.attribute.AttributeModifier(
                            uuid,
                            attributeModifier.amount,
                            attributeModifier.operation
                    );
                }
                if (old != null) {
                    old.entries().stream().filter(m -> m.getValue().getKey().equals(uuid)).findAny().ifPresent(
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
     * @return Final damage or -1 if it should cancel this event
     */
    public double meleeDamage(Player p, double originDamage, ItemStack stack, Entity entity, double multiplier) {
        double damage = originDamage;
        if (!canDoMeleeTo(stack, entity) || ItemManager.canUse(p, this) == Event.Result.DENY) {
            return -1;
        }
        boolean can = consumeDurability(stack, getHittingCost());
        if (!can) {
            return -1;
        }
        if (getDamageMode() != DamageMode.VANILLA) {
            damage = getDamageMin() != getDamageMax() ? (getDamageMin() + ThreadLocalRandom.current().nextInt(getDamageMax() - getDamageMin() + 1)) : getDamageMin();


            if (getDamageMode().toString().contains("MULTIPLY")) {
                damage *= originDamage;
            }

            if (getDamageMode() == DamageMode.FIXED) {
                Collection<PotionEffect> potionEffects = p.getActivePotionEffects();
                double strength = 0, weak = 0;
                for (PotionEffect pe : potionEffects) {
                    if (pe.getType().equals(PotionEffectType.STRENGTH)) {
                        strength = 3 * (pe.getAmplifier() + 1);
                    }
                    if (pe.getType().equals(PotionEffectType.WEAKNESS)) {
                        weak = 4 * (pe.getAmplifier() + 1);
                    }
                }
                damage = damage + strength - weak;
            }

            if (getDamageMode() == DamageMode.ADDITIONAL) {
                damage += originDamage;
            }
            if (getDamageMode().toString().contains("RESPECT_VANILLA")) {
                damage *= multiplier;
            }
            if (damage < 0) damage = 0;
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
        if (!entity.isValid() || p.getWorld() != entity.getWorld()) {
            return -1;
        }
        double distance = p.getLocation().distance(entity.getLocation());
        if (!canDoProjectileTo(stack, distance, entity)) {
            return -1;
        }

        if (getDamageMode() != DamageMode.VANILLA) {
            damage = getDamageMin() != getDamageMax() ? (getDamageMin() + ThreadLocalRandom.current().nextInt(getDamageMax() - getDamageMin() + 1)) : getDamageMin();

            if (getDamageMode().toString().contains("MULTIPLY")) {
                damage *= originDamage;
            }

            //Apply force adjustments
            if (damager.hasMetadata("RPGItems.Force")) {
                damage *= damager.getMetadata("RPGItems.Force").get(0).asFloat();
            }
            if (getDamageMode().toString().contains("ADDITIONAL")) {
                damage += originDamage;
            }
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
        List<Condition<?>> conditions = conds.stream().filter(p -> ids.contains(p.id())).toList();
        List<Condition<?>> failed = conditions.stream().filter(p -> p.isStatic() ? !context.get(p).isOK() : !p.check(player, i, context).isOK()).toList();
        if (failed.isEmpty()) return null;
        return failed.stream().anyMatch(Condition::isCritical) ? PowerResult.abort() : PowerResult.condition();
    }

    private Map<Condition<?>, PowerResult<?>> checkStaticCondition(Player player, ItemStack i, List<Condition<?>> conds) {
        Set<String> ids = powers.stream().flatMap(p -> p.getConditions().stream()).collect(Collectors.toSet());
        List<Condition<?>> statics = conds.stream().filter(Condition::isStatic).filter(p -> ids.contains(p.id())).toList();
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
        lines.removeFirst();
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
                if (txt != null && !txt.isEmpty()) {
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
                if (getDamageMode().toString().contains("ADDITIONAL")) {
                    damageStr += I18n.formatDefault("item.additionaldamage", getDamageMin() == getDamageMax() ? String.valueOf(getDamageMin()) : getDamageMin() + "-" + getDamageMax());
                } else if (getDamageMode().toString().contains("MULTIPLY")) {
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
        meta.displayName(MiniMessage.miniMessage().deserialize("<!i>" + I18n.replaceLegacyColorCodes(getDisplayName())));
        rStack.setItemMeta(meta);

        updateItem(rStack, false, null);
        return rStack;
    }

    public void toModel(ItemStack itemStack) {
        updateItem(itemStack, null);
        ItemMeta itemMeta = itemStack.getItemMeta();
        SubItemTagContainer meta = makeTag(Objects.requireNonNull(itemMeta).getPersistentDataContainer(), TAG_META);
        meta.remove(TAG_OWNER);
        meta.remove(TAG_STACK_ID);
        set(meta, TAG_IS_MODEL, true);
        ItemTagUtils.setBoolean(itemStack, NBT_IS_MODEL, true);
        meta.commit();
        itemMeta.setDisplayName(getDisplayName());
        itemStack.setItemMeta(itemMeta);
    }

    public void unModel(ItemStack itemStack, Player owner) {
        updateItem(itemStack, owner);
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
                p.sendMessage(I18n.getInstance(p.getLocale()).getFormatted("message.error.permission", getDisplayName()));
            return Event.Result.DENY;
        }
        return Event.Result.ALLOW;
    }

    public void print(CommandSender sender) {
        print(sender, true);
    }

    public void print(CommandSender sender, boolean advance) {
        String author = this.getAuthor();
        Component authorComponent = Component.text(author);
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
                    .append(I18n.getInstance(((Player) sender).getLocale()).getFormatted("message.item.print"), toItemStack())
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
        this.updateItem(item, true, null);
    }

    public Optional<Integer> getItemStackDurability(ItemStack item) {
        if (getMaxDurability() == -1) {
            return Optional.empty();
        }
        ItemMeta itemMeta = item.getItemMeta();
        //Power Consume will make this null in triggerPostFire().
        if (itemMeta == null) {
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

        if (item.getAmount() > 1) {
            float durcost = (float) val / item.getAmount();
            float point = durcost - (int) durcost;
            if (Math.random() <= point) {
                val = (int) durcost + 1;
            }
        }

        ItemMeta itemMeta = item.getItemMeta();

        if (getMaxDurability() != -1) {
            SubItemTagContainer tagContainer = makeTag(Objects.requireNonNull(itemMeta), TAG_META);
            durability = computeIfAbsent(tagContainer, TAG_DURABILITY, PersistentDataType.INTEGER, this::getDefaultDurability);

            int newDurability = durability - val;

            if (checkbound) {
                if (getMaxDurability() == 0) {
                    if (newDurability < getDurabilityLowerBound()) {
                        newDurability = getDurabilityLowerBound();
                    }
                } else {
                    if (newDurability < getDurabilityLowerBound()) {
                        newDurability = getDurabilityLowerBound();
                    }
                    if (durability <= getDurabilityUpperBound() && newDurability > getDurabilityUpperBound()) {
                        newDurability = getDurabilityUpperBound();
                    }
                }

                if (newDurability == durability) {
                    set(tagContainer, TAG_DURABILITY, durability);
                    tagContainer.commit();
                    item.setItemMeta(itemMeta);
                    return false;
                }
            }

            durability = newDurability;

            boolean clear = false;
            if (durability <= 0) {
                clear = true;
            } else if (durability > getMaxDurability() && getMaxDurability() > 0) {
                durability = getMaxDurability();
            }

            set(tagContainer, TAG_DURABILITY, durability);
            tagContainer.commit();
            item.setItemMeta(itemMeta);
            this.updateItem(item, false, null);
            if (clear) {
                item.setAmount(0);
            }
        }

        return true;
    }


    public void give(Player player, int count, boolean wear) {
        ItemStack itemStack = toItemStack();
        itemStack.setAmount(count);
        if (wear) {
            if (
                    item.toString().contains("HELMET")
            ) {
                if (player.getInventory().getHelmet() == null) {
                    player.getInventory().setHelmet(itemStack);
                    return;
                }
            } else if (
                    item.toString().contains("CHESTPLATE")
            ) {
                if (player.getInventory().getChestplate() == null) {
                    player.getInventory().setChestplate(itemStack);
                    return;
                }
            } else if (
                    item.toString().contains("LEGGINGS")
            ) {
                if (player.getInventory().getLeggings() == null) {
                    player.getInventory().setLeggings(itemStack);
                    return;
                }
            } else if (
                    item.toString().contains("BOOTS")
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
        if ("".equals(power.getPlaceholderId())) {
            String placeholderId = power.getName() + "-" + getPowers().stream().filter(power1 -> power1.getName().equals(power.getName())).count();
            power.setPlaceholderId(placeholderId);
        }
        powers.add(power);
        keys.put(power, key);
        String placeholderId = power.getPlaceholderId();
        placeholders.put(placeholderId, power);
        if (update) {
            rebuild();
        }
    }

    public void removePower(Power power) {
        powers.remove(power);
        keys.remove(power);
        String placeholderId = power.getPlaceholderId();
        if (!"".equals(placeholderId)) {
            placeholders.remove(placeholderId, power);
        }
        power.deinit();
        rebuild();
    }

    public void addCondition(NamespacedKey key, Condition<?> condition) {
        addCondition(key, condition, true);
    }

    private void addCondition(NamespacedKey key, Condition<?> condition, boolean update) {
        if ("".equals(condition.getPlaceholderId())) {
            String placeholderId = condition.getName() + "-" + getConditions().stream().filter(power1 -> power1.getName().equals(condition.getName())).count();
            condition.setPlaceholderId(placeholderId);
        }
        conditions.add(condition);
        keys.put(condition, key);
        String placeholderId = condition.getPlaceholderId();
        placeholders.put(placeholderId, condition);
        if (update) {
            rebuild();
        }
    }

    public void removeCondition(Condition<?> condition) {
        conditions.remove(condition);
        keys.remove(condition);
        String placeholderId = condition.getPlaceholderId();
        if (!"".equals(placeholderId)) {
            placeholders.remove(placeholderId, condition);
        }
        rebuild();
    }

    public void addMarker(NamespacedKey key, Marker marker) {
        addMarker(key, marker, true);
    }

    private void addMarker(NamespacedKey key, Marker marker, boolean update) {
        if ("".equals(marker.getPlaceholderId())) {
            String placeholderId = marker.getName() + "-" + getMarkers().stream().filter(power1 -> power1.getName().equals(marker.getName())).count();
            marker.setPlaceholderId(placeholderId);
        }
        markers.add(marker);
        keys.put(marker, key);
        String placeholderId = marker.getPlaceholderId();
        placeholders.put(placeholderId, marker);
        if (update) {
            rebuild();
        }
    }

    public void removeMarker(Marker marker) {
        markers.remove(marker);
        keys.remove(marker);
        String placeholderId = marker.getPlaceholderId();
        if (!"".equals(placeholderId)) {
            placeholders.remove(placeholderId, marker);
        }
        rebuild();
    }

    public Map<String, List<PlaceholderHolder>> checkDuplicatePlaceholderIds() {
        Map<String, List<PlaceholderHolder>> ids = new HashMap<>();
        getPlaceholdersStream()
                .map(ph -> ph)
                .forEach(placeholder -> {
                    String placeholderId = placeholder.getPlaceholderId();
                    if ("".equals(placeholderId)) {
                        return;
                    }
                    List<PlaceholderHolder> placeholderHolders = ids.computeIfAbsent(placeholderId, (s) -> new ArrayList<>());
                    placeholderHolders.add(placeholder);
                });
        Map<String, List<PlaceholderHolder>> result = new HashMap<>();
        ids.forEach((key, value) -> {
            if (value.size() > 1) {
                result.put(key, value);
            }
        });
        return result;
    }

    public Stream<PlaceholderHolder> getPlaceholdersStream() {
        List<Power> powers = getPowers();
        List<Condition<?>> conditions = getConditions();
        List<Marker> markers = getMarkers();
        return Stream.concat(
                Stream.concat(
                        powers.stream().map(ph -> ((PlaceholderHolder) ph)),
                        conditions.stream().map(ph -> ((PlaceholderHolder) ph))),
                markers.stream());
    }

    public void addDescription(String str) {
        getDescription().add(HexColorUtils.hexColored(str));
        rebuild();
    }

    public void toggleBar() {
        setHasDurabilityBar(!isHasDurabilityBar());
        rebuild();
    }

    public Component getComponent(CommandSender sender) {
        String locale = RPGItems.plugin.cfg.language;
        if (sender instanceof Player) {
            locale = ((Player) sender).getLocale();
        }
        return getComponent(locale);
    }

    public Component getComponent(String locale) {
        Component msg = Component.text(getDisplayName());
        msg = msg.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/rpgitem" + getName())).hoverEvent(toItemStack());
        return msg;
    }

    private <TEvent extends Event, T extends Pimpl, TResult, TReturn> List<T> getPower(Trigger<TEvent, T, TResult, TReturn> trigger, Player player, ItemStack stack) {
        return powers.stream()
                .filter(p -> p.getTriggers().contains(trigger))
                .map(p -> {
                    Class<? extends Power> cls = p.getClass();
                    Power proxy = Interceptor.create(p, player, stack, trigger);
                    return PowerManager.createImpl(cls, proxy);
                })
                .map(p -> p.cast(trigger.getPowerClass()))
                .collect(Collectors.toList());
    }

    public PlaceholderHolder getPlaceholderHolder(String placeholderId) {
        return placeholders.get(placeholderId);
    }

    public PlaceholderHolder replacePlaceholder(String powerId, PlaceholderHolder placeHolder) {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        PlaceholderHolder oldPh = placeholders.get(powerId);
        PlaceholderHolder newPh = null;
        if (oldPh == null) {
            return null;
        }
        placeHolder.save(yamlConfiguration);
        if (oldPh instanceof Power) {
            int i = powers.indexOf(oldPh);
            if (i == -1) {
                throw new IllegalStateException();
            }
            Class<? extends Power> pwClz = ((Power) oldPh).getClass();
            Power pow = PowerManager.instantiate(pwClz);
            pow.setItem(this);
            pow.init(yamlConfiguration);
            powers.set(i, pow);
            newPh = pow;
        } else if (oldPh instanceof Condition) {
            int i = conditions.indexOf(oldPh);
            if (i == -1) {
                throw new IllegalStateException();
            }
            Class<? extends Condition> pwClz = ((Condition<?>) oldPh).getClass();
            Condition<?> pow = PowerManager.instantiate(pwClz);
            pow.setItem(this);
            pow.init(yamlConfiguration);
            conditions.set(i, pow);
            newPh = pow;
        } else if (oldPh instanceof Marker) {
            int i = markers.indexOf(oldPh);
            if (i == -1) {
                throw new IllegalStateException();
            }
            Class<? extends Marker> pwClz = ((Marker) oldPh).getClass();
            Marker pow = PowerManager.instantiate(pwClz);
            pow.setItem(this);
            pow.init(yamlConfiguration);
            markers.set(i, pow);
            newPh = pow;
        }
        if (newPh == null) {
            return null;
        }

        NamespacedKey remove = keys.remove(oldPh);
        keys.put(newPh, remove);
        placeholders.put(powerId, newPh);
        return newPh;
    }


    public void updateFromTemplate(RPGItem target) throws UnknownPowerException {
        Set<String> templatePlaceHolders = target.getTemplatePlaceHolders();
        Map<String, List<String>> powerMap = new LinkedHashMap<>();
        Map<String, Object> valMap = new LinkedHashMap<>();
        //extract original val from self
        templatePlaceHolders.forEach(s -> {
            String[] split = s.split(":");
            PlaceholderHolder power = getPlaceholderHolder(split[0]);
            String propName = split[1];
            Object origVal = null;
            try {
                origVal = getPropVal(power.getClass(), propName, power);
                List<String> strings = powerMap.computeIfAbsent(split[0], (str) -> new ArrayList<>());
                strings.add(s);
                valMap.put(s, origVal);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        });

        copyFromTemplate(target);

        //replace powers & fill placeholders
        target.getPlaceholdersStream().forEach(power -> {
            String powerId = power.getPlaceholderId();
            PlaceholderHolder replaced = replacePlaceholder(powerId, power);
            List<String> strings = powerMap.get(powerId);
            if (strings != null) {
                strings.forEach(s -> {
                    String[] split = s.split(":");
                    String propName = split[1];
                    Object origVal = valMap.get(s);
                    try {
                        setPropVal(replaced.getClass(), propName, replaced, origVal);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        throw new RuntimeException();
                    }
                });
            }
        });

        ItemManager.save(this);
    }

    private Object getPropVal(Class<?> aClass, String propName, PlaceholderHolder placeholder) throws IllegalAccessException {
        Field getMethod = getField(aClass, propName);
        getMethod.setAccessible(true);
        return getMethod.get(placeholder);
    }

    private void setPropVal(Class<?> aClass, String propName, PlaceholderHolder placeholder, Object value) throws IllegalAccessException {
        Field getMethod = getField(aClass, propName);
        getMethod.setAccessible(true);
        getMethod.set(placeholder, value);
    }


    private Field getField(Class<?> aClass, String methodName) {
        Field getMethod = null;
        while (true) {
            try {
                getMethod = aClass.getDeclaredField(methodName);
                break;
            } catch (NoSuchFieldException e) {
                aClass = aClass.getSuperclass();
            }
            if (aClass == null) {
                throw new RuntimeException("invalid placeholder");
            }
        }
        return getMethod;
    }

    private void copyFromTemplate(RPGItem target) throws UnknownPowerException {
        List<Power> powers = new ArrayList<>(getPowers());
        List<Marker> markers = new ArrayList<>(getMarkers());
        List<Condition<?>> conditions = new ArrayList<>(getConditions());
        boolean isTemplate = isTemplate();
        Set<String> templates = new HashSet<>(getTemplates());
        placeholders.clear();
        this.powers.clear();
        this.markers.clear();
        this.conditions.clear();

        //copy other settings,
        YamlConfiguration config = new YamlConfiguration();
        target.save(config);
        this.restore(config);

        this.powers = powers;
        this.markers = markers;
        this.conditions = conditions;
        this.isTemplate = isTemplate;
        this.templates = templates;
        this.rebuildPlaceholder();
    }

    private void rebuildPlaceholder() {
        placeholders.clear();
        getPlaceholdersStream().forEach(placeholderHolder -> {
            placeholders.put(placeholderHolder.getPlaceholderId(), placeholderHolder);
        });
    }

    public List<String> getTemplatePlaceholders() {
        return new ArrayList<>(templatePlaceholders);
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

    public void setArmour(int a) {
        setArmour(a, true);
    }

    public boolean isAlwaysAllowMelee() {
        return alwaysAllowMelee;
    }

    public void setAlwaysAllowMelee(boolean alwaysAllowMelee) {
        this.alwaysAllowMelee = alwaysAllowMelee;
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
        this.displayName = HexColorUtils.hexColored(displayName);
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

    void setFile(File itemFile) {
        file = itemFile;
    }

    public EnchantMode getEnchantMode() {
        return enchantMode;
    }

    public void setEnchantMode(EnchantMode enchantMode) {
        this.enchantMode = enchantMode;
    }

    public int getHitCost() {
        return hitCost;
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

    public List<Map<DataComponentType, Object>> getComponents() {
        return components;
    }

    public void setComponents(List<Map<DataComponentType, Object>> components) {
        this.components = components;
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

    public boolean isTemplate() {
        return isTemplate;
    }

    public void setIsTemplate(boolean template) {
        isTemplate = template;
    }

    public boolean isTemplateOf(String templateName) {
        return templates.contains(templateName);
    }

    public Set<String> getTemplates() {
        return templates;
    }

    public void setTemplateOf(String templateName) {
        this.templates.add(templateName);
    }

    public void addTemplatePlaceHolder(String placeHolder) {
        this.templatePlaceholders.add(placeHolder);
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

    public void removeTemplatePlaceHolder(String placeHolder) {
        this.templatePlaceholders.remove(placeHolder);
    }

    private Set<String> getTemplatePlaceHolders() {
        return templatePlaceholders;
    }

    public void setTemplatePlaceHolders(List<String> placeHolder) {
        this.templatePlaceholders.clear();
        this.templatePlaceholders.addAll(placeHolder);
    }

    public AttributeMode getAttributeMode() {
        return attributeMode;
    }

    public void setAttributeMode(AttributeMode attributeMode) {
        this.attributeMode = attributeMode;
    }

    public NamespacedKey getItemModel() {
        return this.itemModel;
    }

    public void setItemModel(NamespacedKey itemModel) {
        this.itemModel = itemModel;
    }


    public enum DamageMode {
        FIXED,
        FIXED_WITHOUT_EFFECT,
        FIXED_RESPECT_VANILLA,
        FIXED_WITHOUT_EFFECT_RESPECT_VANILLA,
        VANILLA,
        ADDITIONAL,
        ADDITIONAL_RESPECT_VANILLA,
        MULTIPLY
    }


    public enum EnchantMode {
        DISALLOW,
        PERMISSION,
        ALLOW;
    }

    public void setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
    }

    public enum UpdateMode {
        FULL_UPDATE, DISPLAY_ONLY, LORE_ONLY, ENCHANT_ONLY, NO_DISPLAY, NO_LORE, NO_ENCHANT, NO_UPDATE
    }

    public UpdateMode getUpdateMode() {
        return updateMode;
    }

    public enum AttributeMode {
        FULL_UPDATE, PARTIAL_UPDATE
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
