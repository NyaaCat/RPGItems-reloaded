package think.rpgitems.item;

import static think.rpgitems.utils.ItemTagUtils.*;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.utils.HexColorUtils;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import cat.nyaa.nyaacore.utils.ItemTagUtils;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Multimap;
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
import think.rpgitems.power.chain.PowerChain;
import think.rpgitems.power.cond.SlotCondition;
import think.rpgitems.power.marker.*;
import think.rpgitems.power.propertymodifier.Modifier;
import think.rpgitems.power.propertymodifier.RgiParameter;
import think.rpgitems.power.trigger.BaseTriggers;
import think.rpgitems.power.trigger.Trigger;

public class RPGItem {
  public static final NamespacedKey TAG_META = new NamespacedKey(RPGItems.plugin, "meta");
  public static final NamespacedKey TAG_ITEM_UID = new NamespacedKey(RPGItems.plugin, "item_uid");
  public static final NamespacedKey TAG_IS_MODEL = new NamespacedKey(RPGItems.plugin, "is_model");
  public static final NamespacedKey TAG_DURABILITY =
      new NamespacedKey(RPGItems.plugin, "durability");
  public static final NamespacedKey TAG_OWNER = new NamespacedKey(RPGItems.plugin, "owner");
  public static final NamespacedKey TAG_STACK_ID = new NamespacedKey(RPGItems.plugin, "stack_id");
  public static final NamespacedKey TAG_MODIFIER =
      new NamespacedKey(RPGItems.plugin, "property_modifier");
  public static final NamespacedKey TAG_VERSION = new NamespacedKey(RPGItems.plugin, "version");
  public static final String DAMAGE_TYPE = "RGI_DAMAGE_TYPE";
  public static final String NBT_UID = "rpgitem_uid";
  public static final String NBT_ITEM_UUID = "rpgitem_item_uuid";
  public static final String NBT_IS_MODEL = "rpgitem_is_model";

  private static final Cache<UUID, List<Modifier>> modifierCache =
      CacheBuilder.newBuilder().concurrencyLevel(1).expireAfterAccess(1, TimeUnit.MINUTES).build();

  static RPGItems plugin;

  // Powers
  private List<PowerChain> chains = new ArrayList<>();
  private List<Condition<?>> conditions = new ArrayList<>();
  private List<Marker> markers = new ArrayList<>();

  @SuppressWarnings("rawtypes")
  private Map<String, Trigger> triggers = new HashMap<>();

  private HashMap<PropertyHolder, NamespacedKey> keys = new HashMap<>();
  private File file;

  private int id;
  private int uid;
  private String name;
  int tooltipWidth = 150;

  RPGItemMeta rpgItemMeta = new RPGItemMeta();

  public RPGItem(String name, int uid, CommandSender author) {
    this.name = name;
    this.uid = uid;
    this.setAuthor(
        author instanceof Player
            ? ((Player) author).getUniqueId().toString()
            : plugin.cfg.defaultAuthor);
    setEnchantMode(plugin.cfg.defaultEnchantMode);
    setItem(Material.WOODEN_SWORD);
    setDisplayName(getItem().toString());
    getItemFlags().add(ItemFlag.HIDE_ATTRIBUTES);
    setMcVersion(RPGItems.getServerMCVersion());
    setPluginSerial(RPGItems.getSerial());
    setPluginVersion(RPGItems.getVersion());
    rebuild();
  }

  public RPGItem(ConfigurationSection s, File f)
      throws UnknownPowerException, UnknownExtensionException {
    setFile(f);
    name = s.getString("name");
    id = s.getInt("id");
    uid = s.getInt("uid");

    if (uid == 0) {
      uid = ItemManager.nextUid();
    }
    restore(s);
  }

  public RPGItem(ConfigurationSection s, String name, int uid)
      throws UnknownPowerException, UnknownExtensionException {
    if (uid >= 0) throw new IllegalArgumentException();
    this.name = name;
    this.uid = uid;
    restore(s);
  }

  public static void updateItemStack(ItemStack item) {
    Optional<RPGItem> rItem = ItemManager.toRPGItem(item);
    rItem.ifPresent(r -> r.updateItem(item, false));
  }

  private void restore(ConfigurationSection s)
      throws UnknownPowerException, UnknownExtensionException {

    // Powers
    ConfigurationSection powerList = s.getConfigurationSection("powers");
    if (powerList != null) {
      for (String sectionKey : powerList.getKeys(false)) {
        ConfigurationSection section = powerList.getConfigurationSection(sectionKey);
        String powerName = Objects.requireNonNull(section).getString("powerName");
        // 3.7 -> 3.8 Migration
        if (Objects.requireNonNull(powerName).endsWith("condition")) {
          loadCondition(section, powerName);
        } else if (Stream.of(
                "attributemodifier",
                "lorefilter",
                "ranged",
                "rangedonly",
                "selector",
                "unbreakable")
            .anyMatch(powerName::endsWith)) {
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
        ConfigurationSection section =
            Objects.requireNonNull(conditionList).getConfigurationSection(sectionKey);
        String conditionName =
            Objects.requireNonNull(Objects.requireNonNull(section).getString("conditionName"));
        loadCondition(section, conditionName);
      }
    }
    // Markers
    ConfigurationSection markerList = s.getConfigurationSection("markers");
    if (markerList != null) {
      for (String sectionKey : markerList.getKeys(false)) {
        ConfigurationSection section =
            Objects.requireNonNull(markerList).getConfigurationSection(sectionKey);
        String markerName =
            Objects.requireNonNull(Objects.requireNonNull(section).getString("markerName"));
        loadMarker(section, markerName);
      }
    }
    // Triggers
    ConfigurationSection triggerList = s.getConfigurationSection("triggers");
    if (triggerList != null) {
      for (String sectionKey : triggerList.getKeys(false)) {
        ConfigurationSection section =
            Objects.requireNonNull(triggerList).getConfigurationSection(sectionKey);
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
      setHasDurabilityBar(
          getItem().getMaxDurability() == 0 || s.getBoolean("forceBar") || isCustomItemModel());
    }
    setHasDurabilityBar(s.getBoolean("hasDurabilityBar", isHasDurabilityBar()));

    setShowPowerText(s.getBoolean("showPowerText", true));
    setShowArmourLore(s.getBoolean("showArmourLore", true));
    setCustomModelData(s.getInt("customModelData", -1));
    setQuality(s.getString("quality", null));
    setType(s.getString("item", "item"));

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

  public int getCustomModelData() {
    return rpgItemMeta.customModelData;
  }

  public void setCustomModelData(int customModelData) {
    this.rpgItemMeta.customModelData = customModelData;
  }

  public void setArmourExpression(String armour) {
    this.rpgItemMeta.armourExpression = armour;
  }

  private void loadPower(ConfigurationSection section, String powerName)
      throws UnknownPowerException {
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

  private void loadCondition(ConfigurationSection section, String powerName)
      throws UnknownPowerException {
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

  private void loadMarker(ConfigurationSection section, String powerName)
      throws UnknownPowerException {
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
  private void loadTrigger(ConfigurationSection section, String triggerName)
      throws UnknownPowerException {
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

    savePowers(s);
    saveConditions(s);
    saveMarkers(s);
    saveTriggers(s);
    rpgItemMeta.save(s);
  }

  private void saveTriggers(ConfigurationSection s) {
    int i = 0;
    ConfigurationSection triggerConfigs = s.createSection("triggers");
    for (Entry<String, Trigger> p : triggers.entrySet()) {
      MemoryConfiguration pConfig = new MemoryConfiguration();
      p.getValue().save(pConfig);
      pConfig.set("base", p.getValue().getBase());
      triggerConfigs.set(p.getKey(), pConfig);
      i++;
    }
  }

  private void saveMarkers(ConfigurationSection s) {
    ConfigurationSection markerConfigs = s.createSection("markers");
    int i = 0;
    for (Marker p : markers) {
      MemoryConfiguration pConfig = new MemoryConfiguration();
      pConfig.set("markerName", p.getNamespacedKey().toString());
      p.save(pConfig);
      markerConfigs.set(Integer.toString(i), pConfig);
      i++;
    }
  }

  private void saveConditions(ConfigurationSection s) {
    ConfigurationSection conditionConfigs = s.createSection("conditions");
    int i = 0;
    for (Condition<?> p : conditions) {
      MemoryConfiguration pConfig = new MemoryConfiguration();
      pConfig.set("conditionName", p.getNamespacedKey().toString());
      p.save(pConfig);
      conditionConfigs.set(Integer.toString(i), pConfig);
      i++;
    }
  }

  private void savePowers(ConfigurationSection s) {
    ConfigurationSection powerConfigs = s.createSection("powers");
    int i = 0;
    for (Power p : getPowers()) {
      MemoryConfiguration pConfig = new MemoryConfiguration();
      pConfig.set("powerName", getPropertyHolderKey(p).toString());
      p.save(pConfig);
      powerConfigs.set(Integer.toString(i), pConfig);
      i++;
    }
  }

  public String getDamageType() {
    return this.rpgItemMeta.damageType;
  }

  public void setDamageType(String damageType) {
    this.rpgItemMeta.damageType = damageType;
  }

  public String getArmourExpression() {
    return rpgItemMeta.armourExpression;
  }

  public void updateItem(ItemStack item) {
    updateItem(item, false);
  }

  public void updateItem(ItemStack item, boolean loreOnly) {
    List<String> reservedLores = this.filterLores(item);
    item.setType(getItem());
    ItemMeta meta = item.getItemMeta();
    List<String> lore = new ArrayList<>(getLore());
    PersistentDataContainer itemTagContainer =
        Objects.requireNonNull(meta).getPersistentDataContainer();
    SubItemTagContainer rpgitemsTagContainer = makeTag(itemTagContainer, TAG_META);
    set(rpgitemsTagContainer, TAG_ITEM_UID, getUid());
    addDurabilityBar(rpgitemsTagContainer, lore);
    if (meta instanceof LeatherArmorMeta) {
      ((LeatherArmorMeta) meta).setColor(Color.fromRGB(getDataValue()));
    }
    Damageable damageable = (Damageable) meta;
    if (getMaxDurability() > 0) {
      int durability =
          computeIfAbsent(
              rpgitemsTagContainer,
              TAG_DURABILITY,
              PersistentDataType.INTEGER,
              this::getDefaultDurability);
      if (isCustomItemModel()) {
        damageable.setDamage(getDataValue());
      } else {
        damageable.setDamage(
            (getItem().getMaxDurability()
                - ((short)
                    ((double) getItem().getMaxDurability()
                        * ((double) durability / (double) getMaxDurability())))));
      }
    } else {
      if (isCustomItemModel()) {
        damageable.setDamage(getDataValue());
      } else {
        damageable.setDamage(getItem().getMaxDurability() != 0 ? 0 : getDataValue());
      }
    }
    // Patch for mcMMO buff. See SkillUtils.java#removeAbilityBuff in mcMMO
    if (item.hasItemMeta()
        && Objects.requireNonNull(item.getItemMeta()).hasLore()
        && Objects.requireNonNull(item.getItemMeta().getLore()).contains("mcMMO Ability Tool"))
      lore.add("mcMMO Ability Tool");
    lore.addAll(reservedLores);
    meta.setLore(lore);

    // quality prefix
    String qualityPrefix = plugin.cfg.qualityPrefixes.get(getQuality());
    if (qualityPrefix != null) {
      if (meta.hasDisplayName() && !meta.getDisplayName().startsWith(qualityPrefix)) {
        String displayName = meta.getDisplayName();
        meta.setDisplayName(HexColorUtils.hexColored(qualityPrefix) + displayName);
      }
    }

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

    if (getCustomModelData() != -1) {
      meta.setCustomModelData(getCustomModelData());
    } else {
      meta.setCustomModelData(null);
    }
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
    try {
      ItemTagUtils.setInt(item, NBT_UID, uid);
      if (RPGItems.plugin.cfg.itemStackUuid) {
        if (!ItemTagUtils.getString(item, NBT_ITEM_UUID).isPresent()) {
          UUID uuid = UUID.randomUUID();
          ItemTagUtils.setString(item, NBT_ITEM_UUID, uuid.toString());
        }
      }
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  private static final NamespacedKey RGI_UNIQUE_MARK =
      new NamespacedKey(RPGItems.plugin, "RGI_UNIQUE_MARK");

  private static final NamespacedKey RGI_UNIQUE_ID =
      new NamespacedKey(RPGItems.plugin, "RGI_UNIQUE_ID");

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
      int durability =
          computeIfAbsent(
              meta, TAG_DURABILITY, PersistentDataType.INTEGER, this::getDefaultDurability);
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
          case NUMERIC_MINUS_ONE:
            {
              out.append(ChatColor.GREEN.toString()).append(boxChar).append(" ");
              out.append(
                  ratio < 0.1 ? ChatColor.RED : ratio < 0.3 ? ChatColor.YELLOW : ChatColor.GREEN);
              out.append(formatBar(durability, maxDurability, barFormat));
              out.append(ChatColor.RESET).append(" / ").append(ChatColor.AQUA);
              out.append(formatBar(maxDurability, maxDurability, barFormat));
              out.append(ChatColor.GREEN).append(boxChar);
              break;
            }
          case DEFAULT:
            {
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
        else lore.set(lore.size() - 1, out.toString());
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
        return String.format(String.format("0b%%%ds", binLen), Integer.toBinaryString(durability))
            .replace(' ', '0');
      case NUMERIC_BIN_MINUS_ONE:
        int binLenM1 = Integer.toBinaryString(maxDurability - 1).length();
        return String.format(
                String.format("0b%%%ds", binLenM1), Integer.toBinaryString(durability - 1))
            .replace(' ', '0');
    }
    throw new UnsupportedOperationException();
  }

  private List<String> filterLores(ItemStack i) {
    List<String> ret = new ArrayList<>();
    List<LoreFilter> patterns =
        getMarker(LoreFilter.class).stream()
            .filter(p -> !Strings.isNullOrEmpty(p.regex))
            .map(LoreFilter::compile)
            .collect(Collectors.toList());
    if (patterns.isEmpty()) return Collections.emptyList();
    if (!i.hasItemMeta() || !Objects.requireNonNull(i.getItemMeta()).hasLore())
      return Collections.emptyList();
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
    Multimap<Attribute, org.bukkit.attribute.AttributeModifier> old =
        itemMeta.getAttributeModifiers();
    if (getAttributeMode().equals(AttributeMode.FULL_UPDATE)) {
      if (old != null && !old.isEmpty()) {
        old.forEach(itemMeta::removeAttributeModifier);
      }
    }
    if (!attributeModifiers.isEmpty()) {
      for (AttributeModifier attributeModifier : attributeModifiers) {
        Attribute attribute = attributeModifier.attribute;
        UUID uuid = new UUID(attributeModifier.uuidMost, attributeModifier.uuidLeast);
        org.bukkit.attribute.AttributeModifier modifier =
            new org.bukkit.attribute.AttributeModifier(
                uuid,
                attributeModifier.name,
                attributeModifier.amount,
                attributeModifier.operation,
                attributeModifier.slot);
        if (old != null) {
          old.entries().stream()
              .filter(m -> m.getValue().getUniqueId().equals(uuid))
              .findAny()
              .ifPresent(e -> itemMeta.removeAttributeModifier(e.getKey(), e.getValue()));
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
    if (item.getType() == Material.BOW
        || item.getType() == Material.SNOWBALL
        || item.getType() == Material.EGG
        || item.getType() == Material.POTION) {
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
   * @param p Player who launched the damager
   * @param originDamage Origin damage value
   * @param stack ItemStack of this item
   * @param entity Victim of this damage event
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
        damage =
            getDamageMin() != getDamageMax()
                ? (getDamageMin()
                    + ThreadLocalRandom.current().nextInt(getDamageMax() - getDamageMin() + 1))
                : getDamageMin();

        if (getDamageMode() == DamageMode.MULTIPLY) {
          damage *= originDamage;
          break;
        }

        Collection<PotionEffect> potionEffects = p.getActivePotionEffects();
        double strength = 0, weak = 0;
        for (PotionEffect pe : potionEffects) {
          if (pe.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
            strength = 3 * (pe.getAmplifier() + 1); // MC 1.9+
          }
          if (pe.getType().equals(PotionEffectType.WEAKNESS)) {
            weak = 4 * (pe.getAmplifier() + 1); // MC 1.9+
          }
        }
        damage = damage + strength - weak;

        if (getDamageMode() == DamageMode.ADDITIONAL) {
          damage += originDamage;
        }
        if (damage < 0) damage = 0;
        break;
      case VANILLA:
        // no-op
        break;
    }
    return damage;
  }

  /**
   * Event-type independent projectile damage event
   *
   * @param p Player who launched the damager
   * @param originDamage Origin damage value
   * @param stack ItemStack of this item
   * @param damager Projectile of this damage event
   * @param entity Victim of this damage event
   * @return Final damage or -1 if should cancel this event
   */
  public double projectileDamage(
      Player p, double originDamage, ItemStack stack, Entity damager, Entity entity) {
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
        damage =
            getDamageMin() != getDamageMax()
                ? (getDamageMin()
                    + ThreadLocalRandom.current().nextInt(getDamageMax() - getDamageMin() + 1))
                : getDamageMin();

        if (getDamageMode() == DamageMode.MULTIPLY) {
          damage *= originDamage;
          break;
        }

        // Apply force adjustments
        if (damager.hasMetadata("RPGItems.Force")) {
          damage *= damager.getMetadata("RPGItems.Force").get(0).asFloat();
        }
        if (getDamageMode() == DamageMode.ADDITIONAL) {
          damage += originDamage;
        }
        break;
      case VANILLA:
        // no-op
        break;
    }
    return damage;
  }

  /**
   * Event-type independent take damage event
   *
   * @param p Player taking damage
   * @param originDamage Origin damage value
   * @param stack ItemStack of this item
   * @param damager Cause of this damage. May be null
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
   * @param p Player taking damage
   * @param stack ItemStack of this item
   * @param block Block
   * @return If should process this event
   */
  public boolean breakBlock(Player p, ItemStack stack, Block block) {
    return consumeDurability(stack, getBlockBreakingCost());
  }

  public static List<Modifier> getModifiers(ItemStack stack) {
    Optional<String> opt = ItemTagUtils.getString(stack, NBT_ITEM_UUID);
    if (!opt.isPresent()) {
      Optional<RPGItem> rpgItemOpt = ItemManager.toRPGItemByMeta(stack);
      if (!rpgItemOpt.isPresent()) {
        return Collections.emptyList();
      }
      RPGItem rpgItem = rpgItemOpt.get();
      rpgItem.updateItem(stack);
      Optional<String> opt1 = ItemTagUtils.getString(stack, NBT_ITEM_UUID);
      if (!opt1.isPresent()) {
        return Collections.emptyList();
      }
      opt = opt1;
    }

    UUID key = UUID.fromString(opt.get());
    List<Modifier> modifiers = modifierCache.getIfPresent(key);
    if (modifiers == null) {
      ItemMeta itemMeta = stack.getItemMeta();
      if (itemMeta == null) return new ArrayList<>();
      SubItemTagContainer tag =
          makeTag(Objects.requireNonNull(itemMeta).getPersistentDataContainer(), TAG_MODIFIER);
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
      for (NamespacedKey key = PowerManager.parseKey(String.valueOf(i));
          tag.has(key, PersistentDataType.TAG_CONTAINER);
          key = PowerManager.parseKey(String.valueOf(++i))) {
        PersistentDataContainer container = getTag(tag, key);
        String modifierName = getString(container, "modifier_name");
        Class<? extends Modifier> modifierClass =
            PowerManager.getModifier(PowerManager.parseKey(modifierName));
        Modifier modifier = PowerManager.instantiate(modifierClass);
        modifier.init(container);
        ret.add(modifier);
      }
      return ret;
    } finally {
      tag.commit();
    }
  }

  private <
          TEvent extends Event,
          TPower extends Power,
          TPimpl extends Pimpl<TPower>,
          TResult,
          TReturn>
      boolean triggerPreCheck(
          Player player,
          ItemStack i,
          TEvent event,
          Trigger<TEvent, TPower, TPimpl, TResult, TReturn> trigger,
          List<TPower> powers) {
    if (i.getType().equals(Material.AIR)) return false;
    if (powers.isEmpty()) return false;
    if (checkPermission(player, true) == Event.Result.DENY) return false;

    RPGItemsPowersPreFireEvent<TEvent, TPower, TPimpl, TResult, TReturn> preFire =
        new RPGItemsPowersPreFireEvent<>(player, i, event, this, trigger, powers);
    Bukkit.getServer().getPluginManager().callEvent(preFire);
    return !preFire.isCancelled();
  }

  private <T> PowerResult<T> checkConditions(
      Player player,
      ItemStack i,
      Power power,
      List<Condition<?>> conds,
      Map<PropertyHolder, PowerResult<?>> context) {
    Set<String> ids = power.getConditions();
    List<Condition<?>> conditions =
        conds.stream().filter(p -> ids.contains(p.id())).collect(Collectors.toList());
    List<Condition<?>> failed =
        conditions.stream()
            .filter(
                p -> p.isStatic() ? !context.get(p).isOK() : !p.check(player, i, context).isOK())
            .collect(Collectors.toList());
    if (failed.isEmpty()) return null;
    return failed.stream().anyMatch(Condition::isCritical)
        ? PowerResult.abort()
        : PowerResult.condition();
  }

  private Map<Condition<?>, PowerResult<?>> checkStaticCondition(
      Player player, ItemStack i, List<Condition<?>> conds) {
    // todo adapt chain framework
    Set<String> ids =
        getPowers().stream().flatMap(p -> p.getConditions().stream()).collect(Collectors.toSet());
    List<Condition<?>> statics =
        conds.stream()
            .filter(Condition::isStatic)
            .filter(p -> ids.contains(p.id()))
            .collect(Collectors.toList());
    Map<Condition<?>, PowerResult<?>> result = new LinkedHashMap<>();
    for (Condition<?> c : statics) {
      result.put(c, c.check(player, i, Collections.unmodifiableMap(result)));
    }
    return result;
  }

  public <
          TEvent extends Event,
          TPower extends Power,
          TPimpl extends Pimpl<TPower>,
          TResult,
          TReturn>
      TReturn power(
          Player player,
          ItemStack i,
          TEvent event,
          Trigger<TEvent, TPower, TPimpl, TResult, TReturn> trigger,
          Object context) {
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
          resultMap.put(power, result);
        } else {
          if (power.requiredContext() != null) {
            result = handleContext(player, i, event, trigger, power);
          } else {
            result = trigger.run(power, null, player, i, event, context);
          }
          resultMap.put(power, result);
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
  public <
          TEvent extends Event,
          TPower extends Power,
          TPimpl extends Pimpl<TPower>,
          TResult,
          TReturn>
      void powerCustomTrigger(
          Player player,
          ItemStack i,
          TEvent event,
          Trigger<TEvent, TPower, TPimpl, TResult, TReturn> trigger,
          Object context) {
    this.triggers.entrySet().parallelStream()
        .filter(e -> trigger.getClass().isInstance(e.getValue()))
        .sorted(Comparator.comparing(en -> en.getValue().getPriority()))
        .filter(e -> e.getValue().check(player, i, event))
        .forEach(e -> this.power(player, i, event, e.getValue(), context));
  }

  public <
          TEvent extends Event,
          TPower extends Power,
          TPimpl extends Pimpl<TPower>,
          TResult,
          TReturn>
      TReturn power(
          Player player,
          ItemStack i,
          TEvent event,
          Trigger<TEvent, TPower, TPimpl, TResult, TReturn> trigger) {
    return power(player, i, event, trigger, null);
  }

  public <
          TEvent extends Event,
          TPower extends Power,
          TPimpl extends Pimpl<TPower>,
          TResult,
          TReturn>
      PowerResult<TResult> handleContext(
          Player player,
          ItemStack i,
          TEvent event,
          Trigger<TEvent, TPower, TPimpl, TResult, TReturn> trigger,
          TPower power) {
    PowerResult<TResult> result;
    String contextKey = power.requiredContext();
    Object context = Context.instance().get(player.getUniqueId(), contextKey);
    if (context == null) {
      return PowerResult.context();
    }
    if (context instanceof Location) {
      if (power instanceof PowerLocation) {
        PowerResult<Void> overrideResult =
            BaseTriggers.LOCATION.run(power, null, player, i, event, context);
        result = trigger.warpResult(overrideResult, power, null, player, i, event);
      } else {
        throw new IllegalStateException();
      }
    } else if (context instanceof Pair) {
      Object key = ((Pair) context).getKey();
      if (key instanceof LivingEntity) {
        PowerResult<Void> overrideResult =
            BaseTriggers.LIVINGENTITY.run(power, null, player, i, event, context);
        result = trigger.warpResult(overrideResult, power, null, player, i, event);
      } else {
        throw new IllegalStateException();
      }
    } else {
      throw new IllegalStateException();
    }
    return result;
  }

  private <
          TEvent extends Event,
          TPower extends Power,
          TPimpl extends Pimpl<TPower>,
          TResult,
          TReturn>
      void triggerPostFire(
          Player player,
          ItemStack itemStack,
          TEvent event,
          Trigger<TEvent, TPower, TPimpl, TResult, TReturn> trigger,
          Map<PropertyHolder, PowerResult<?>> resultMap,
          TReturn ret) {
    RPGItemsPowersPostFireEvent<TEvent, TPower, TPimpl, TResult, TReturn> postFire =
        new RPGItemsPowersPostFireEvent<>(player, itemStack, event, this, trigger, resultMap, ret);
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
          damageStr +=
              I18n.formatDefault(
                  "item.additionaldamage",
                  getDamageMin() == getDamageMax()
                      ? String.valueOf(getDamageMin())
                      : getDamageMin() + "-" + getDamageMax());
        } else if (getDamageMode() == DamageMode.MULTIPLY) {
          damageStr +=
              I18n.formatDefault(
                  "item.multiplydamage",
                  getDamageMin() == getDamageMax()
                      ? String.valueOf(getDamageMin())
                      : getDamageMin() + "-" + getDamageMax());
        } else {
          damageStr +=
              I18n.formatDefault(
                  "item.damage",
                  getDamageMin() == getDamageMax()
                      ? String.valueOf(getDamageMin())
                      : getDamageMin() + "-" + getDamageMax());
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
    PersistentDataContainer itemTagContainer =
        Objects.requireNonNull(meta).getPersistentDataContainer();
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
    SubItemTagContainer meta =
        makeTag(Objects.requireNonNull(itemMeta).getPersistentDataContainer(), TAG_META);
    meta.remove(TAG_OWNER);
    meta.remove(TAG_STACK_ID);
    set(meta, TAG_IS_MODEL, true);
    try {
      ItemTagUtils.setBoolean(itemStack, NBT_IS_MODEL, true);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    meta.commit();
    itemMeta.setDisplayName(getDisplayName());
    itemStack.setItemMeta(itemMeta);
  }

  public void unModel(ItemStack itemStack, Player owner) {
    updateItem(itemStack);
    ItemMeta itemMeta = itemStack.getItemMeta();
    SubItemTagContainer meta =
        makeTag(Objects.requireNonNull(itemMeta).getPersistentDataContainer(), TAG_META);
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
        p.sendMessage(
            I18n.getInstance(p.getLocale()).format("message.error.permission", getDisplayName()));
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
          .append(
              I18n.getInstance(((Player) sender).getLocale()).format("message.item.print"),
              toItemStack())
          .send(sender);
    } else {
      List<String> lines = getTooltipLines();
      for (String line : lines) {
        sender.sendMessage(line);
      }
    }
    I18n i18n = I18n.getInstance(locale);

    new Message("")
        .append(
            I18n.formatDefault("message.print.author"),
            Collections.singletonMap("{author}", authorComponent))
        .send(sender);
    if (!advance) {
      return;
    }

    new Message(I18n.formatDefault("message.print.license", getLicense())).send(sender);
    new Message(I18n.formatDefault("message.print.note", getNote())).send(sender);

    sender.sendMessage(
        I18n.formatDefault(
            "message.durability.info",
            getMaxDurability(),
            getDefaultDurability(),
            getDurabilityLowerBound(),
            getDurabilityUpperBound()));
    if (isCustomItemModel()) {
      sender.sendMessage(
          I18n.formatDefault(
              "message.print.customitemmodel", getItem().name() + ":" + getDataValue()));
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
    // Power Consume will make this null in triggerPostFire().
    if (itemMeta == null) {
      return Optional.empty();
    }
    SubItemTagContainer tagContainer = makeTag(itemMeta, TAG_META);
    int durability =
        computeIfAbsent(
            tagContainer, TAG_DURABILITY, PersistentDataType.INTEGER, this::getDefaultDurability);
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
      durability =
          computeIfAbsent(
              tagContainer, TAG_DURABILITY, PersistentDataType.INTEGER, this::getDefaultDurability);
      if (checkbound
          && ((val > 0 && durability < getDurabilityLowerBound())
              || (val < 0 && durability > getDurabilityUpperBound()))) {
        tagContainer.commit();
        item.setItemMeta(itemMeta);
        return false;
      }
      if (durability <= val && hasMarker(Unbreakable.class) && !isCustomItemModel()) {
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
    Material item = getItem();
    if (wear) {
      if (item.equals(Material.CHAINMAIL_HELMET)
          || item.equals(Material.DIAMOND_HELMET)
          || item.equals(Material.GOLDEN_HELMET)
          || item.equals(Material.IRON_HELMET)
          || item.equals(Material.LEATHER_HELMET)
          || item.equals(Material.TURTLE_HELMET)) {
        if (player.getInventory().getHelmet() == null) {
          player.getInventory().setHelmet(itemStack);
          return;
        }
      } else if (item.equals(Material.CHAINMAIL_CHESTPLATE)
          || item.equals(Material.DIAMOND_CHESTPLATE)
          || item.equals(Material.GOLDEN_CHESTPLATE)
          || item.equals(Material.IRON_CHESTPLATE)
          || item.equals(Material.LEATHER_CHESTPLATE)) {
        if (player.getInventory().getChestplate() == null) {
          player.getInventory().setChestplate(itemStack);
          return;
        }
      } else if (item.equals(Material.CHAINMAIL_LEGGINGS)
          || item.equals(Material.DIAMOND_LEGGINGS)
          || item.equals(Material.GOLDEN_LEGGINGS)
          || item.equals(Material.IRON_LEGGINGS)
          || item.equals(Material.LEATHER_LEGGINGS)) {
        if (player.getInventory().getLeggings() == null) {
          player.getInventory().setLeggings(itemStack);
          return;
        }
      } else if (item.equals(Material.CHAINMAIL_BOOTS)
          || item.equals(Material.DIAMOND_BOOTS)
          || item.equals(Material.GOLDEN_BOOTS)
          || item.equals(Material.IRON_BOOTS)
          || item.equals(Material.LEATHER_BOOTS)) {
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
    return markers.stream()
        .filter(p -> p.getClass().equals(marker))
        .map(marker::cast)
        .collect(Collectors.toList());
  }

  public <T extends Condition<?>> List<T> getConditions(Class<T> condition) {
    return conditions.stream()
        .filter(p -> p.getClass().equals(condition))
        .map(condition::cast)
        .collect(Collectors.toList());
  }

  public <T extends Marker> List<T> getMarker(Class<T> marker, boolean subclass) {
    return subclass
        ? markers.stream().filter(marker::isInstance).map(marker::cast).collect(Collectors.toList())
        : getMarker(marker);
  }

  public <T extends Marker> List<T> getMarker(NamespacedKey key, Class<T> marker) {
    return markers.stream()
        .filter(p -> p.getClass().equals(marker) && getPropertyHolderKey(p).equals(key))
        .map(marker::cast)
        .collect(Collectors.toList());
  }

  public <T extends Power> List<T> getPower(NamespacedKey key, Class<T> power) {
    return getPowers().stream()
        .filter(p -> p.getClass().equals(power) && getPropertyHolderKey(p).equals(key))
        .map(power::cast)
        .collect(Collectors.toList());
  }

  public <T extends Condition<?>> List<T> getCondition(NamespacedKey key, Class<T> condition) {
    // todo adapt chain framework
    return getPowers().stream()
        .filter(p -> p.getClass().equals(condition) && getPropertyHolderKey(p).equals(key))
        .map(condition::cast)
        .collect(Collectors.toList());
  }

  public Condition<?> getCondition(String id) {
    return conditions.stream().filter(c -> c.id().equals(id)).findAny().orElse(null);
  }

  public void addPower(NamespacedKey key, Power power) {
    addPower(key, power, true);
  }

  private void addPower(NamespacedKey key, Power power, boolean update) {
    // todo adapt chain framework
    //        powers.add(power);
    keys.put(power, key);
    if (update) {
      rebuild();
    }
  }

  public void removePower(Power power) {
    // todo adapt chain framework
    //        powers.remove(power);
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
    getDescription().add(HexColorUtils.hexColored(str));
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
    HoverEvent hover =
        new HoverEvent(
            HoverEvent.Action.SHOW_ITEM,
            new BaseComponent[] {new TextComponent(ItemStackUtils.itemToJson(toItemStack()))});
    msg.setHoverEvent(hover);
    return msg;
  }

  @SuppressWarnings("unchecked")
  private <
          TEvent extends Event,
          TPower extends Power,
          TPimpl extends Pimpl<TPower>,
          TResult,
          TReturn>
      List<TPower> getPower(
          Trigger<TEvent, TPower, TPimpl, TResult, TReturn> trigger,
          Player player,
          ItemStack stack) {
    RPGItems.logger.info(player.toString());
    RPGItems.logger.info(stack.toString());
    return getPowers().stream()
        .filter(p -> p.getTriggers().contains(trigger))
        .map(p -> ((TPower) p)) // TODO
        .collect(Collectors.toList());
  }

  @SuppressWarnings("rawtypes")
  public static class DynamicMethodInterceptor implements MethodInterceptor {
    private static WeakHashMap<Player, WeakHashMap<ItemStackWrapper, WeakHashMap<Power, Power>>>
        cache = new WeakHashMap<>();

    private static Power makeProxy(
        Power orig, Player player, Class<? extends Power> cls, ItemStack stack, Trigger trigger) {
      Enhancer enhancer = new Enhancer();
      enhancer.setSuperclass(cls);
      enhancer.setInterfaces(new Class[] {trigger.getPimplClass()});
      enhancer.setCallback(new DynamicMethodInterceptor(orig, player, stack));
      return (Power) enhancer.create();
    }

    protected static Power create(
        Power orig, Player player, Class<? extends Power> cls, ItemStack stack, Trigger trigger) {
      return cache
          .computeIfAbsent(player, (k) -> new WeakHashMap<>())
          .computeIfAbsent(ItemStackWrapper.of(stack), (p) -> new WeakHashMap<>())
          .computeIfAbsent(orig, (s) -> makeProxy(orig, player, cls, stack, trigger));
    }

    private final Power orig;
    private final Player player;
    private final Map<Method, PropertyInstance> getters;
    private ItemStack stack;

    protected DynamicMethodInterceptor(Power orig, Player player, ItemStack stack) {
      this.orig = orig;
      this.player = player;
      this.getters =
          PowerManager.getProperties(orig.getClass()).entrySet().stream()
              .collect(Collectors.toMap(e -> e.getValue().getKey(), e -> e.getValue().getValue()));
      this.stack = stack;
    }

    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy)
        throws Throwable {
      if (getters.containsKey(method)) {
        PropertyInstance propertyInstance = getters.get(method);
        Class<?> type = propertyInstance.field().getType();
        List<Modifier> playerModifiers = getModifiers(player);
        List<Modifier> stackModifiers = getModifiers(stack);
        List<Modifier> modifiers =
            Stream.concat(playerModifiers.stream(), stackModifiers.stream())
                .sorted(Comparator.comparing(Modifier::priority))
                .collect(Collectors.toList());
        // Numeric modifiers
        if (type == int.class
            || type == Integer.class
            || type == float.class
            || type == Float.class
            || type == double.class
            || type == Double.class) {

          @SuppressWarnings("unchecked")
          List<Modifier<Double>> numberModifiers =
              modifiers.stream()
                  .filter(
                      m ->
                          (m.getModifierTargetType() == Double.class)
                              && m.match(orig, propertyInstance))
                  .map(m -> (Modifier<Double>) m)
                  .collect(Collectors.toList());
          Number value = (Number) methodProxy.invoke(orig, args);
          double origValue = value.doubleValue();
          for (Modifier<Double> numberModifier : numberModifiers) {
            RgiParameter param = new RgiParameter<>(orig.getItem(), orig, stack, origValue);
            origValue = numberModifier.apply(param);
          }
          if (int.class.equals(type) || Integer.class.equals(type)) {
            return (int) Math.round(origValue);
          } else if (float.class.equals(type) || Float.class.equals(type)) {
            return (float) (origValue);
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
    getPowers().forEach(Power::deinit);
  }

  public int getArmour() {
    return rpgItemMeta.armour;
  }

  public boolean isAlwaysAllowMelee() {
    return rpgItemMeta.alwaysAllowMelee;
  }

  public boolean isCanBeOwned() {
    return rpgItemMeta.canBeOwned;
  }

  public boolean isHasStackId() {
    return rpgItemMeta.hasStackId;
  }

  public void setAlwaysAllowMelee(boolean alwaysAllowMelee) {
    this.rpgItemMeta.alwaysAllowMelee = alwaysAllowMelee;
  }

  public void setArmour(int a) {
    setArmour(a, true);
  }

  public void setArmour(int a, boolean update) {
    this.rpgItemMeta.armour = a;
    if (update) {
      rebuild();
    }
  }

  public String getAuthor() {
    return rpgItemMeta.author;
  }

  public void setAuthor(String author) {
    this.rpgItemMeta.author = author;
  }

  public int getBlockBreakingCost() {
    return rpgItemMeta.blockBreakingCost;
  }

  public void setBlockBreakingCost(int blockBreakingCost) {
    this.rpgItemMeta.blockBreakingCost = blockBreakingCost;
  }

  public int getDamageMax() {
    return rpgItemMeta.damageMax;
  }

  public void setCanBeOwned(boolean canBeOwned) {
    this.rpgItemMeta.canBeOwned = canBeOwned;
  }

  private void setDamageMax(int damageMax) {
    this.rpgItemMeta.damageMax = damageMax;
  }

  public int getDamageMin() {
    return rpgItemMeta.damageMin;
  }

  private void setDamageMin(int damageMin) {
    this.rpgItemMeta.damageMin = damageMin;
  }

  public void setDamage(int min, int max) {
    setDamageMin(min);
    setDamageMax(max);
    rebuild();
  }

  public DamageMode getDamageMode() {
    return rpgItemMeta.damageMode;
  }

  public void setDamageMode(DamageMode damageMode) {
    this.rpgItemMeta.damageMode = damageMode;
  }

  public int getDataValue() {
    return rpgItemMeta.dataValue;
  }

  public void setDataValue(int dataValue) {
    this.rpgItemMeta.dataValue = dataValue;
  }

  public int getDefaultDurability() {
    return rpgItemMeta.defaultDurability;
  }

  public void setDefaultDurability(int newVal) {
    this.rpgItemMeta.defaultDurability = newVal;
  }

  public List<String> getDescription() {
    return this.rpgItemMeta.description;
  }

  public void setDescription(List<String> description) {
    this.rpgItemMeta.description = description;
  }

  public String getDisplayName() {
    return rpgItemMeta.displayName;
  }

  public void setDisplayName(String displayName) {
    this.rpgItemMeta.displayName = HexColorUtils.hexColored(displayName);
  }

  public int getDurabilityLowerBound() {
    return rpgItemMeta.durabilityLowerBound;
  }

  public void setDurabilityLowerBound(int durabilityLowerBound) {
    this.rpgItemMeta.durabilityLowerBound = durabilityLowerBound;
  }

  public int getDurabilityUpperBound() {
    return rpgItemMeta.durabilityUpperBound;
  }

  public void setDurabilityUpperBound(int durabilityUpperBound) {
    this.rpgItemMeta.durabilityUpperBound = durabilityUpperBound;
  }

  public void setDurabilityBound(int min, int max) {
    setDurabilityLowerBound(min);
    setDurabilityUpperBound(max);
  }

  public Map<Enchantment, Integer> getEnchantMap() {
    return this.rpgItemMeta.enchantMap;
  }

  public void setEnchantMap(Map<Enchantment, Integer> enchantMap) {
    this.rpgItemMeta.enchantMap = enchantMap;
  }

  public File getFile() {
    return file;
  }

  public EnchantMode getEnchantMode() {
    return rpgItemMeta.enchantMode;
  }

  public void setEnchantMode(EnchantMode enchantMode) {
    this.rpgItemMeta.enchantMode = enchantMode;
  }

  void setFile(File itemFile) {
    file = itemFile;
  }

  public int getHitCost() {
    return rpgItemMeta.hitCost;
  }

  public void setHasStackId(boolean hasStackId) {
    this.rpgItemMeta.hasStackId = hasStackId;
  }

  public void setHitCost(int hitCost) {
    this.rpgItemMeta.hitCost = hitCost;
  }

  public int getHittingCost() {
    return rpgItemMeta.hittingCost;
  }

  public void setHittingCost(int hittingCost) {
    this.rpgItemMeta.hittingCost = hittingCost;
  }

  public Material getItem() {
    return rpgItemMeta.item;
  }

  public void setItem(Material material) {
    this.rpgItemMeta.item = material;
  }

  public List<ItemFlag> getItemFlags() {
    return this.rpgItemMeta.itemFlags;
  }

  public void setItemFlags(List<ItemFlag> itemFlags) {
    this.rpgItemMeta.itemFlags = itemFlags;
  }

  public String getLicense() {
    return rpgItemMeta.license;
  }

  public void setLicense(String license) {
    this.rpgItemMeta.license = license;
  }

  public List<String> getLore() {
    return Collections.unmodifiableList(this.rpgItemMeta.lore);
  }

  private void setLore(List<String> lore) {
    this.rpgItemMeta.lore = lore;
  }

  public int getMaxDurability() {
    return rpgItemMeta.maxDurability;
  }

  public void setMaxDurability(int newVal) {
    this.rpgItemMeta.maxDurability = newVal <= 0 ? -1 : newVal;
    setDefaultDurability(this.rpgItemMeta.maxDurability);
  }

  public String getMcVersion() {
    return rpgItemMeta.mcVersion;
  }

  public void setMcVersion(String mcVersion) {
    this.rpgItemMeta.mcVersion = mcVersion;
  }

  public String getName() {
    return name;
  }

  public NamespacedKey getNamespacedKey() {
    return rpgItemMeta.namespacedKey;
  }

  public void setNamespacedKey(NamespacedKey namespacedKey) {
    this.rpgItemMeta.namespacedKey = namespacedKey;
  }

  public String getNote() {
    return rpgItemMeta.note;
  }

  public void setNote(String note) {
    this.rpgItemMeta.note = note;
  }

  public String getPermission() {
    return Strings.isNullOrEmpty(this.rpgItemMeta.permission)
        ? "rpgitems.item.use." + getName()
        : this.rpgItemMeta.permission;
  }

  public void setPermission(String p) {
    this.rpgItemMeta.permission = p;
  }

  public int getPluginVersion() {
    return rpgItemMeta.pluginVersion;
  }

  public void setPluginVersion(int pluginVersion) {
    this.rpgItemMeta.pluginVersion = pluginVersion;
  }

  public int getPluginSerial() {
    return rpgItemMeta.pluginSerial;
  }

  public void setPluginSerial(int pluginSerial) {
    this.rpgItemMeta.pluginSerial = pluginSerial;
  }

  public List<Power> getPowers() {
    return chains.stream()
        .flatMap(powerChain -> powerChain.getPowers().stream())
        .collect(Collectors.toList());
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
    return rpgItemMeta.customItemModel;
  }

  public void setCustomItemModel(boolean customItemModel) {
    this.rpgItemMeta.customItemModel = customItemModel;
  }

  public boolean isHasDurabilityBar() {
    return rpgItemMeta.hasDurabilityBar;
  }

  public void setHasDurabilityBar(boolean hasDurabilityBar) {
    this.rpgItemMeta.hasDurabilityBar = hasDurabilityBar;
  }

  public boolean isHasPermission() {
    return rpgItemMeta.hasPermission;
  }

  public void setHasPermission(boolean b) {
    this.rpgItemMeta.hasPermission = b;
  }

  public boolean isHitCostByDamage() {
    return rpgItemMeta.hitCostByDamage;
  }

  public void setHitCostByDamage(boolean hitCostByDamage) {
    this.rpgItemMeta.hitCostByDamage = hitCostByDamage;
  }

  public boolean isIgnoreWorldGuard() {
    return rpgItemMeta.ignoreWorldGuard;
  }

  public void setIgnoreWorldGuard(boolean ignoreWorldGuard) {
    this.rpgItemMeta.ignoreWorldGuard = ignoreWorldGuard;
  }

  public BarFormat getBarFormat() {
    return rpgItemMeta.barFormat;
  }

  public void setBarFormat(BarFormat barFormat) {
    this.rpgItemMeta.barFormat = barFormat;
  }

  public boolean isShowArmourLore() {
    return rpgItemMeta.showArmourLore;
  }

  public void setShowArmourLore(boolean showArmourLore) {
    this.rpgItemMeta.showArmourLore = showArmourLore;
  }

  public boolean isShowPowerText() {
    return rpgItemMeta.showPowerText;
  }

  public void setShowPowerText(boolean showPowerText) {
    this.rpgItemMeta.showPowerText = showPowerText;
  }

  public void setAttributeMode(AttributeMode attributeMode) {
    this.rpgItemMeta.attributeMode = attributeMode;
  }

  public String getQuality() {
    return rpgItemMeta.quality;
  }

  public void setQuality(String quality) {
    this.rpgItemMeta.quality = quality;
  }

  public String getType() {
    return rpgItemMeta.type;
  }

  public void setType(String type) {
    this.rpgItemMeta.type = type;
  }

  public AttributeMode getAttributeMode() {
    return rpgItemMeta.attributeMode;
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
    FULL_UPDATE,
    PARTIAL_UPDATE;
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
