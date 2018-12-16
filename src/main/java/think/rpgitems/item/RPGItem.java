package think.rpgitems.item;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.inventory.meta.tags.ItemTagType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.librazy.nclangchecker.LangKey;
import org.librazy.nclangchecker.LangKeyType;
import think.rpgitems.Events;
import think.rpgitems.Handler;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;
import think.rpgitems.power.impl.*;
import think.rpgitems.support.WGSupport;
import think.rpgitems.utils.MaterialUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static org.bukkit.ChatColor.COLOR_CHAR;
import static think.rpgitems.utils.ItemTagUtils.*;

public class RPGItem {
    @Deprecated
    public static final int MC_ENCODED_ID_LENGTH = 16;
    public static final NamespacedKey TAG_ITEM_UID = new NamespacedKey(RPGItems.plugin, "item_uid");
    public static final NamespacedKey TAG_META = new NamespacedKey(RPGItems.plugin, "meta");
    public static final NamespacedKey TAG_DURABILITY = new NamespacedKey(RPGItems.plugin, "durability");

    static RPGItems plugin;
    private boolean ignoreWorldGuard = false;
    private List<String> description = new ArrayList<>();
    private boolean showPowerText = true;
    private boolean showArmourLore = true;
    private Map<Enchantment, Integer> enchantMap = null;
    private List<ItemFlag> itemFlags = new ArrayList<>();
    private boolean customItemModel = false;
    private boolean numericBar = plugin.cfg.numericBar;
    // Powers
    private List<Power> powers = new ArrayList<>();
    private HashMap<Power, NamespacedKey> powerKeys = new HashMap<>();
    // Recipes
    private int recipeChance = 6;
    private boolean hasRecipe = false;
    private List<ItemStack> recipe = null;
    // Drops
    private Map<String, Double> dropChances = new HashMap<>();
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
    private int armour = 0;
    private String type = I18n.format("item.type");
    private String hand = I18n.format("item.hand");

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

    private int blockBreakingCost = 0;
    private int hittingCost = 0;
    private int hitCost = 0;
    private boolean hitCostByDamage = false;
    private String mcVersion;
    private int pluginSerial;
    private List<String> lore;

    public RPGItem(String name, int uid, CommandSender author) {
        this.name = name;
        this.uid = uid;
        this.setAuthor(author instanceof Player ? ((Player) author).getUniqueId().toString() : plugin.cfg.defaultAuthor);
        setItem(Material.WOODEN_SWORD);
        setDisplayName(getItem().toString());
        getItemFlags().add(ItemFlag.HIDE_ATTRIBUTES);
        setMcVersion(RPGItems.getServerMCVersion());
        setPluginSerial(RPGItems.getSerial());
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
        RPGItem rItem = ItemManager.toRPGItem(item);
        if (rItem == null)
            return;
        rItem.updateItem(item);
    }

    @Deprecated
    private static String getMCEncodedUID(int id) {
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

    @Deprecated
    public static Optional<Integer> decodeId(String str) throws NumberFormatException {
        if (str.length() < 16) {
            return Optional.empty();
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (str.charAt(i) != COLOR_CHAR) {
                return Optional.empty();
            }
            i++;
            out.append(str.charAt(i));
        }
        return Optional.of(Integer.parseUnsignedInt(out.toString(), 16));
    }

    public static RPGItems getPlugin() {
        return plugin;
    }

    private void restore(ConfigurationSection s) throws UnknownPowerException, UnknownExtensionException {
        setAuthor(s.getString("author", ""));
        setNote(s.getString("note", ""));
        setLicense(s.getString("license", ""));
        setPluginSerial(s.getInt("pluginSerial", 0));
        setMcVersion(s.getString("mcVersion", ""));

        String display = s.getString("display");

        @SuppressWarnings("deprecation") Quality quality = s.isString("quality") ? Quality.valueOf(s.getString("quality")) : null;
        if (quality != null) {
            display = quality.colour + ChatColor.BOLD + display;
        }

        setDisplayName(display);
        setType(s.getString("type", I18n.format("item.type")), false);
        setHand(ChatColor.translateAlternateColorCodes('&', s.getString("hand", I18n.format("item.hand"))), false);
        setDescription(s.getStringList("description"));
        for (int i = 0; i < getDescription().size(); i++) {
            getDescription().set(i, ChatColor.translateAlternateColorCodes('&', getDescription().get(i)));
        }
        setDamageMin(s.getInt("damageMin"));
        setDamageMax(s.getInt("damageMax"));
        setArmour(s.getInt("armour", 0), false);
        String materialName = s.getString("item");
        setItem(MaterialUtils.getMaterial(materialName, Bukkit.getConsoleSender()));
        ItemMeta itemMeta = Bukkit.getItemFactory().getItemMeta(getItem());
        if (itemMeta instanceof LeatherArmorMeta) {
            setDataValue(s.getInt("item_colour"));
        } else if (itemMeta instanceof Damageable) {
            setDataValue(s.getInt("item_data"));
        }
        setIgnoreWorldGuard(s.getBoolean("ignoreWorldGuard", false));

        // Powers
        ConfigurationSection powerList = s.getConfigurationSection("powers");
        if (powerList != null) {
            Map<Power, ConfigurationSection> conf = new HashMap<>();
            for (String sectionKey : powerList.getKeys(false)) {
                ConfigurationSection section = powerList.getConfigurationSection(sectionKey);
                try {
                    String powerName = section.getString("powerName");
                    NamespacedKey key = PowerManager.parseKey(powerName);
                    Class<? extends Power> power = PowerManager.getPower(key);
                    if (power == null) {
                        plugin.getLogger().warning("Unknown power:" + key + " on item " + this.name);
                        throw new UnknownPowerException(key);
                    }
                    Power pow = power.getConstructor().newInstance();
                    pow.setItem(this);
                    pow.init(section);
                    addPower(key, pow, false);
                    conf.put(pow, section);
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
            // 3.5 -> 3.6 conversion
            List<PowerSelector> selectors = getPower(PowerSelector.class);
            for (PowerSelector selector : selectors) {
                ConfigurationSection section = conf.get(selector);
                String applyTo = section.getString("applyTo");
                if (Strings.isNullOrEmpty(applyTo)) {
                    continue;
                }
                selector.id = UUID.randomUUID().toString();
                Set<? extends Class<? extends Power>> applicable = Arrays.stream(applyTo.split(",")).map(p -> p.split(" ", 2)[0]).map(PowerManager::parseLegacyKey).map(PowerManager::getPower).collect(Collectors.toSet());
                for (Class<? extends Power> pow : applicable) {
                    List<? extends Power> app = getPower(pow);
                    for (Power power : app) {
                        if (power instanceof BasePower) {
                            ((BasePower) power).selectors.add(selector.id());
                        }
                    }
                }
            }
        }

        setHasPermission(s.getBoolean("haspermission", false));
        setPermission(s.getString("permission", "rpgitem.item." + name));
        // Recipes
        setRecipeChance(s.getInt("recipechance", 6));
        setHasRecipe(s.getBoolean("hasRecipe", false));
        if (isHasRecipe()) {
            setRecipe(s.getList("recipe").stream()
                       .map(i -> i instanceof ItemStack ? Pair.of(null, (ItemStack) i) : Pair.of(i, (ItemStack) null))
                       .map(p -> Optional.ofNullable(p.getValue())
                                         .orElseThrow(() -> new IllegalArgumentException("Bad itemstack " + p.getKey())))
                       .collect(Collectors.toList()));
        }

        ConfigurationSection drops = s.getConfigurationSection("dropChances");
        if (drops != null) {
            Map<String, Double> dropChances = getDropChances();
            for (String key : drops.getKeys(false)) {
                double chance = drops.getDouble(key, 0.0);
                chance = Math.min(chance, 100.0);
                if (chance > 0) {
                    dropChances.put(key, chance);
                    if (!Events.drops.containsKey(key)) {
                        Events.drops.put(key, new HashSet<>());
                    }
                    Set<Integer> set = Events.drops.get(key);
                    set.add(getUid());
                } else {
                    dropChances.remove(key);
                    if (Events.drops.containsKey(key)) {
                        Set<Integer> set = Events.drops.get(key);
                        set.remove(getUid());
                    }
                }
                dropChances.put(key, chance);
            }
        }
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
            for (String enchName : enchConf.getKeys(false)) {
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
        setNumericBar(s.getBoolean("numericBar", plugin.cfg.numericBar));
        String damageModeStr = s.getString("damageMode", "FIXED");
        try {
            setDamageMode(DamageMode.valueOf(damageModeStr));
        } catch (IllegalArgumentException e) {
            setDamageMode(DamageMode.FIXED);
        }
        rebuild();
        String lore = s.getString("lore");
        if (!Strings.isNullOrEmpty(lore)) {
            getTooltipLines();
            @SuppressWarnings("deprecation") List<String> lores = Utils.wrapLines(String.format("%s%s\"%s\"", ChatColor.YELLOW, ChatColor.ITALIC,
                    ChatColor.translateAlternateColorCodes('&', lore)), tooltipWidth);
            getDescription().addAll(0, lores);
            rebuild();
        }
    }

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
        s.set("type", getType().replaceAll("" + COLOR_CHAR, "&"));
        s.set("hand", getHand().replaceAll("" + COLOR_CHAR, "&"));
        ArrayList<String> descriptionConv = new ArrayList<>(getDescription());
        for (int i = 0; i < descriptionConv.size(); i++) {
            descriptionConv.set(i, descriptionConv.get(i).replaceAll("" + COLOR_CHAR, "&"));
        }
        s.set("description", descriptionConv);
        s.set("item", getItem().toString());
        s.set("ignoreWorldGuard", isIgnoreWorldGuard());

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
            pConfig.set("powerName", getPowerKey(p).toString());
            p.save(pConfig);
            powerConfigs.set(Integer.toString(i), pConfig);
            i++;
        }

        // Recipes
        s.set("recipechance", getRecipeChance());
        s.set("hasRecipe", isHasRecipe());
        if (isHasRecipe()) {
            s.set("recipe", getRecipe());
            s.set("namespacedKey", getNamespacedKey().getKey());
        }

        ConfigurationSection drops = s.createSection("dropChances");
        for (String key : getDropChances().keySet()) {
            drops.set(key, getDropChances().get(key));
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
        s.set("numericBar", isNumericBar());
    }

    public void updateItem(ItemStack item) {
        List<String> reservedLores = this.filterLores(item);
        item.setType(getItem());
        ItemMeta meta = item.getItemMeta();
        List<String> lore = getLore();
        @SuppressWarnings("deprecation")
        CustomItemTagContainer itemTagContainer = item.getItemMeta().getCustomTagContainer();
        SubItemTagContainer rpgitemsTagContainer = makeTag(itemTagContainer, TAG_META);
        addDurabilityBar(rpgitemsTagContainer, lore);
        // Patch for mcMMO buff. See SkillUtils.java#removeAbilityBuff in mcMMO
        if (item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().contains("mcMMO Ability Tool"))
            lore.add("mcMMO Ability Tool");
        lore.addAll(reservedLores);
        meta.setLore(lore);
        if (isCustomItemModel() || hasPower(PowerUnbreakable.class)) {
            meta.setUnbreakable(true);
        } else {
            meta.setUnbreakable(false);
        }
        meta.removeItemFlags(meta.getItemFlags().toArray(new ItemFlag[0]));

        for (ItemFlag flag : getItemFlags()) {
            if (flag == ItemFlag.HIDE_ATTRIBUTES && hasPower(PowerAttributeModifier.class)) {
                continue;
            }
            meta.addItemFlags(flag);
        }
        Set<Enchantment> enchs = meta.getEnchants().keySet();
        for (Enchantment e : enchs) {
            meta.removeEnchant(e);
        }
        Map<Enchantment, Integer> enchantMap = getEnchantMap();
        if (enchantMap != null) {
            for (Enchantment e : enchantMap.keySet()) {
                meta.addEnchant(e, enchantMap.get(e), true);
            }
        }
        Damageable damageable = (Damageable) meta;
        if (getMaxDurability() > 0) {
            int durability = computeIfAbsent(rpgitemsTagContainer, TAG_DURABILITY, ItemTagType.INTEGER, this::getDefaultDurability);
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
        rpgitemsTagContainer.commit();
        item.setItemMeta(refreshAttributeModifiers(meta));
    }

    private void addDurabilityBar(CustomItemTagContainer meta, List<String> lore) {
        if (getMaxDurability() > 0) {
            int durability = computeIfAbsent(meta, TAG_DURABILITY, ItemTagType.INTEGER, this::getDefaultDurability);
            if (isHasDurabilityBar()) {
                StringBuilder out = new StringBuilder();
                char boxChar = '\u25A0';
                double ratio = (double) durability / (double) getMaxDurability();
                if (isNumericBar()) {
                    out.append(ChatColor.GREEN.toString()).append(boxChar).append(" ");
                    out.append(ratio < 0.1 ? ChatColor.RED : ratio < 0.3 ? ChatColor.YELLOW : ChatColor.GREEN);
                    out.append(durability);
                    out.append(ChatColor.RESET).append(" / ").append(ChatColor.AQUA);
                    out.append(getMaxDurability());
                    out.append(ChatColor.GREEN).append(boxChar);
                } else {
                    int boxCount = tooltipWidth / 6;
                    int mid = (int) ((double) boxCount * (ratio));
                    for (int i = 0; i < boxCount; i++) {
                        out.append(i < mid ? ChatColor.GREEN : i == mid ? ChatColor.YELLOW : ChatColor.RED);
                        out.append(boxChar);
                    }
                }
                if (!lore.get(lore.size() - 1).contains(boxChar + ""))
                    lore.add(out.toString());
                else
                    lore.set(lore.size() - 1, out.toString());
            }
        }
    }

    private List<String> filterLores(ItemStack i) {
        List<String> ret = new ArrayList<>();
        List<PowerLoreFilter> patterns = getPower(PowerLoreFilter.class).stream()
                                                                        .filter(p -> !Strings.isNullOrEmpty(p.regex))
                                                                        .map(PowerLoreFilter::compile)
                                                                        .collect(Collectors.toList());
        if (patterns.isEmpty()) return Collections.emptyList();
        if (!i.hasItemMeta() || !i.getItemMeta().hasLore()) return Collections.emptyList();
        for (String str : i.getItemMeta().getLore()) {
            for (PowerLoreFilter p : patterns) {
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
        List<PowerAttributeModifier> attributeModifiers = getPower(PowerAttributeModifier.class);
        if (!attributeModifiers.isEmpty()) {
            Multimap<Attribute, AttributeModifier> old = itemMeta.getAttributeModifiers();
            for (PowerAttributeModifier attributeModifier : attributeModifiers) {
                Attribute attribute = attributeModifier.attribute;
                UUID uuid = new UUID(attributeModifier.uuidMost, attributeModifier.uuidLeast);
                AttributeModifier modifier = new AttributeModifier(
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

    public void resetRecipe(boolean removeOld) {
        boolean hasOldRecipe = false;
        if (removeOld) {
            Iterator<Recipe> it = Bukkit.recipeIterator();
            while (it.hasNext()) {
                Recipe recipe = it.next();
                RPGItem rpgitem = ItemManager.toRPGItem(recipe.getResult());
                if (rpgitem == null)
                    continue;
                if (rpgitem.getUid() == getUid()) {
                    hasOldRecipe = true;
                }
            }
        }
        if (isHasRecipe()) {
            if (getNamespacedKey() == null || hasOldRecipe) {
                setNamespacedKey(new NamespacedKey(RPGItems.plugin, "item_" + getUid()));
            }
            ShapedRecipe shapedRecipe = new ShapedRecipe(getNamespacedKey(), toItemStack());

            Map<ItemStack, Character> charMap = new HashMap<>();
            int i = 0;
            for (ItemStack s : getRecipe()) {
                if (!charMap.containsKey(s)) {
                    charMap.put(s, (char) (65 + (i++)));
                }
            }

            StringBuilder shape = new StringBuilder();
            for (ItemStack m : getRecipe()) {
                shape.append(charMap.get(m));
            }
            shapedRecipe.shape(shape.substring(0, 3), shape.substring(3, 6), shape.substring(6, 9));

            for (Entry<ItemStack, Character> e : charMap.entrySet()) {
                if (e.getKey() != null) {
                    shapedRecipe.setIngredient(e.getValue(), e.getKey().getData());
                }
            }
            try {
                Bukkit.addRecipe(shapedRecipe);
            } catch (IllegalStateException exception) {
                plugin.getLogger().log(Level.INFO, "Error adding recipe. It's ok when reloading plugin", exception);
            }
        }
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
        if (ItemManager.canUse(p, this) == Event.Result.DENY|| hasPower(PowerRangedOnly.class)) {
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
            return originDamage;
        }
        List<PowerRanged> ranged = getPower(PowerRanged.class, true);
        if (!ranged.isEmpty()) {
            double distance = p.getLocation().distance(entity.getLocation());
            if (ranged.get(0).rm > distance || distance > ranged.get(0).r) {
                return -1;
            }
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
                if (damager.hasMetadata("rpgitems.force")) {
                    damage *= damager.getMetadata("rpgitems.force").get(0).asFloat();
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

    private <TEvent extends Event, TPower extends Power, TResult, TReturn> boolean triggerPreCheck(Player player, ItemStack i, TEvent event, Trigger<TEvent, TPower, TResult, TReturn> trigger, List<TPower> powers) {
        if (i.getType().equals(Material.AIR)) return false;
        if (powers.isEmpty()) return false;
        if (checkPermission(player, true) == Event.Result.DENY) return false;
        if (WGSupport.canUse(player, this, powers, plugin.cfg.wgShowWarning) == Event.Result.DENY) return false;

        RPGItemsPowersPreFireEvent<TEvent, TPower, TResult, TReturn> preFire = new RPGItemsPowersPreFireEvent<>(player, i, event, this, trigger, powers);
        Bukkit.getServer().getPluginManager().callEvent(preFire);
        return !preFire.isCancelled();
    }

    @SuppressWarnings("unchecked")
    private <T> PowerResult<T> checkConditions(Player player, ItemStack i, Power power, List<PowerCondition> conds, Map<Power, PowerResult> context) {
        Set<String> ids = power.getConditions();
        List<PowerCondition> conditions = conds.stream().filter(p -> ids.contains(p.id())).collect(Collectors.toList());
        List<PowerCondition> failed = conditions.stream().filter(p -> p.isStatic() ? !context.get(p).isOK() : !p.check(player, i, context).isOK()).collect(Collectors.toList());
        if (failed.isEmpty()) return null;
        return failed.stream().anyMatch(PowerCondition::isCritical) ? PowerResult.abort() : PowerResult.condition();
    }

    @SuppressWarnings("unchecked")
    private Map<PowerCondition, PowerResult> checkStaticCondition(Player player, ItemStack i, List<PowerCondition> conds) {
        Set<String> ids = powers.stream().flatMap(p -> p.getConditions().stream()).collect(Collectors.toSet());
        List<PowerCondition> statics = conds.stream().filter(PowerCondition::isStatic).filter(p -> ids.contains(p.id())).collect(Collectors.toList());
        Map<PowerCondition, PowerResult> result = new LinkedHashMap<>();
        for (PowerCondition c : statics) {
            result.put(c, c.check(player, i, result));
        }
        return result;
    }

    public <TEvent extends Event, TPower extends Power, TResult, TReturn> TReturn power(Player player, ItemStack i, TEvent event, Trigger<TEvent, TPower, TResult, TReturn> trigger) {
        List<TPower> powers = this.getPower(trigger);
        TReturn ret = trigger.def(player, i, event);
        if (!triggerPreCheck(player, i, event, trigger, powers)) return ret;
        List<PowerCondition> conds = getPower(PowerCondition.class, true);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (TPower power : powers) {
            PowerResult<TResult> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = trigger.run(power, player, i, event);
                resultMap.put(power, result);
            }
            ret = trigger.next(ret, result);
            if (result.isAbort()) break;
        }
        triggerPostFire(player, i, event, trigger, resultMap, ret);
        return ret;
    }

    private <TEvent extends Event, TPower extends Power, TResult, TReturn> void triggerPostFire(Player player, ItemStack itemStack, TEvent event, Trigger<TEvent, TPower, TResult, TReturn> trigger, Map<Power, PowerResult> resultMap, TReturn ret) {
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
            armorMinLen = Utils.getStringWidth(ChatColor.stripColor(getHand() + "     " + getType()));

            if (getArmour() != 0) {
                damageStr = getArmour() + "% " + I18n.format("item.armour");
            }
            if ((getDamageMin() != 0 || getDamageMax() != 0) && getDamageMode() != DamageMode.VANILLA) {
                damageStr = damageStr == null ? "" : damageStr + " & ";
                if (getDamageMode() == DamageMode.ADDITIONAL) {
                    damageStr += I18n.format("item.additionaldamage", getDamageMin() == getDamageMax() ? String.valueOf(getDamageMin()) : getDamageMin() + "-" + getDamageMax());
                } else if (getDamageMode() == DamageMode.MULTIPLY) {
                    damageStr += I18n.format("item.multiplydamage", getDamageMin() == getDamageMax() ? String.valueOf(getDamageMin()) : getDamageMin() + "-" + getDamageMax());
                } else {
                    damageStr += I18n.format("item.damage", getDamageMin() == getDamageMax() ? String.valueOf(getDamageMin()) : getDamageMin() + "-" + getDamageMax());
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
            output.add(1, ChatColor.WHITE + getHand() + StringUtils.repeat(" ", (width - Utils.getStringWidth(ChatColor.stripColor(getHand() + getType()))) / 4) + getType());
        }

        return output;
    }

    public ItemStack toItemStack() {
        ItemStack rStack = new ItemStack(getItem());
        ItemMeta meta = rStack.getItemMeta();
        @SuppressWarnings("deprecation") CustomItemTagContainer itemTagContainer = meta.getCustomTagContainer();
        SubItemTagContainer rpgitemsTagContainer = makeTag(itemTagContainer, TAG_META);
        set(rpgitemsTagContainer, TAG_ITEM_UID, getUid());
        rpgitemsTagContainer.commit();
        meta.setDisplayName(getDisplayName());
        rStack.setItemMeta(meta);
        updateItem(rStack);
        return rStack;
    }

    public Event.Result checkPermission(Player p, boolean showWarn) {
        if (isHasPermission() && !p.hasPermission(getPermission())) {
            if (showWarn) p.sendMessage(I18n.format("message.error.permission", getDisplayName()));
            return Event.Result.DENY;
        }
        return Event.Result.ALLOW;
    }

    public void print(CommandSender sender) {
        String author = this.getAuthor();
        BaseComponent authorComponent = new TextComponent(author);
        try {
            UUID uuid = UUID.fromString(this.getAuthor());
            OfflinePlayer authorPlayer = Bukkit.getOfflinePlayer(uuid);
            author = authorPlayer.getName();
            authorComponent = Handler.getAuthorComponent(uuid, authorPlayer, author);
        } catch (IllegalArgumentException ignored) {
        }

        if (sender instanceof Player) {
            new Message("")
                    .append(I18n.format("message.item.print"), toItemStack())
                    .send(sender);
        } else {
            List<String> lines = getTooltipLines();
            for (String line : lines) {
                sender.sendMessage(line);
            }
        }

        new Message("").append(I18n.format("message.print.author"), Collections.singletonMap("{author}", authorComponent)).send(sender);
        new Message(I18n.format("message.print.license", getLicense())).send(sender);
        new Message(I18n.format("message.print.note", getNote())).send(sender);

        sender.sendMessage(I18n.format("message.durability.info", getMaxDurability(), getDefaultDurability(), getDurabilityLowerBound(), getDurabilityUpperBound()));
        if (isCustomItemModel()) {
            sender.sendMessage(I18n.format("message.print.customitemmodel", getItem().name() + ":" + getDataValue()));
        }
        if (!getItemFlags().isEmpty()) {
            StringBuilder str = new StringBuilder();
            for (ItemFlag flag : getItemFlags()) {
                if (str.length() > 0) {
                    str.append(", ");
                }
                str.append(flag.name());
            }
            sender.sendMessage(I18n.format("message.print.itemflags") + str);
        }
    }

    public void setItemStackDurability(ItemStack item, int val) {
        ItemMeta itemMeta = item.getItemMeta();
        SubItemTagContainer tagContainer = makeTag(itemMeta, TAG_META);
        if (getMaxDurability() != -1) {
            set(tagContainer, TAG_DURABILITY, val);
        }
        tagContainer.commit();
        item.setItemMeta(itemMeta);
        this.updateItem(item);
    }

    public Optional<Integer> getItemStackDurability(ItemStack item) {
        if (getMaxDurability() != -1) {
            return Optional.empty();
        }
        ItemMeta itemMeta = item.getItemMeta();
        SubItemTagContainer tagContainer = makeTag(itemMeta, TAG_META);
        int durability = computeIfAbsent(tagContainer, TAG_DURABILITY, ItemTagType.INTEGER, this::getDefaultDurability);
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
            SubItemTagContainer tagContainer = makeTag(itemMeta, TAG_META);
            durability = computeIfAbsent(tagContainer, TAG_DURABILITY, ItemTagType.INTEGER, this::getDefaultDurability);
            if (checkbound && (
                    (val > 0 && durability < getDurabilityLowerBound()) ||
                            (val < 0 && durability > getDurabilityUpperBound())
            )) {
                tagContainer.commit();
                item.setItemMeta(itemMeta);
                return false;
            }
            if (durability <= val
                        && hasPower(PowerUnbreakable.class)
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
        }
        item.setItemMeta(itemMeta);
        this.updateItem(item);
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

    public <T extends Power> List<T> getPower(NamespacedKey key, Class<T> power) {
        return powers.stream().filter(p -> p.getClass().equals(power) && getPowerKey(p).equals(key)).map(power::cast).collect(Collectors.toList());
    }

    public <T extends Power> List<T> getPower(Class<T> power, boolean subclass) {
        return subclass ? powers.stream().filter(power::isInstance).map(power::cast).collect(Collectors.toList()) : getPower(power);
    }

    public void addPower(NamespacedKey key, Power power) {
        addPower(key, power, true);
    }

    private void addPower(NamespacedKey key, Power power, boolean update) {
        powers.add(power);
        powerKeys.put(power, key);
        if (update) {
            rebuild();
        }
    }

    public void removePower(Power power) {
        powers.remove(power);
        powerKeys.remove(power);
        power.deinit();
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

    public BaseComponent getComponent() {
        BaseComponent msg = new TextComponent(getDisplayName());
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/rpgitem " + getName()));
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                new BaseComponent[]{new TextComponent(ItemStackUtils.itemToJson(toItemStack()))});
        msg.setHoverEvent(hover);
        return msg;
    }

    private <TEvent extends Event, T extends Power, TResult, TReturn> List<T> getPower(Trigger<TEvent, T, TResult, TReturn> trigger) {
        return powers.stream().filter(p -> p.getTriggers().contains(trigger)).map(p -> p.cast(trigger.getPowerClass())).collect(Collectors.toList());
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
        this.displayName = ChatColor.translateAlternateColorCodes('&', displayName);
    }

    public Map<String, Double> getDropChances() {
        return dropChances;
    }

    public void setDropChances(Map<String, Double> dropChances) {
        this.dropChances = dropChances;
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

    public String getHand() {
        return hand;
    }

    public void setHand(String h) {
        setHand(h, true);
    }

    public void setHand(String h, boolean update) {
        hand = ChatColor.translateAlternateColorCodes('&', h);
        if (update) {
            rebuild();
        }
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

    @Deprecated
    public int getId() {
        return id;
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
        return lore;
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

    public int getPluginSerial() {
        return pluginSerial;
    }

    public void setPluginSerial(int pluginSerial) {
        this.pluginSerial = pluginSerial;
    }

    public List<Power> getPowers() {
        return powers;
    }

    public NamespacedKey getPowerKey(Power power) {
        return Objects.requireNonNull(powerKeys.get(power));
    }

    public List<ItemStack> getRecipe() {
        return recipe;
    }

    public void setRecipe(List<ItemStack> recipe) {
        this.recipe = recipe;
    }

    public int getRecipeChance() {
        return recipeChance;
    }

    public void setRecipeChance(int p) {
        recipeChance = p;
    }

    public int getTooltipWidth() {
        return tooltipWidth;
    }

    public String getType() {
        return type;
    }

    public void setType(String str) {
        setType(str, true);
    }

    public void setType(String str, boolean update) {
        type = ChatColor.translateAlternateColorCodes('&', str);
        if (update)
            rebuild();
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

    public boolean isHasRecipe() {
        return hasRecipe;
    }

    public void setHasRecipe(boolean hasRecipe) {
        this.hasRecipe = hasRecipe;
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

    public boolean isNumericBar() {
        return numericBar;
    }

    public void setNumericBar(boolean numericBar) {
        this.numericBar = numericBar;
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

    @LangKey(type = LangKeyType.SUFFIX)
    public enum DamageMode {
        FIXED,
        VANILLA,
        ADDITIONAL,
        MULTIPLY,
    }

    @Deprecated
    private enum Quality {
        TRASH(ChatColor.GRAY.toString(), "7"), COMMON(ChatColor.WHITE.toString(), "f"), UNCOMMON(ChatColor.GREEN.toString(), "a"), RARE(ChatColor.BLUE.toString(), "9"), EPIC(ChatColor.DARK_PURPLE.toString(), "5"), LEGENDARY(ChatColor.GOLD.toString(), "6");

        public final String colour;
        public final String cCode;

        Quality(String colour, String code) {
            this.colour = colour;
            this.cCode = code;
        }
    }
}
