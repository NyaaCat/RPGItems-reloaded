package think.rpgitems.item;

import cat.nyaa.nyaacore.Message;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.librazy.nclangchecker.LangKey;
import org.librazy.nclangchecker.LangKeyType;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Font;
import think.rpgitems.data.RPGMetadata;
import think.rpgitems.power.*;
import think.rpgitems.power.impl.*;
import think.rpgitems.support.WGSupport;
import think.rpgitems.utils.MaterialUtils;

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
    public List<ItemFlag> itemFlags = new ArrayList<>();
    public boolean customItemModel = false;
    // Powers
    public List<Power> powers = new ArrayList<>();
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

    String file;

    private ItemStack item;
    private ItemMeta localeMeta;
    private int id;
    private int uid;
    private String name;
    private String encodedUID;
    private boolean haspermission;
    private String permission;
    private String displayName;
    private Quality quality = Quality.TRASH;
    private int damageMin = 0, damageMax = 3;
    private int armour = 0;
    private String loreText = "";
    private String type = RPGItems.plugin.getConfig().getString("defaults.sword", "Sword");
    private String hand = RPGItems.plugin.getConfig().getString("defaults.hand", "One handed");

    private String author;
    private String note;
    private String license;

    private int tooltipWidth = 150;
    // Durability
    private int maxDurability = -1;
    private boolean hasBar = false;
    private boolean forceBar = false;
    private int _loreMinLen = 0;

    public RPGItem(String name, int uid) {
        this.name = name;
        this.uid = uid;
        encodedUID = getMCEncodedUID(uid);
        item = new ItemStack(Material.WOODEN_SWORD);
        hasBar = true;

        displayName = item.getType().toString();

        localeMeta = item.getItemMeta();
        itemFlags.add(ItemFlag.HIDE_ATTRIBUTES);
        rebuild();
    }

    public RPGItem(ConfigurationSection s) throws UnknownPowerException, UnknownExtensionException {
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

    @SuppressWarnings({"unchecked", "deprecation"})
    private void restore(ConfigurationSection s) throws UnknownPowerException, UnknownExtensionException {
        author = s.getString("author", "");
        note = s.getString("note", "");
        license = s.getString("license", "");

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
        String materialName = s.getString("item");
        Material material = MaterialUtils.getMaterial(materialName, Bukkit.getConsoleSender());
        item = new ItemStack(material);
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
            Map<Power, ConfigurationSection> conf = new HashMap<>();
            for (String sectionKey : powerList.getKeys(false)) {
                ConfigurationSection section = powerList.getConfigurationSection(sectionKey);
                try {
                    String powerName = section.getString("powerName");
                    NamespacedKey key = PowerManager.parseKey(powerName);
                    if (!PowerManager.hasPower(key)) {
                        plugin.getLogger().warning("Unknown power:" + key + " on item " + this.name);
                        throw new UnknownPowerException(key);
                    }
                    Power pow = PowerManager.getPower(key).getConstructor().newInstance();
                    pow.init(section);
                    pow.setItem(this);
                    addPower(pow, false);
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
                    return;
                }
                selector.id = UUID.randomUUID().toString();
                Set<? extends Class<? extends Power>> applicable = Arrays.stream(applyTo.split(",")).map(PowerManager::parseLegacyKey).map(PowerManager::getPower).collect(Collectors.toSet());
                for (Class<? extends Power> pow : applicable) {
                    List<? extends Power> app = getPower(pow);
                    for (Power power : app) {
                        if (power instanceof BasePower) {
                            ((BasePower) power).selectors.add(selector.id);
                        }
                    }
                }
            }
        }

        encodedUID = getMCEncodedUID(uid);
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
                Enchantment tmp = null;
                try {
                    tmp = Enchantment.getByKey(NamespacedKey.minecraft(enchName));
                } catch (IllegalArgumentException e) {
                    tmp = Enchantment.getByName(enchName);
                }
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
            if (rItem.customItemModel) {
                damageable.setDamage(((Damageable) rItem.localeMeta).getDamage());
            } else {
                damageable.setDamage((short) (rItem.item.getType().getMaxDurability() - ((short) ((double) rItem.item.getType().getMaxDurability() * ((double) durability / (double) rItem.maxDurability)))));
            }
        } else {
            if (rItem.customItemModel) {
                damageable.setDamage(((Damageable) rItem.localeMeta).getDamage());
            } else {
                damageable.setDamage(rItem.hasBar ? (short) 0 : ((Damageable) rItem.localeMeta).getDamage());
            }
        }
        item.setItemMeta((ItemMeta) damageable);
    }

    private static List<String> filterLores(RPGItem r, ItemStack i) {
        List<String> ret = new ArrayList<>();
        List<Pattern> patterns = r.getPower(PowerLoreFilter.class).stream()
                                  .filter(p -> p.regex != null)
                                  .map(p -> p.regex)
                                  .map(Pattern::compile)
                                  .collect(Collectors.toList());
        if (patterns.isEmpty()) return Collections.emptyList();
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

    public static String getMCEncodedUID(int id) {
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
        if (id != 0) {
            s.set("id", id);
        }
        s.set("uid", uid);

        s.set("author", author);
        s.set("note", note);
        s.set("license", license);

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
        } else if (meta instanceof Damageable) {
            s.set("item_data", ((Damageable) localeMeta).getDamage());
        }
        ConfigurationSection powerConfigs = s.createSection("powers");
        int i = 0;
        for (Power p : powers) {
            MemoryConfiguration pConfig = new MemoryConfiguration();
            pConfig.set("powerName", p.getNamespacedKey().toString());
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
            Bukkit.addRecipe(shapedRecipe);
        }
    }

    private boolean triggerPreCheck(Player player, ItemStack i, List<? extends Power> powers, TriggerType triggerType) {
        if (powers.isEmpty()) return false;
        if (!checkPermission(player, true)) return false;
        if (!WGSupport.check(player, this, powers)) return false;

        RPGItemsPowersPreFireEvent preFire = new RPGItemsPowersPreFireEvent(i, this, player, triggerType, powers);
        Bukkit.getServer().getPluginManager().callEvent(preFire);
        return !preFire.isCancelled();
    }

    private <T> PowerResult<T> checkConditions(Player player, ItemStack i, Power power, List<PowerCondition> conds, Map<Power, PowerResult> context) {
        Set<String> ids = power.getConditions();
        List<PowerCondition> conditions = conds.stream().filter(p -> ids.contains(p.id())).collect(Collectors.toList());
        List<PowerCondition> failed = conditions.stream().filter(p -> p.isStatic() ? !context.get(p).isOK() : !p.check(player, i, context).isOK()).collect(Collectors.toList());
        if (failed.isEmpty()) return null;
        return failed.stream().anyMatch(PowerCondition::isCritical) ? PowerResult.abort() : PowerResult.condition();
    }

    private Map<PowerCondition, PowerResult> checkStaticCondition(Player player, ItemStack i, List<PowerCondition> conds) {
        Set<String> ids = powers.stream().flatMap(p -> p.getConditions().stream()).collect(Collectors.toSet());
        List<PowerCondition> statics = conds.stream().filter(PowerCondition::isStatic).filter(p -> ids.contains(p.id())).collect(Collectors.toList());
        return statics.stream().collect(Collectors.toMap(
                s -> s,
                s -> s.check(player, i, null)
        ));
    }

    public void leftClick(Player player, ItemStack i, Block block, PlayerInteractEvent event) {
        if (!triggerPreCheck(player, i, this.getPowerLeftClick(), TriggerType.LEFT_CLICK)) return;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerLeftClick power : getPowerLeftClick()) {
            PowerResult<Void> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.leftClick(player, i, block, event);
                resultMap.put(power, result);
            }
            if (result.isAbort()) break;
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.LEFT_CLICK, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void rightClick(Player player, ItemStack i, Block block, PlayerInteractEvent event) {
        if (!triggerPreCheck(player, i, this.getPowerRightClick(), TriggerType.RIGHT_CLICK)) return;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerRightClick power : this.getPowerRightClick()) {
            PowerResult<Void> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.rightClick(player, i, block, event);
                resultMap.put(power, result);
            }
            if (result.isAbort()) break;
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.RIGHT_CLICK, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void offhandClick(Player player, ItemStack i, PlayerInteractEvent event) {
        if (!triggerPreCheck(player, i, this.getPowerOffhandClick(), TriggerType.OFFHAND_CLICK)) return;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerOffhandClick power : getPowerOffhandClick()) {
            PowerResult<Void> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.offhandClick(player, i, event);
                resultMap.put(power, result);
            }
            resultMap.put(power, result);
            if (result.isAbort()) break;
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.OFFHAND_CLICK, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void sneak(Player player, ItemStack i, PlayerToggleSneakEvent event) {
        if (!triggerPreCheck(player, i, this.getPowerSneak(), TriggerType.SNEAK)) return;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerSneak power : getPowerSneak()) {
            PowerResult<Void> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.sneak(player, i, event);
                resultMap.put(power, result);
            }
            resultMap.put(power, result);
            if (result.isAbort()) break;
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.SNEAK, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void sprint(Player player, ItemStack i, PlayerToggleSprintEvent event) {
        if (!triggerPreCheck(player, i, this.getPowerSprint(), TriggerType.SPRINT)) return;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerSprint power : getPowerSprint()) {
            PowerResult<Void> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.sprint(player, i, event);
                resultMap.put(power, result);
            }
            resultMap.put(power, result);
            if (result.isAbort()) break;
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.SPRINT, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void projectileHit(Player player, ItemStack i, Projectile arrow, ProjectileHitEvent event) {
        if (!triggerPreCheck(player, i, this.getPowerProjectileHit(), TriggerType.PROJECTILE_HIT)) return;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerProjectileHit power : getPowerProjectileHit()) {
            PowerResult<Void> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.projectileHit(player, i, arrow, event);
                resultMap.put(power, result);
            }
            resultMap.put(power, result);
            if (result.isAbort()) break;
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.PROJECTILE_HIT, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public double hit(Player player, ItemStack i, LivingEntity target, EntityDamageByEntityEvent event) {
        double ret = event.getDamage();
        if (!triggerPreCheck(player, i, this.getPowerHit(), TriggerType.HIT)) return ret;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerHit power : getPowerHit()) {
            PowerResult<Double> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.hit(player, i, target, event.getDamage(), event);
                resultMap.put(power, result);
            }
            if (result.isAbort()) break;
            if (!result.isOK()) continue;
            ret = Math.max(ret, result.getData());
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.HIT, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
        return ret;
    }

    public double takeHit(Player player, ItemStack i, EntityDamageEvent ev) {
        double ret = ev.getDamage();
        if (!triggerPreCheck(player, i, this.getPowerHitTaken(), TriggerType.HIT_TAKEN)) return ret;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerHitTaken power : getPowerHitTaken()) {
            PowerResult<Double> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.takeHit(player, i, ev.getDamage(), ev);
                resultMap.put(power, result);
            }
            if (result.isAbort()) break;
            if (!result.isOK()) continue;
            ret = Math.min(ret, result.getData());
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.HIT_TAKEN, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
        return ret;
    }

    public void hurt(Player player, ItemStack i, EntityDamageEvent ev) {
        if (!triggerPreCheck(player, i, this.getPowerHurt(), TriggerType.HURT)) return;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerHurt power : getPowerHurt()) {
            PowerResult<Void> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.hurt(player, i, ev);
                resultMap.put(power, result);
            }
            if (result.isAbort()) break;
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.HURT, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void swapToOffhand(Player player, ItemStack i, PlayerSwapHandItemsEvent ev) {
        if (!triggerPreCheck(player, i, this.getPowerSwapToOffhand(), TriggerType.SWAP_TO_OFFHAND)) return;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerSwapToOffhand power : getPowerSwapToOffhand()) {
            PowerResult<Boolean> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.swapToOffhand(player, i, ev);
                resultMap.put(power, result);
            }
            if (!result.getData()) {
                ev.setCancelled(true);
            }
            if (result.isAbort()) break;
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.SWAP_TO_OFFHAND, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void swapToMainhand(Player player, ItemStack i, PlayerSwapHandItemsEvent ev) {
        if (!triggerPreCheck(player, i, this.getPowerSwapToMainhand(), TriggerType.SWAP_TO_MAINHAND)) return;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerSwapToMainhand power : getPowerSwapToMainhand()) {
            PowerResult<Boolean> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.swapToMainhand(player, i, ev);
                resultMap.put(power, result);
            }
            resultMap.put(power, result);
            if (!result.getData()) {
                ev.setCancelled(true);
            }
            if (result.isAbort()) break;
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.SWAP_TO_MAINHAND, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void pickupOffhand(Player player, ItemStack i, InventoryClickEvent ev) {
        if (!triggerPreCheck(player, i, this.getPowerSwapToMainhand(), TriggerType.PICKUP_OFF_HAND)) return;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerSwapToMainhand power : getPowerSwapToMainhand()) {
            PowerResult<Boolean> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.pickupOffhand(player, i, ev);
                resultMap.put(power, result);
            }
            if (!result.getData()) {
                ev.setCancelled(true);
            }
            if (result.isAbort()) break;
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.PICKUP_OFF_HAND, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void placeOffhand(Player player, ItemStack i, InventoryClickEvent ev) {
        if (!triggerPreCheck(player, i, this.getPowerSwapToOffhand(), TriggerType.PLACE_OFF_HAND)) return;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerSwapToOffhand power : getPowerSwapToOffhand()) {
            PowerResult<Boolean> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.placeOffhand(player, i, ev);
                resultMap.put(power, result);
            }
            if (!result.getData()) {
                ev.setCancelled(true);
            }
            if (result.isAbort()) break;
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.PLACE_OFF_HAND, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);

        if (getDurability(i) <= 0) {
            i.setAmount(0);
            i.setType(Material.AIR);
        }
    }

    public void tick(Player player, ItemStack i) {
        if (!triggerPreCheck(player, i, this.getPowerTick(), TriggerType.TICK)) return;
        List<PowerCondition> conds = getPower(PowerCondition.class);
        Map<PowerCondition, PowerResult> staticCond = checkStaticCondition(player, i, conds);
        Map<Power, PowerResult> resultMap = new LinkedHashMap<>(staticCond);
        for (PowerTick power : getPowerTick()) {
            PowerResult<Void> result = checkConditions(player, i, power, conds, resultMap);
            if (result != null) {
                resultMap.put(power, result);
            } else {
                result = power.tick(player, i);
                resultMap.put(power, result);
            }
            if (result.isAbort()) break;
        }
        RPGItemsPowersPostFireEvent postFire = new RPGItemsPowersPostFireEvent(i, this, player, TriggerType.TICK, resultMap);
        Bukkit.getServer().getPluginManager().callEvent(postFire);
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
        if (customItemModel || hasPower(PowerUnbreakable.class)) {
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
        resetRecipe(true);
    }

    public ItemMeta getLocaleMeta() {
        return localeMeta.clone();
    }

    public void addExtra(RPGMetadata rpgMeta, List<String> lore) {
        if (maxDurability > 0) {
            if (!rpgMeta.containsKey(RPGMetadata.DURABILITY)) {
                rpgMeta.put(RPGMetadata.DURABILITY, defaultDurability);
            }
            int durability = ((Number) rpgMeta.get(RPGMetadata.DURABILITY)).intValue();

            if (!hasBar || forceBar || customItemModel) {
                StringBuilder out = new StringBuilder();
                char boxChar = '\u25A0';
                int boxCount = tooltipWidth / 6;
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
        }
    }

    public List<String> getTooltipLines() {
        ArrayList<String> output = new ArrayList<>();
        output.add(getMCEncodedUID() + quality.colour + ChatColor.BOLD + displayName);

        // add powerLores
        if (showPowerLore) {
            for (Power p : powers) {
                String txt = p.displayText();
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
        addExtra(rpgMeta, lore);
        meta.setLore(lore);
        rStack.setItemMeta(meta);
        rStack = refreshAttributeModifiers(this, rStack);
        return rStack;
    }

    public static ItemStack refreshAttributeModifiers(RPGItem item, ItemStack rStack) {
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
        if (defaultDurability == 0) {
            setDefaultDurability(newVal);
        }
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

    public <T extends Power> List<T> getPower(Class<T> power, boolean subclass) {
        return subclass ? powers.stream().filter(power::isInstance).map(power::cast).collect(Collectors.toList()) : getPower(power);
    }

    public void addPower(Power power) {
        addPower(power, true);
    }

    private void addPower(Power power, boolean update) {
        powers.add(power);
        if (update)
            rebuild();
    }

    public void removePower(Power power) {
        powers.remove(power);
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

    private List<PowerLeftClick> getPowerLeftClick() {
        return powers.stream().filter(p -> p.getTriggers().contains(TriggerType.LEFT_CLICK)).map(p -> (PowerLeftClick) p).collect(Collectors.toList());
    }

    private List<PowerRightClick> getPowerRightClick() {
        return powers.stream().filter(p -> p.getTriggers().contains(TriggerType.RIGHT_CLICK)).map(p -> (PowerRightClick) p).collect(Collectors.toList());
    }

    private List<PowerProjectileHit> getPowerProjectileHit() {
        return powers.stream().filter(p -> p.getTriggers().contains(TriggerType.PROJECTILE_HIT)).map(p -> (PowerProjectileHit) p).collect(Collectors.toList());
    }

    private List<PowerHit> getPowerHit() {
        return powers.stream().filter(p -> p.getTriggers().contains(TriggerType.HIT)).map(p -> (PowerHit) p).collect(Collectors.toList());
    }

    private List<PowerHitTaken> getPowerHitTaken() {
        return powers.stream().filter(p -> p.getTriggers().contains(TriggerType.HIT_TAKEN)).map(p -> (PowerHitTaken) p).collect(Collectors.toList());
    }

    private List<PowerHurt> getPowerHurt() {
        return powers.stream().filter(p -> p.getTriggers().contains(TriggerType.HURT)).map(p -> (PowerHurt) p).collect(Collectors.toList());
    }

    private List<PowerTick> getPowerTick() {
        return powers.stream().filter(p -> p.getTriggers().contains(TriggerType.TICK)).map(p -> (PowerTick) p).collect(Collectors.toList());
    }

    private List<PowerOffhandClick> getPowerOffhandClick() {
        return powers.stream().filter(p -> p.getTriggers().contains(TriggerType.OFFHAND_CLICK)).map(p -> (PowerOffhandClick) p).collect(Collectors.toList());
    }

    private List<PowerSneak> getPowerSneak() {
        return powers.stream().filter(p -> p.getTriggers().contains(TriggerType.SNEAK)).map(p -> (PowerSneak) p).collect(Collectors.toList());
    }

    private List<PowerSprint> getPowerSprint() {
        return powers.stream().filter(p -> p.getTriggers().contains(TriggerType.SPRINT)).map(p -> (PowerSprint) p).collect(Collectors.toList());
    }

    private List<PowerSwapToOffhand> getPowerSwapToOffhand() {
        return powers.stream().filter(p -> p.getTriggers().contains(TriggerType.SWAP_TO_OFFHAND)).map(p -> (PowerSwapToOffhand) p).collect(Collectors.toList());
    }

    private List<PowerSwapToMainhand> getPowerSwapToMainhand() {
        return powers.stream().filter(p -> p.getTriggers().contains(TriggerType.SWAP_TO_MAINHAND)).map(p -> (PowerSwapToMainhand) p).collect(Collectors.toList());
    }

    @LangKey(type = LangKeyType.SUFFIX)
    public enum DamageMode {
        FIXED,
        VANILLA,
        ADDITIONAL,
        MULTIPLY,
    }
}
