package think.rpgitems.item;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import com.google.common.base.Strings;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.librazy.nclangchecker.LangKey;
import org.librazy.nclangchecker.LangKeyType;
import think.rpgitems.Events;
import think.rpgitems.Handler;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.data.RPGMetadata;
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

public class RPGItem {
    public static final int MC_ENCODED_ID_LENGTH = 16;
    static RPGItems plugin;
    private boolean ignoreWorldGuard = false;
    private List<String> description = new ArrayList<>();
    private boolean showPowerLore = true;
    private boolean showArmourLore = true;
    private Map<Enchantment, Integer> enchantMap = null;
    private List<ItemFlag> itemFlags = new ArrayList<>();
    private boolean customItemModel = false;
    private boolean numericBar = plugin.cfg.numericBar;
    // Powers
    private List<Power> powers = new ArrayList<>();
    private HashMap<Power, NamespacedKey> powerKeys = new HashMap<>();
    // Recipes
    private int recipechance = 6;
    private boolean hasRecipe = false;
    private List<ItemStack> recipe = null;
    // Drops
    private Map<String, Double> dropChances = new HashMap<>();
    private int defaultDurability;
    private int durabilityLowerBound;
    private int durabilityUpperBound;

    private int blockBreakingCost = 1;
    private int hittingCost = 1;
    private int hitCost = 1;
    private boolean hitCostByDamage = false;
    private DamageMode damageMode = DamageMode.FIXED;
    private File file;

    private NamespacedKey namespacedKey;
    private ItemStack item;
    private ItemMeta localeMeta;
    private int id;
    private int uid;
    private String name;
    private String encodedUID;
    private boolean haspermission;
    private String permission;
    private String displayName;
    private int damageMin = 0, damageMax = 3;
    private int armour = 0;
    private String type = I18n.format("item.type");
    private String hand = I18n.format("item.hand");

    private String author = plugin.cfg.defaultAuthor;
    private String note = plugin.cfg.defaultNote;
    private String license = plugin.cfg.defaultLicense;

    private int tooltipWidth = 150;
    // Durability
    private int maxDurability = -1;
    private boolean hasBar = false;
    private boolean forceBar = plugin.cfg.forceBar;
    private String mcVersion;
    private int pluginSerial;

    public RPGItem(String name, int uid, CommandSender author) {
        this.name = name;
        this.uid = uid;
        this.author = author instanceof Player ? ((Player) author).getUniqueId().toString() : plugin.cfg.defaultAuthor;
        encodedUID = getMCEncodedUID(uid);
        item = new ItemStack(Material.WOODEN_SWORD);
        hasBar = true;

        displayName = item.getType().toString();

        localeMeta = item.getItemMeta();
        itemFlags.add(ItemFlag.HIDE_ATTRIBUTES);
        mcVersion = RPGItems.getServerMCVersion();
        pluginSerial = RPGItems.getSerial();
        rebuild();
    }

    public RPGItem(ConfigurationSection s, File f) throws UnknownPowerException, UnknownExtensionException {
        file = f;
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

    private static RPGMetadata getMetadata(ItemStack item) {
        // Check for broken item
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore() || item.getItemMeta().getLore().size() == 0) {
            // Broken item
            return new RPGMetadata();
        }
        return RPGMetadata.parseLoreline(item.getItemMeta().getLore().get(0));
    }

    public static void updateItem(ItemStack item) {
        RPGItem rItem = ItemManager.toRPGItem(item);
        if (rItem == null)
            return;
        updateItem(rItem, item, getMetadata(item));
    }

    public static void updateItem(RPGItem rItem, ItemStack item) {
        updateItem(rItem, item, getMetadata(item));
    }

    public static void updateItem(RPGItem rItem, ItemStack item, RPGMetadata rpgMeta) {
        List<String> reservedLores = filterLores(rItem, item);
        item.setType(rItem.item.getType());
        ItemMeta meta = rItem.getLocaleMeta();
        List<String> lore = meta.getLore();
        rItem.addExtra(rpgMeta, lore);
        lore.set(0, meta.getLore().get(0) + rpgMeta.toMCString());
        // Patch for mcMMO buff. See SkillUtils.java#removeAbilityBuff in mcMMO
        if (item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().contains("mcMMO Ability Tool"))
            lore.add("mcMMO Ability Tool");
        lore.addAll(reservedLores);
        meta.setLore(lore);
        Map<Enchantment, Integer> enchs = item.getEnchantments();
        if (!enchs.isEmpty()) {
            for (Enchantment ench : enchs.keySet()) {
                meta.addEnchant(ench, enchs.get(ench), true);
            }
        }
        item.setItemMeta(meta);
        item.setItemMeta(refreshAttributeModifiers(rItem, item).getItemMeta());
        Damageable damageable = (Damageable) item.getItemMeta();
        if (rItem.maxDurability > 0) {
            int durability = ((Number) rpgMeta.get(RPGMetadata.DURABILITY)).intValue();
            if (rItem.isCustomItemModel()) {
                damageable.setDamage(((Damageable) rItem.localeMeta).getDamage());
            } else {
                damageable.setDamage((rItem.item.getType().getMaxDurability() - ((short) ((double) rItem.item.getType().getMaxDurability() * ((double) durability / (double) rItem.maxDurability)))));
            }
        } else {
            if (rItem.isCustomItemModel()) {
                damageable.setDamage(((Damageable) rItem.localeMeta).getDamage());
            } else {
                damageable.setDamage(rItem.hasBar ? 0 : ((Damageable) rItem.localeMeta).getDamage());
            }
        }
        item.setItemMeta((ItemMeta) damageable);
    }

    private static List<String> filterLores(RPGItem r, ItemStack i) {
        List<String> ret = new ArrayList<>();
        List<PowerLoreFilter> patterns = r.getPower(PowerLoreFilter.class).stream()
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

    public static int decodeId(String str) throws NumberFormatException {
        if (str.length() < 16) {
            return -1;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (str.charAt(i) != COLOR_CHAR) {
                throw new IllegalArgumentException();
            }
            i++;
            out.append(str.charAt(i));
        }
        return Integer.parseUnsignedInt(out.toString(), 16);
    }

    public static RPGItems getPlugin() {
        return plugin;
    }

    private static ItemStack refreshAttributeModifiers(RPGItem item, ItemStack rStack) {
        List<PowerAttributeModifier> attributeModifiers = item.getPower(PowerAttributeModifier.class);
        ItemMeta itemMeta = rStack.getItemMeta();
        if (!attributeModifiers.isEmpty()) {
            for (PowerAttributeModifier attributeModifier : attributeModifiers) {
                Attribute attribute = attributeModifier.attribute;
                AttributeModifier modifier = new AttributeModifier(
                        new UUID(attributeModifier.uuidMost, attributeModifier.uuidLeast),
                        attributeModifier.name,
                        attributeModifier.amount,
                        attributeModifier.operation,
                        attributeModifier.slot
                );
                itemMeta.addAttributeModifier(attribute, modifier);
            }
        }
        rStack.setItemMeta(itemMeta);
        return rStack;
    }

    private void restore(ConfigurationSection s) throws UnknownPowerException, UnknownExtensionException {
        author = s.getString("author", "");
        note = s.getString("note", "");
        license = s.getString("license", "");
        pluginSerial = s.getInt("pluginSerial", 0);
        mcVersion = s.getString("mcVersion", "");

        String display = s.getString("display");

        @SuppressWarnings("deprecation") Quality quality = s.isString("quality") ? Quality.valueOf(s.getString("quality")) : null;
        if (quality != null) {
            display = quality.colour + ChatColor.BOLD + display;
        }

        displayName = ChatColor.translateAlternateColorCodes('&', display);
        setType(s.getString("type", I18n.format("item.type")), false);
        hand = ChatColor.translateAlternateColorCodes('&', s.getString("hand", I18n.format("item.hand")));
        description = s.getStringList("description");
        for (int i = 0; i < description.size(); i++) {
            description.set(i, ChatColor.translateAlternateColorCodes('&', description.get(i)));
        }
        damageMin = s.getInt("damageMin");
        damageMax = s.getInt("damageMax");
        armour = s.getInt("armour", 0);
        String materialName = s.getString("item");
        Material material = MaterialUtils.getMaterial(materialName, Bukkit.getConsoleSender());
        item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(Color.fromRGB(s.getInt("item_colour", 0)));
        } else {
            ((Damageable) meta).setDamage(s.getInt("item_data", 0));
        }
        localeMeta = meta.clone();
        ignoreWorldGuard = s.getBoolean("ignoreWorldGuard", false);

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
        if (plugin.cfg.pidCompat && id > 0) {
            encodedUID = getMCEncodedUID(id);
        } else {
            encodedUID = getMCEncodedUID(uid);
        }

        haspermission = s.getBoolean("haspermission", false);
        permission = s.getString("permission", "rpgitem.item." + name);
        // Recipes
        recipechance = s.getInt("recipechance", 6);
        hasRecipe = s.getBoolean("hasRecipe", false);
        if (hasRecipe) {
            recipe = s.getList("recipe").stream()
                      .map(i -> i instanceof ItemStack ? Pair.of(null, (ItemStack) i) : Pair.of(i, (ItemStack) null))
                      .map(p -> Optional.ofNullable(p.getValue())
                                        .orElseThrow(() -> new IllegalArgumentException("Bad itemstack " + p.getKey())))
                      .collect(Collectors.toList());
            namespacedKey = new NamespacedKey(RPGItems.plugin, s.getString("namespacedKey", name + "_" + uid));
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
                    set.add(getUID());
                } else {
                    dropChances.remove(key);
                    if (Events.drops.containsKey(key)) {
                        Set<Integer> set = Events.drops.get(key);
                        set.remove(getUID());
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
        if (defaultDurability <= 0) {
            defaultDurability = maxDurability;
        }
        durabilityLowerBound = s.getInt("durabilityLowerBound", 0);
        durabilityUpperBound = s.getInt("durabilityUpperBound", item.getType().getMaxDurability());
        forceBar = s.getBoolean("forceBar", plugin.cfg.forceBar);

        if (maxDurability == 0) {
            maxDurability = -1;
        }

        if (defaultDurability <= 0) {
            defaultDurability = maxDurability > 0 ? maxDurability : 0;
        }

        showPowerLore = s.getBoolean("showPowerText", true);
        showArmourLore = s.getBoolean("showArmourLore", true);

        if (s.isConfigurationSection("enchantments")) {
            ConfigurationSection enchConf = s.getConfigurationSection("enchantments");
            enchantMap = new HashMap<>();
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
                    enchantMap.put(ench, enchConf.getInt(enchName));
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
                    plugin.getLogger().log(Level.WARNING, "Ignoring unknown item flags", e);
                }
            }
        }
        customItemModel = s.getBoolean("customItemModel", false);
        numericBar = s.getBoolean("numericBar", plugin.cfg.numericBar);
        String damageModeStr = s.getString("damageMode", "FIXED");
        try {
            damageMode = DamageMode.valueOf(damageModeStr);
        } catch (IllegalArgumentException e) {
            damageMode = DamageMode.FIXED;
        }
        rebuild();
        String lore = s.getString("lore");
        if (!Strings.isNullOrEmpty(lore)) {
            getTooltipLines();
            @SuppressWarnings("deprecation") List<String> lores = Utils.wrapLines(String.format("%s%s\"%s\"", ChatColor.YELLOW, ChatColor.ITALIC,
                    ChatColor.translateAlternateColorCodes('&', lore)), tooltipWidth);
            description.addAll(0, lores);
        }
    }

    public void save(ConfigurationSection s) {
        s.set("name", name);
        if (id != 0) {
            s.set("id", id);
        }
        s.set("uid", uid);

        s.set("author", author);
        s.set("note", note);
        s.set("license", license);

        s.set("mcVersion", mcVersion);
        s.set("pluginSerial", pluginSerial);

        s.set("haspermission", haspermission);
        s.set("permission", permission);
        s.set("display", displayName.replaceAll("" + COLOR_CHAR, "&"));
        s.set("damageMin", damageMin);
        s.set("damageMax", damageMax);
        s.set("armour", armour);
        s.set("type", type.replaceAll("" + COLOR_CHAR, "&"));
        s.set("hand", hand.replaceAll("" + COLOR_CHAR, "&"));
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
        } else if (meta instanceof Damageable) {
            s.set("item_data", ((Damageable) localeMeta).getDamage());
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
                ench.set(e.getKey().getKey(), enchantMap.get(e));
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
        s.set("numericBar", numericBar);
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
                if (rpgitem.getUID() == getUID()) {
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
        if (ItemManager.canNotUse(p, this) || hasPower(PowerRangedOnly.class)) {
            return -1;
        }
        boolean can = consumeDurability(stack, hittingCost);
        if (!can) {
            return -1;
        }
        switch (damageMode) {
            case MULTIPLY:
            case FIXED:
            case ADDITIONAL:
                damage = getDamageMin() != getDamageMax() ? (getDamageMin() + ThreadLocalRandom.current().nextInt(getDamageMax() - getDamageMin() + 1)) : getDamageMin();

                if (damageMode == DamageMode.MULTIPLY) {
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

                if (damageMode == DamageMode.ADDITIONAL) {
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
        if (ItemManager.canNotUse(p, this)) {
            return originDamage;
        }
        List<PowerRanged> ranged = getPower(PowerRanged.class, true);
        if (!ranged.isEmpty()) {
            double distance = p.getLocation().distance(entity.getLocation());
            if (ranged.get(0).rm > distance || distance > ranged.get(0).r) {
                return -1;
            }
        }
        switch (damageMode) {
            case FIXED:
            case ADDITIONAL:
            case MULTIPLY:
                damage = getDamageMin() != getDamageMax() ? (getDamageMin() + ThreadLocalRandom.current().nextInt(getDamageMax() - getDamageMin() + 1)) : getDamageMin();

                if (damageMode == DamageMode.MULTIPLY) {
                    damage *= originDamage;
                    break;
                }

                //Apply force adjustments
                if (damager.hasMetadata("rpgitems.force")) {
                    damage *= damager.getMetadata("rpgitems.force").get(0).asFloat();
                }
                if (damageMode == DamageMode.ADDITIONAL) {
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
        if (ItemManager.canNotUse(p, this)) {
            return originDamage;
        }
        boolean can;
        if (!hitCostByDamage) {
            can = consumeDurability(stack, hitCost);
        } else {
            can = consumeDurability(stack, (int) (hitCost * originDamage / 100d));
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
        return consumeDurability(stack, blockBreakingCost);
    }

    private <TEvent extends Event, TPower extends Power, TResult, TReturn> boolean triggerPreCheck(Player player, ItemStack i, TEvent event, Trigger<TEvent, TPower, TResult, TReturn> trigger, List<TPower> powers) {
        if (i.getType().equals(Material.AIR)) return false;
        if (powers.isEmpty()) return false;
        if (!checkPermission(player, true)) return false;
        if (!WGSupport.canUse(player, this, powers)) return false;

        RPGItemsPowersPreFireEvent<TEvent, TPower, TResult, TReturn> preFire = new RPGItemsPowersPreFireEvent<>(player, i, event, this, trigger, powers);
        Bukkit.getServer().getPluginManager().callEvent(preFire);
        return !preFire.isCancelled();
    }

    @SuppressWarnings("unchecked")
    private <T> PowerResult<T> checkConditions(Player player, ItemStack i, Power power, List<PowerCondition> conds, Map<Power, PowerResult> context) {
        Set<String> ids = power.getConditions();
        List<PowerCondition> conditions = conds.stream().filter(p -> ids.contains(p.id())).collect(Collectors.toList());
        List<Pair<PowerCondition, PowerResult>> results = conditions.stream().map(p -> p.isStatic() ? Pair.of(p, context.get(p)) : Pair.of(p, p.check(player, i, context))).collect(Collectors.toList());
        List<Pair<PowerCondition, PowerResult>> faileds = results.stream().filter(p -> !p.getValue().isOK()).collect(Collectors.toList());
        if (faileds.isEmpty()) return null;
        for (Pair<PowerCondition, PowerResult> failed : faileds) {
            failed.getValue().sendMessage(player);
        }
        return faileds.stream().map(Pair::getKey).anyMatch(PowerCondition::isCritical) ? PowerResult.abort() : PowerResult.condition();
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

    private <TEvent extends Event, TPower extends Power, TResult, TReturn> void triggerPostFire(Player player, ItemStack i, TEvent event, Trigger<TEvent, TPower, TResult, TReturn> trigger, Map<Power, PowerResult> resultMap, TReturn ret) {
        RPGItemsPowersPostFireEvent<TEvent, TPower, TResult, TReturn> postFire = new RPGItemsPowersPostFireEvent<>(player, i, event, this, trigger, resultMap, ret);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void rebuild() {
        hasBar = item.getType().getMaxDurability() != 0;
        List<String> lines = getTooltipLines();
        ItemMeta meta = getLocaleMeta();
        meta.setDisplayName(lines.get(0));
        lines.remove(0);
        if (lines.size() > 0) {
            lines.set(0, getMCEncodedUID() + lines.get(0));
        } else {
            lines.add(0, getMCEncodedUID());
        }
        meta.setLore(lines);
        if (isCustomItemModel() || hasPower(PowerUnbreakable.class)) {
            meta.setUnbreakable(true);
        } else {
            meta.setUnbreakable(false);
        }
        for (ItemFlag flag : meta.getItemFlags()) {
            meta.removeItemFlags(flag);
        }
        for (ItemFlag flag : itemFlags) {
            if (flag == ItemFlag.HIDE_ATTRIBUTES && hasPower(PowerAttributeModifier.class)) {
                continue;
            }
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
    }

    public ItemMeta getLocaleMeta() {
        return localeMeta.clone();
    }

    private void addExtra(RPGMetadata rpgMeta, List<String> lore) {
        if (maxDurability > 0) {
            if (!rpgMeta.containsKey(RPGMetadata.DURABILITY)) {
                rpgMeta.put(RPGMetadata.DURABILITY, defaultDurability);
            }
            int durability = ((Number) rpgMeta.get(RPGMetadata.DURABILITY)).intValue();

            if (!hasBar || forceBar || isCustomItemModel()) {
                StringBuilder out = new StringBuilder();
                char boxChar = '\u25A0';
                double ratio = (double) durability / (double) maxDurability;
                if (numericBar) {
                    out.append(ChatColor.GREEN.toString()).append(boxChar).append(" ");
                    out.append(ratio < 0.1 ? ChatColor.RED : ratio < 0.3 ? ChatColor.YELLOW : ChatColor.GREEN);
                    out.append(durability);
                    out.append(ChatColor.RESET).append(" / ").append(ChatColor.AQUA);
                    out.append(maxDurability);
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

    @SuppressWarnings("deprecation")
    public List<String> getTooltipLines() {
        ArrayList<String> output = new ArrayList<>();
        output.add(getMCEncodedUID() + displayName);

        // add powerLores
        if (showPowerLore) {
            for (Power p : powers) {
                String txt = p.displayText();
                if (txt != null && txt.length() > 0) {
                    output.add(txt);
                }
            }
        }

        // add descriptions
        output.addAll(description);

        // compute width
        int width = 0;
        for (String str : output) {
            width = Math.max(width, Utils.getStringWidth(ChatColor.stripColor(str)));
        }

        // compute armorMinLen
        int armorMinLen = 0;
        String damageStr = null;
        if (showArmourLore) {
            armorMinLen = Utils.getStringWidth(ChatColor.stripColor(hand + "     " + type));

            if (armour != 0) {
                damageStr = armour + "% " + I18n.format("item.armour");
            }
            if ((damageMin != 0 || damageMax != 0) && damageMode != DamageMode.VANILLA) {
                damageStr = damageStr == null ? "" : damageStr + " & ";
                if (damageMode == DamageMode.ADDITIONAL) {
                    damageStr += I18n.format("item.additionaldamage", damageMin == damageMax ? damageMin : damageMin + "-" + damageMax);
                } else if (damageMode == DamageMode.MULTIPLY) {
                    damageStr += I18n.format("item.multiplydamage", damageMin == damageMax ? damageMin : damageMin + "-" + damageMax);
                } else {
                    damageStr += I18n.format("item.damage", damageMin == damageMax ? damageMin : damageMin + "-" + damageMax);
                }
            }
            if (damageStr != null) {
                armorMinLen = Math.max(armorMinLen, Utils.getStringWidth(ChatColor.stripColor(damageStr)));
            }
        }
        tooltipWidth = width = Math.max(width, armorMinLen);

        if (showArmourLore) {
            if (damageStr != null) {
                output.add(1, ChatColor.WHITE + damageStr);
            }
            output.add(1, ChatColor.WHITE + hand + StringUtils.repeat(" ", (width - Utils.getStringWidth(ChatColor.stripColor(hand + type))) / 4) + type);
        }

        return output;
    }

    public ItemStack toItemStack() {
        ItemStack rStack = item.clone();
        RPGMetadata rpgMeta = new RPGMetadata();
        ItemMeta meta = getLocaleMeta();
        List<String> lore = meta.getLore();
        lore.set(0, meta.getLore().get(0) + rpgMeta.toMCString());
        addExtra(rpgMeta, lore);
        meta.setLore(lore);
        rStack.setItemMeta(meta);
        refreshAttributeModifiers(this, rStack);
        updateItem(this, rStack);
        return rStack;
    }

    public String getName() {
        return name;
    }

    @Deprecated
    public int getID() {
        return id;
    }

    public int getUID() {
        return uid;
    }

    public String getMCEncodedUID() {
        return encodedUID;
    }

    public void print(CommandSender sender) {
        String author = this.author;
        BaseComponent authorComponent = new TextComponent(author);
        try {
            UUID uuid = UUID.fromString(this.author);
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
        new Message(I18n.format("message.print.license", license)).send(sender);
        new Message(I18n.format("message.print.note", note)).send(sender);

        sender.sendMessage(I18n.format("message.durability.info", getMaxDurability(), defaultDurability, durabilityLowerBound, durabilityUpperBound));
        if (isCustomItemModel()) {
            sender.sendMessage(I18n.format("message.print.customitemmodel", item.getType().name() + ":" + ((Damageable) localeMeta).getDamage()));
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

    public String getDisplay() {
        return displayName;
    }

    public void setDisplay(String str) {
        displayName = ChatColor.translateAlternateColorCodes('&', str);
        rebuild();
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

    public String getHand() {
        return hand;
    }

    public void setHand(String h) {
        hand = ChatColor.translateAlternateColorCodes('&', h);
        rebuild();
    }

    public void setDamage(int min, int max) {
        damageMin = min;
        damageMax = max;
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
        recipechance = p;
        rebuild();
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String p) {
        permission = p;
        rebuild();
    }

    public boolean getHasPermission() {
        return haspermission;
    }

    public void setHaspermission(boolean b) {
        haspermission = b;
        rebuild();
    }

    public boolean checkPermission(Player p, boolean showWarn) {
        if (getHasPermission() && !p.hasPermission(getPermission())) {
            if (showWarn) p.sendMessage(I18n.format("message.error.permission", getDisplay()));
            return false;
        }
        return true;
    }

    public int getArmour() {
        return armour;
    }

    public void setArmour(int a) {
        armour = a;
        rebuild();
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

    public int getDataValue() {
        return ((Damageable) localeMeta).getDamage();
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

    public void setDurabilityBound(int min, int max) {
        durabilityLowerBound = min;
        durabilityUpperBound = max;
    }

    public int getMaxDurability() {
        return maxDurability <= 0 ? -1 : maxDurability;
    }

    public void setMaxDurability(int newVal) {
        setDefaultDurability(newVal);
        setMaxDurability(newVal, true);
    }

    public void setDurability(ItemStack item, int val) {
        RPGMetadata meta = getMetadata(item);
        if (getMaxDurability() != -1) {
            meta.put(RPGMetadata.DURABILITY, val);
        }
        updateItem(this, item, meta);
    }

    public int getDurability(ItemStack item) {
        RPGMetadata meta = getMetadata(item);
        int durability = getDefaultDurability();
        if (getMaxDurability() != -1 && meta.containsKey(RPGMetadata.DURABILITY)) {
            durability = ((Number) meta.get(RPGMetadata.DURABILITY)).intValue();
        }
        return durability;
    }

    public boolean consumeDurability(ItemStack item, int val) {
        return consumeDurability(item, val, true);
    }

    public boolean consumeDurability(ItemStack item, int val, boolean checkbound) {
        if (val == 0) return true;
        RPGMetadata meta = getMetadata(item);
        int durability;
        if (getMaxDurability() != -1) {
            durability = meta.containsKey(RPGMetadata.DURABILITY) ? ((Number) meta.get(RPGMetadata.DURABILITY)).intValue() : getDefaultDurability();
            if (checkbound && (
                    (val > 0 && durability < durabilityLowerBound) ||
                            (val < 0 && durability > durabilityUpperBound)
            )) {
                return false;
            }
            if (durability <= val
                        && getLocaleMeta().isUnbreakable()
                        && !isCustomItemModel()) {
                return false;
            }
            durability -= val;
            if (durability > getMaxDurability()) {
                durability = getMaxDurability();
            }
            meta.put(RPGMetadata.DURABILITY, durability);
        }
        updateItem(this, item, meta);
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
        if (update)
            rebuild();
    }

    public void removePower(Power power) {
        powers.remove(power);
        powerKeys.remove(power);
        power.deinit();
        rebuild();
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

    public File getFile() {
        return file;
    }

    void setFile(File itemFile) {
        file = itemFile;
    }

    public int getTooltipWidth() {
        return tooltipWidth;
    }

    public boolean isCustomItemModel() {
        return customItemModel;
    }

    public void setCustomItemModel(boolean customItemModel) {
        this.customItemModel = customItemModel;
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

    public boolean isShowPowerLore() {
        return showPowerLore;
    }

    public void setShowPowerLore(boolean showPowerLore) {
        this.showPowerLore = showPowerLore;
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

    public boolean isNumericBar() {
        return numericBar;
    }

    public void setNumericBar(boolean numericBar) {
        this.numericBar = numericBar;
    }

    public List<Power> getPowers() {
        return powers;
    }

    public void setPowers(List<Power> powers) {
        this.powers = powers;
    }

    public int getRecipechance() {
        return recipechance;
    }

    public void setRecipechance(int recipechance) {
        this.recipechance = recipechance;
    }

    public boolean isHasRecipe() {
        return hasRecipe;
    }

    public void setHasRecipe(boolean hasRecipe) {
        this.hasRecipe = hasRecipe;
    }

    public List<ItemStack> getRecipe() {
        return recipe;
    }

    public void setRecipe(List<ItemStack> recipe) {
        this.recipe = recipe;
    }

    public Map<String, Double> getDropChances() {
        return dropChances;
    }

    public void setDropChances(Map<String, Double> dropChances) {
        this.dropChances = dropChances;
    }

    public int getDefaultDurability() {
        return defaultDurability;
    }

    public void setDefaultDurability(int newVal) {
        defaultDurability = newVal;
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

    public DamageMode getDamageMode() {
        return damageMode;
    }

    public void setDamageMode(DamageMode damageMode) {
        this.damageMode = damageMode;
    }

    public String getMCVersion() {
        return mcVersion;
    }

    public void setMCVersion(String mcVersion) {
        this.mcVersion = mcVersion;
    }

    public int getPluginSerial() {
        return pluginSerial;
    }

    public void setPluginSerial(int pluginSerial) {
        this.pluginSerial = pluginSerial;
    }

    public NamespacedKey getPowerKey(Power power) {
        return Objects.requireNonNull(powerKeys.get(power));
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
