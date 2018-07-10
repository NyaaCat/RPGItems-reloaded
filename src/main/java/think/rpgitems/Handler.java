package think.rpgitems;

import cat.nyaa.nyaacore.LanguageRepository;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import think.rpgitems.commands.*;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.Quality;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;
import think.rpgitems.support.WorldGuard;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static think.rpgitems.power.Power.*;

public class Handler extends RPGCommandReceiver {
    private final RPGItems plugin;

    Handler(RPGItems plugin, LanguageRepository i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    private static void setPower(Power power, String field, String value) throws BadCommandException, IllegalAccessException {
        Field f;
        Class<? extends Power> cls = power.getClass();
        try {
            f = cls.getField(field);
        } catch (NoSuchFieldException e) {
            throw new BadCommandException("internal.error.invalid_command_arg", e);//TODO
        }
        setPower(power, f, value);
    }

    @SuppressWarnings("unchecked")
    private static void setPower(Power power, Field field, String value) throws BadCommandException, IllegalAccessException {
        Class<? extends Power> cls = power.getClass();
        Transformer tf = field.getAnnotation(Transformer.class);
        if (tf != null) {
            value = transformers.get(cls, tf.value()).apply(power, value);
        }
        BooleanChoice bc = field.getAnnotation(BooleanChoice.class);
        if (bc != null) {
            String trueChoice = bc.trueChoice();
            String falseChoice = bc.falseChoice();
            if (value.equalsIgnoreCase(trueChoice) || value.equalsIgnoreCase(falseChoice)) {
                field.set(power, value.equalsIgnoreCase(trueChoice));
            } else {
                throw new BadCommandException("message.error.invalid_option", field.getName(), falseChoice + ", " + trueChoice);//TODO
            }
            return;
        }
        List<String> rest;
        AcceptedValue as = field.getAnnotation(AcceptedValue.class);
        if (as != null) {
            rest = Arrays.asList(as.value());
            if (!rest.contains(value)) {
                throw new BadCommandException("message.error.invalid_option", field.getName(), rest.stream().reduce(" ", (a, b) -> a + ", " + b));
            }
        }
        Validator ck = field.getAnnotation(Validator.class);
        if (ck != null) {
            Boolean b = validators.get(cls, ck.value()).apply(power, value);
            if (!b) {
                throw new BadCommandException("message.error.invalid_option", field.getName(), I18n.format(ck.message(), value));
            }
        }
        Setter st = field.getAnnotation(Setter.class);
        if (st != null) {
            setters.get(cls, st.value()).accept(power, value);
        } else {
            if (field.getType() == int.class) {
                try {
                    field.set(power, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    throw new BadCommandException("internal.error.bad_int", value);
                }
            } else if (field.getType() == long.class) {
                try {
                    field.set(power, Long.parseLong(value));
                } catch (NumberFormatException e) {
                    throw new BadCommandException("internal.error.bad_int", value);
                }
            } else if (field.getType() == double.class) {
                try {
                    field.set(power, Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    throw new BadCommandException("internal.error.bad_double", value);
                }
            } else if (field.getType() == String.class) {
                field.set(power, value);
            } else if (field.getType() == boolean.class) {
                if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                    field.set(power, Boolean.valueOf(value));
                } else {
                    throw new BadCommandException("message.error.invalid_option", field.getName(), "true, false");
                }
            } else if (field.getType().isEnum()) {
                try {
                    field.set(power, Enum.valueOf((Class<Enum>) field.getType(), value));
                } catch (IllegalArgumentException e) {
                    throw new BadCommandException("internal.error.bad_enum", field.getName(), Stream.of(field.getType().getEnumConstants()).map(Object::toString).reduce(" ", (a, b) -> a + ", " + b));
                }
            } else {
                throw new BadCommandException("internal.error.invalid_command_arg");
            }
        }
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }

    @SubCommand("reload")
    @Attribute("command")
    public void reload(CommandSender sender, Arguments args) {
        plugin.reloadConfig();
        plugin.i18n.load();
        WorldGuard.reload();
        ItemManager.reload();
        if (plugin.getConfig().getBoolean("localeInv", false)) {
            Events.useLocaleInv = true;
        }
        sender.sendMessage(ChatColor.GREEN + "[RPGItems] Reloaded RPGItems.");
    }

    @SubCommand("list")
    @Attribute("command")
    public void listItems(CommandSender sender, Arguments args) {
        Collection<RPGItem> items = ItemManager.itemByName.values();
        int perPage = RPGItems.plugin.getConfig().getInt("itemperpage", 9);
        Stream<RPGItem> stream = ItemManager.itemByName.values().stream();

        if (args.length() != 1) {
            int max = (int) Math.ceil(items.size() / (double) perPage);
            int page = args.nextInt();
            if (!(0 < page || page <= max)) {
                throw new BadCommandException("message.num_out_of_range", page, 0, max);
            }
            stream = ItemManager.itemByName.values().stream().skip((page - 1) * perPage).limit(page);
            sender.sendMessage(ChatColor.AQUA + "RPGItems: " + page + " / " + max);
        }

        stream.forEach(item -> new Message("").append(I18n.format("message.item.list", item.getName()), item.toItemStack()).send(sender)
        );
    }

    @SubCommand("worldguard")
    @Attribute("command")
    public void toggleWorldGuard(CommandSender sender, Arguments args) {
        if (!WorldGuard.isEnabled()) {
            msg(sender, "message.worldguard.error");
            return;
        }
        if (WorldGuard.useWorldGuard) {
            msg(sender, "message.worldguard.disable");
        } else {
            msg(sender, "message.worldguard.enable");
        }
        WorldGuard.useWorldGuard = !WorldGuard.useWorldGuard;
        RPGItems.plugin.getConfig().set("support.worldguard", WorldGuard.useWorldGuard);
        RPGItems.plugin.saveConfig();
    }

    @SubCommand("wgcustomflag")
    @Attribute("command")
    public void toggleCustomFlag(CommandSender sender, Arguments args) {
        if (!WorldGuard.isEnabled()) {
            msg(sender, "message.worldguard.error");
            return;
        }
        if (WorldGuard.useCustomFlag) {
            msg(sender, "message.wgcustomflag.disable");
        } else {
            msg(sender, "message.wgcustomflag.enable");
        }
        WorldGuard.useCustomFlag = !WorldGuard.useCustomFlag;
        RPGItems.plugin.getConfig().set("support.wgcustomflag", WorldGuard.useCustomFlag);
        RPGItems.plugin.saveConfig();
    }

    @SubCommand("wgforcerefresh")
    @Attribute("command")
    public void toggleForceRefresh(CommandSender sender, Arguments args) {
        if (!WorldGuard.isEnabled()) {
            msg(sender, "message.worldguard.error");
            return;
        }
        if (WorldGuard.forceRefresh) {
            msg(sender, "message.wgforcerefresh.disable");
        } else {
            msg(sender, "message.wgforcerefresh.enable");
        }
        WorldGuard.forceRefresh = !WorldGuard.forceRefresh;
        RPGItems.plugin.getConfig().set("support.wgforcerefresh", WorldGuard.forceRefresh);
        RPGItems.plugin.saveConfig();
    }

    @SubCommand("wgignore")
    @Attribute("item")
    public void itemToggleWorldGuard(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        if (!WorldGuard.isEnabled()) {
            msg(sender, "message.worldguard.error");
            return;
        }
        item.ignoreWorldGuard = !item.ignoreWorldGuard;
        if (item.ignoreWorldGuard) {
            msg(sender, "message.worldguard.override.active");
        } else {
            msg(sender, "message.worldguard.override.disabled");
        }
        ItemManager.save(RPGItems.plugin);
    }

    @SubCommand("create")
    @Attribute("item")
    public void createItem(CommandSender sender, Arguments args) {
        String itemName = args.nextString();
        if (ItemManager.newItem(itemName.toLowerCase()) != null) {
            msg(sender, "message.create.ok", itemName);
            ItemManager.save(RPGItems.plugin);
        } else {
            msg(sender, "message.create.fail");
        }
    }

    @SubCommand("giveperms")
    @Attribute("command")
    public void givePerms(CommandSender sender, Arguments args) {
        RPGItems.plugin.getConfig().set("give-perms", !RPGItems.plugin.getConfig().getBoolean("give-perms", false));
        if (RPGItems.plugin.getConfig().getBoolean("give-perms", false)) {
            msg(sender, "message.giveperms.required");
        } else {
            msg(sender, "message.giveperms.canceled");
        }
        RPGItems.plugin.saveConfig();
    }

    @SubCommand("give")
    @Attribute("item")
    public void giveItem(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        if (args.length() == 2) {
            if (sender instanceof Player) {
                if ((!RPGItems.plugin.getConfig().getBoolean("give-perms", false) && sender.hasPermission("rpgitem")) || (RPGItems.plugin.getConfig().getBoolean("give-perms", false) && sender.hasPermission("rpgitem.give." + item.getName()))) {
                    item.give((Player) sender);
                    msg(sender, "message.give.ok", item.getDisplay());
                } else {
                    msg(sender, "message.error.permission", item.getDisplay());
                }
            } else {
                msg(sender, "message.give.console");
            }
        } else {
            Player player = args.nextPlayer();
            int count;
            try {
                count = args.nextInt();
            } catch (BadCommandException e) {
                count = 1;
            }
            for (int i = 0; i < count; i++) {
                item.give(player);
            }

            msg(sender, "message.give.to", item.getDisplay() + ChatColor.AQUA, player.getName());
            msg(player, "message.give.ok", item.getDisplay());
        }
    }

    @SubCommand("remove")
    @Attribute("item")
    public void removeItem(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        ItemManager.remove(item);
        msg(sender, "message.remove.ok", item.getName());
        ItemManager.save(RPGItems.plugin);
    }

    @SubCommand("display")
    @Attribute("item")
    public void itemDisplay(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        String value = args.next();
        if (value != null) {
            item.setDisplay(value);
            msg(sender, "message.display.set", item.getName(), item.getDisplay());
            ItemManager.save(RPGItems.plugin);
        } else {
            msg(sender, "message.display.get", item.getName(), item.getDisplay());
        }
    }

    @SubCommand("quality")
    @Attribute("item")
    public void itemQuality(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        try {
            Quality quality = args.nextEnum(Quality.class);
            item.setQuality(quality);
            msg(sender, "message.quality.set", item.getName(), item.getQuality().toString().toLowerCase());
            ItemManager.save(RPGItems.plugin);
        } catch (BadCommandException e) {
            msg(sender, "message.quality.get", item.getName(), item.getQuality().toString().toLowerCase());
        }
    }

    @SubCommand("damage")
    @Attribute("item")
    public void itemDamage(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        try {
            int damageMin = args.nextInt();
            int damageMax;
            if (damageMin > 32767) {
                msg(sender, "message.error.damagetolarge");
                return;
            }
            try {
                damageMax = args.nextInt();
            } catch (BadCommandException e) {
                damageMax = damageMin;
            }
            item.setDamage(damageMin, damageMax);
            if (damageMin != damageMax) {
                msg(sender, "message.damage.set.range", item.getName(), item.getDamageMin(), item.getDamageMax());
            } else {
                msg(sender, "message.damage.set.value", item.getName(), item.getDamageMin());
            }
            ItemManager.save(RPGItems.plugin);
        } catch (BadCommandException e) {
            msg(sender, "message.damage.get", item.getName(), item.getDamageMin(), item.getDamageMax());
        }
    }

    @SubCommand("armour")
    @Attribute("item")
    public void itemArmour(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        try {
            int armour = args.nextInt();
            item.setArmour(armour);
            msg(sender, "message.armour.set", item.getName(), item.getArmour());
            ItemManager.save(RPGItems.plugin);
        } catch (BadCommandException e) {
            msg(sender, "message.armour.get", item.getName(), item.getArmour());
        }
    }

    @SubCommand("type")
    @Attribute("item")
    public void itemType(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        String type = args.next();
        if (type != null) {
            item.setType(type);
            msg(sender, "message.type.set", item.getName(), item.getType());
            ItemManager.save(RPGItems.plugin);
        } else {
            msg(sender, "message.type.get", item.getName(), item.getType());
        }
    }

    @SubCommand("hand")
    @Attribute("item")
    public void itemHand(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        String type = args.next();
        if (type != null) {
            item.setHand(type);
            msg(sender, "message.hand.set", item.getName(), item.getType());
            ItemManager.save(RPGItems.plugin);
        } else {
            msg(sender, "message.hand.get", item.getName(), item.getType());
        }
    }

    @SubCommand("item")
    @Attribute("item")
    public void itemItem(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        if (args.length() == 2) {
            new Message("")
                    .append(I18n.format("message.item.get", item.getName(), item.getItem().name(), (int) item.getDataValue()), new ItemStack(item.getItem()))
                    .send(sender);
        } else if (args.length() >= 3) {
            String materialName = args.nextString();
            Material material = Material.matchMaterial(materialName);
            if (material == null || !ReflectionUtils.isValidItem(new ItemStack(material))) {
                msg(sender, "message.error.material", materialName);
                return;
            }
            item.setItem(material, false);
            if (args.length() == 4) {
                int dam;
                try {
                    dam = Integer.parseInt(args.top());
                } catch (Exception e) {
                    String hexColour = "";
                    try {
                        hexColour = args.nextString();
                        dam = Integer.parseInt(hexColour, 16);
                    } catch (NumberFormatException e2) {
                        sender.sendMessage(ChatColor.RED + "Failed to parse " + hexColour);
                        return;
                    }
                }
                ItemMeta meta = item.getLocaleMeta();
                if (meta instanceof LeatherArmorMeta) {
                    ((LeatherArmorMeta) meta).setColor(Color.fromRGB(dam));
                } else {
                    item.setDataValue((short) dam);
                }
                item.updateLocaleMeta(meta);
            }
            item.rebuild();

            new Message("")
                    .append(I18n.format("message.item.set", item.getName(), item.getItem().name(), (int) item.getDataValue()), new ItemStack(item.getItem()))
                    .send(sender);
            ItemManager.save(RPGItems.plugin);
        }
    }

    @SubCommand("print")
    @Attribute("item")
    public void itemInfo(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        item.print(sender);
    }

    @SubCommand("enchantment")
    @Attribute("item:clone,clear")
    public void itemListEnchant(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        if (args.length() == 2) {
            if (item.enchantMap != null) {
                msg(sender, "message.enchantment.listing", item.getName());
                if (item.enchantMap.size() == 0) {
                    msg(sender, "message.enchantment.empty_ench");
                } else {
                    for (Enchantment ench : item.enchantMap.keySet()) {
                        msg(sender, "message.enchantment.item",
                                ench.getName(), item.enchantMap.get(ench));
                    }
                }
            } else {
                msg(sender, "message.enchantment.no_ench");
            }
        }
        String command = args.nextString();
        switch (command) {
            case "clone": {
                if (sender instanceof Player) {
                    ItemStack hand = ((Player) sender).getInventory().getItemInMainHand();
                    if (hand == null || hand.getType() == Material.AIR) {
                        msg(sender, "message.enchantment.fail");
                    } else {
                        if (hand.hasItemMeta()) {
                            item.enchantMap = new HashMap<>(hand.getItemMeta().getEnchants());
                        } else {
                            item.enchantMap = Collections.emptyMap();
                        }
                        item.rebuild();
                        ItemManager.save(RPGItems.plugin);
                        msg(sender, "message.enchantment.success");
                    }
                } else {
                    msg(sender, "message.enchantment.fail");
                }
            }
            break;
            case "clear": {
                item.enchantMap = null;
                item.rebuild();
                ItemManager.save(RPGItems.plugin);
                msg(sender, "message.enchantment.removed");
            }
            break;
            default:
                throw new BadCommandException("message.error.invalid_option", "enchantment", "clone,clear");
        }
    }

    @SubCommand("removepower")
    @Attribute("power")
    public void itemRemovePower(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        String power = args.nextString();
        if (item.removePower(power)) {
            Power.powerUsage.remove(power);
            msg(sender, "message.power.removed", power);
            ItemManager.save(RPGItems.plugin);
        } else {
            msg(sender, "message.power.unknown", power);
        }
    }

    @SubCommand("description")
    @Attribute("item:add,set,remove")
    public void itemAddDescription(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        String command = args.nextString();
        switch (command) {
            case "add": {
                String line = args.nextString();
                item.addDescription(ChatColor.WHITE + line);
                msg(sender, "message.description.ok");
                ItemManager.save(RPGItems.plugin);
            }
            break;
            case "set": {
                int lineNo = args.nextInt();
                String line = args.nextString();
                if (lineNo < 0 || lineNo >= item.description.size()) {
                    msg(sender, "message.num_out_of_range", lineNo, 0, item.description.size());
                    return;
                }
                item.description.set(lineNo, ChatColor.translateAlternateColorCodes('&', ChatColor.WHITE + line));
                item.rebuild();
                msg(sender, "message.description.change");
                ItemManager.save(RPGItems.plugin);
            }
            break;
            case "remove": {
                int lineNo = args.nextInt();
                if (lineNo < 0 || lineNo >= item.description.size()) {
                    msg(sender, "message.num_out_of_range", lineNo, 0, item.description.size());
                    return;
                }
                item.description.remove(lineNo);
                item.rebuild();
                msg(sender, "message.description.remove");
                ItemManager.save(RPGItems.plugin);
            }
            break;
            default:
                throw new BadCommandException("message.error.invalid_option", "description", "add,set,remove");
        }
    }

    @SubCommand("removerecipe")
    @Attribute("item")
    public void itemRemoveRecipe(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        item.hasRecipe = false;
        item.resetRecipe(true);
        ItemManager.save(RPGItems.plugin);
        msg(sender, "message.recipe.removed");
    }

    @SubCommand("recipe")
    @Attribute("item")
    public void itemSetRecipe(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        int chance = args.nextInt();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String title = "RPGItems - " + item.getDisplay();
            if (title.length() > 32) {
                title = title.substring(0, 32);
            }
            Inventory recipeInventory = Bukkit.createInventory(player, 27, title);
            if (item.hasRecipe) {
                ItemStack blank = new ItemStack(Material.THIN_GLASS);
                ItemMeta meta = blank.getItemMeta();
                meta.setDisplayName(I18n.format("message.recipe.1"));
                ArrayList<String> lore = new ArrayList<>();
                lore.add(I18n.format("message.recipe.2"));
                lore.add(I18n.format("message.recipe.3"));
                lore.add(I18n.format("message.recipe.4"));
                lore.add(I18n.format("message.recipe.5"));
                meta.setLore(lore);
                blank.setItemMeta(meta);
                for (int i = 0; i < 27; i++) {
                    recipeInventory.setItem(i, blank);
                }
                for (int x = 0; x < 3; x++) {
                    for (int y = 0; y < 3; y++) {
                        int i = x + y * 9;
                        ItemStack it = item.recipe.get(x + y * 3);
                        if (it != null)
                            recipeInventory.setItem(i, it);
                        else
                            recipeInventory.setItem(i, null);
                    }
                }
            }
            item.setRecipeChance(chance);
            player.openInventory(recipeInventory);
            Events.recipeWindows.put(player.getName(), item.getID());
        } else {
            msg(sender, "message.error.only.player");
        }
    }

    @SubCommand("drop")
    @Attribute("item")
    public void getItemDropChance(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        EntityType type = args.nextEnum(EntityType.class);
        if (args.length() == 3) {
            msg(sender, "message.drop.get", item.getDisplay(), type.toString().toLowerCase(), item.dropChances.get(type.toString()));
        } else {
            double chance = args.nextDouble();
            chance = Math.min(chance, 100.0);
            String typeS = type.toString();
            if (chance > 0) {
                item.dropChances.put(typeS, chance);
                if (!Events.drops.containsKey(typeS)) {
                    Events.drops.put(typeS, new HashSet<>());
                }
                Set<Integer> set = Events.drops.get(typeS);
                set.add(item.getID());
            } else {
                item.dropChances.remove(typeS);
                if (Events.drops.containsKey(typeS)) {
                    Set<Integer> set = Events.drops.get(typeS);
                    set.remove(item.getID());
                }
            }
            ItemManager.save(RPGItems.plugin);
            msg(sender, "message.drop.set", item.getDisplay(), typeS.toLowerCase(), item.dropChances.get(typeS));
        }
    }

    @SubCommand("get")
    @Attribute("property")
    public void getItemPowerProperty(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        String power = args.nextString();
        int nth = args.nextInt();
        String property = args.nextString();
        int i = nth;
        Class<? extends Power> p = Power.powers.get(power);
        if (p == null) {
            msg(sender, "message.power.unknown", power);
            return;
        }
        for (Power pow : item.powers) {
            if (p.isInstance(pow) && --i == 0) {
                try {
                    Field pro = p.getField(property);
                    String val = pro.get(pow).toString();
                    msg(sender, "message.power_property.get", nth, power, property, val);
                } catch (Exception e) {
                    msg(sender, "message.power_property.property_notfound", property);
                    return;
                }
                return;
            }
        }
        msg(sender, "message.power_property.power_notfound");
    }

    @SubCommand("set")
    @Attribute("property")
    public void setItemPowerProperty(CommandSender sender, Arguments args) throws IllegalAccessException {
        RPGItem item = getItemByName(args.nextString());
        String power = args.nextString();
        int nth = args.nextInt();
        String property = args.nextString();
        String val = args.nextString();
        Class<? extends Power> p = Power.powers.get(power);
        if (p == null) {
            msg(sender, "message.power.unknown", power);
            return;
        }
        Optional<Power> op = item.powers.stream().filter(pwr -> pwr.getClass().equals(p)).skip(nth - 1).findFirst();
        if (op.isPresent()) {
            Power pow = op.get();
            setPower(pow, property, val);
        } else {
            msg(sender, "message.power_property.power_notfound");
            return;
        }
        item.rebuild();
        ItemManager.save(RPGItems.plugin);
        msg(sender, "message.power_property.change");
    }

    @SubCommand("cost")
    @Attribute("item:breaking,hitting,hit,toggle")
    public void itemCost(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        String type = args.nextString();
        if (args.length() == 3) {
            switch (type) {
                case "breaking":
                    msg(sender, "message.cost.get", item.blockBreakingCost);
                    break;
                case "hitting":
                    msg(sender, "message.cost.get", item.hittingCost);
                    break;
                case "hit":
                    msg(sender, "message.cost.get", item.hitCost);
                    break;
                case "toggle":
                    item.hitCostByDamage = !item.hitCostByDamage;
                    ItemManager.save(RPGItems.plugin);
                    msg(sender, "message.cost.hit_toggle." + (item.hitCostByDamage ? "enable" : "disable"));
                    break;
                default:
                    throw new BadCommandException("message.error.invalid_option", "cost", "breaking,hitting,hit,toggle");
            }
        } else {
            int newValue = args.nextInt();
            switch (type) {
                case "breaking":
                    item.blockBreakingCost = newValue;
                    ItemManager.save(RPGItems.plugin);
                    msg(sender, "message.cost.change");
                    break;
                case "hitting":
                    item.hittingCost = newValue;
                    ItemManager.save(RPGItems.plugin);
                    msg(sender, "message.cost.change");
                    break;
                case "hit":
                    item.hitCost = newValue;
                    ItemManager.save(RPGItems.plugin);
                    msg(sender, "message.cost.change");
                    break;
                default:
                    throw new BadCommandException("message.error.invalid_option", "cost", "breaking,hitting,hit");
            }
        }
    }

    @SubCommand("durability")
    @Attribute("item:infinite,togglebar,default,bound")
    public void itemDurability(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        if (args.length() == 2) {
            msg(sender, "message.durability.info", item.getMaxDurability(), item.defaultDurability, item.durabilityLowerBound, item.durabilityUpperBound);
            return;
        }
        String arg = args.nextString();
        try {
            int durability = Integer.parseInt(arg);
            item.setMaxDurability(durability);
            ItemManager.save(RPGItems.plugin);
            msg(sender, "message.durability.change");
        } catch (Exception e) {
            switch (arg) {
                case "infinite": {
                    item.setMaxDurability(-1);
                    ItemManager.save(RPGItems.plugin);
                    msg(sender, "message.durability.change");
                }
                break;
                case "togglebar": {
                    item.toggleBar();
                    ItemManager.save(RPGItems.plugin);
                    msg(sender, "message.durability.toggle");
                }
                break;
                case "default": {
                    int durability = args.nextInt();
                    item.setDefaultDurability(durability);
                    ItemManager.save(RPGItems.plugin);
                    msg(sender, "message.durability.change");
                }
                break;
                case "bound": {
                    int min = args.nextInt();
                    int max = args.nextInt();
                    item.setDurabilityBound(min, max);
                    ItemManager.save(RPGItems.plugin);
                    msg(sender, "message.durability.change");
                }
                break;
                default:
                    throw new BadCommandException("message.error.invalid_option", "durability", "value,infinite,togglebar,default,bound");
            }
        }
    }

    @SubCommand("permission")
    @Attribute("item")
    public void setPermission(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        String permission = args.next();
        boolean enabled = args.nextBoolean();
        item.setPermission(permission);
        item.setHaspermission(enabled);
        ItemManager.save(RPGItems.plugin);
        msg(sender, "message.permission.success");
    }

    @SubCommand("togglepowerlore")
    @Attribute("item")
    public void togglePowerLore(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        item.showPowerLore = !item.showPowerLore;
        item.rebuild();
        ItemManager.save(RPGItems.plugin);
        msg(sender, "message.toggleLore." + (item.showPowerLore ? "show" : "hide"));
    }

    @SubCommand("togglearmorlore")
    @Attribute("item")
    public void toggleArmorLore(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        item.showArmourLore = !item.showArmourLore;
        item.rebuild();
        ItemManager.save(RPGItems.plugin);
        msg(sender, "message.toggleLore." + (item.showArmourLore ? "show" : "hide"));
    }

    @SubCommand("additemflag")
    @Attribute("item")
    public void addItemFlag(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        think.rpgitems.item.ItemFlag flag = args.nextEnum(think.rpgitems.item.ItemFlag.class);
        item.itemFlags.add(ItemFlag.valueOf(flag.name()));
        item.rebuild();
        ItemManager.save(RPGItems.plugin);
        msg(sender, "message.itemflag.add", flag.name());
    }

    @SubCommand("removeitemflag")
    @Attribute("item")
    public void removeItemFlag(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        think.rpgitems.item.ItemFlag flag = args.nextEnum(think.rpgitems.item.ItemFlag.class);
        ItemFlag itemFlag = ItemFlag.valueOf(flag.name());
        if (item.itemFlags.contains(itemFlag)) {
            item.itemFlags.remove(itemFlag);
            item.rebuild();
            ItemManager.save(RPGItems.plugin);
            msg(sender, "message.itemflag.remove", flag.name());
        } else {
            msg(sender, "message.itemflag.notfound", flag.name());
        }
    }

    @SubCommand("customitemmodel")
    @Attribute("item")
    public void toggleCustomItemModel(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        item.customItemModel = !item.customItemModel;
        item.rebuild();
        ItemManager.save(RPGItems.plugin);
        msg(sender, "message.customitemmodel." + (item.customItemModel ? "enable" : "disable"));
    }

    @SubCommand("version")
    @Attribute("command")
    public void printVersion(CommandSender sender, Arguments args) {
        msg(sender, "message.version", RPGItems.plugin.getDescription().getVersion());
    }

    @SubCommand("damagemode")
    @Attribute("item")
    public void toggleItemDamageMode(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        if (args.top() != null) {
            item.damageMode = args.nextEnum(RPGItem.DamageMode.class);
            item.rebuild();
            ItemManager.save(RPGItems.plugin);
        }
        msg(sender, "message.damagemode." + item.damageMode.name(), item.getName());
    }

    @SubCommand("power")
    @Attribute("power")
    public void itemAddPower(CommandSender sender, Arguments args) throws IllegalAccessException {
        String itemStr = args.next();
        String powerStr = args.next();
        if (itemStr == null || (itemStr.equals("help") && ItemManager.getItemByName(itemStr) == null) || powerStr == null || powerStr.equals("help")) {
            // TODO: List Available Power
            msg(sender, "manual.power.description");
            msg(sender, "manual.power.usage");
            return;
        }
        if ((itemStr.equals("list") && ItemManager.getItemByName(itemStr) == null) || powerStr.equals("list")) {
            // TODO: List Item Power
            msg(sender, "manual.power.description");
            msg(sender, "manual.power.usage");
            return;
        }
        RPGItem item = getItemByName(itemStr);
        Class<? extends Power> cls = Power.powers.get(powerStr);
        if (cls == null) {
            msg(sender, "message.power.unknown", powerStr);
            return;
        }
        Power power;
        try {
            power = cls.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            msg(sender, "internal.error.command_exception");
            return;
        }
        SortedMap<PowerProperty, Field> argMap = Power.propertyOrders.get(cls);
        Set<Field> settled = new HashSet<>();
        Optional<PowerProperty> req = argMap.keySet()
                                            .stream()
                                            .filter(PowerProperty::required)
                                            .reduce((first, second) -> second); //findLast

        Set<Field> required = req.map(r -> argMap.entrySet()
                                                 .stream()
                                                 .filter(entry -> entry.getKey().order() <= r.order())
                                                 .map(Map.Entry::getValue)
                                                 .collect(Collectors.toSet())).orElse(Collections.emptySet());

        for (Field field : argMap.values()) {
            String name = field.getName();
            String value = args.argString(name, null);
            if (value != null) {
                setPower(power, field, value);
                required.remove(field);
                settled.add(field);
            }
        }
        for (Field field : argMap.entrySet()
                                 .stream()
                                 .filter(p -> p.getKey().order() != Integer.MAX_VALUE)
                                 .map(Map.Entry::getValue)
                                 .collect(Collectors.toList())) {
            if (settled.contains(field)) continue;
            String value = args.next();
            if (value == null) {
                if (!required.isEmpty()) {
                    throw new BadCommandException("message.power.required",
                            String.join(", ",
                                    required.stream().map(Field::getName).collect(Collectors.toList()))
                    );
                } else {
                    break;
                }
            }
            setPower(power, field, value);
            required.remove(field);
            settled.add(field);
        }
        power.item = item;
        item.addPower(power);
        msg(sender, "message.power.ok");
        ItemManager.save(RPGItems.plugin);
    }

    private RPGItem getItemByName(String name) {
        RPGItem item = ItemManager.getItemByName(name);
        if (item != null) {
            return item;
        } else {
            throw new BadCommandException("message.error.item", name);
        }
    }

    @SubCommand("clone")
    @Attribute("item")
    public void cloneItem(CommandSender sender, Arguments args) {
        RPGItem item = getItemByName(args.nextString());
        String name = args.nextString();
        RPGItem i = ItemManager.cloneItem(item, name);
        ItemManager.save(plugin);
        if (i != null) {
            msg(sender, "message.cloneitem.success", item.getName(), i.getName());
        } else {
            msg(sender, "message.cloneitem.fail", item.getName(), name);
        }
    }

}
