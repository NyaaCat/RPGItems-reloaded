package think.rpgitems;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.BadCommandException;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import cat.nyaa.nyaacore.utils.OfflinePlayerUtils;
import com.google.common.base.Strings;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import think.rpgitems.item.ItemGroup;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.Trigger;
import think.rpgitems.support.WGSupport;
import think.rpgitems.utils.MaterialUtils;
import think.rpgitems.utils.NetworkUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static think.rpgitems.item.RPGItem.AttributeMode.FULL_UPDATE;
import static think.rpgitems.item.RPGItem.AttributeMode.PARTIAL_UPDATE;
import static think.rpgitems.item.RPGItem.*;
import static think.rpgitems.power.Utils.rethrow;
import static think.rpgitems.utils.ItemTagUtils.getInt;
import static think.rpgitems.utils.ItemTagUtils.getTag;
import static think.rpgitems.utils.NetworkUtils.Location.GIST;

public class AdminCommands extends RPGCommandReceiver {
    private final RPGItems plugin;
    private final Map<String, String> subCommandCompletion = new HashMap<>();

    AdminCommands(RPGItems plugin, I18n i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
        Arrays.stream(getClass().getDeclaredMethods()).forEach(method -> {
            if (method.getAnnotation(SubCommand.class) != null && method.getAnnotation(Completion.class) != null) {
                subCommandCompletion.put(method.getAnnotation(SubCommand.class).value(), method.getAnnotation(Completion.class).value());
            }
        });
    }

    @Completion("")
    public List<String> itemCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(ItemManager.itemNames());
                break;
            case 2:
                String cmd = arguments.getRawArgs()[1];
                if (subCommandCompletion.containsKey(cmd)) {
                    String comp = subCommandCompletion.get(cmd);
                    completeStr.addAll(Arrays.asList(comp.split(":", 2)[1].split(",")));
                }
                break;
        }
        return filtered(arguments, completeStr);
    }

    @Completion("")
    public List<String> attrCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                String cmd = arguments.getRawArgs()[0];
                if (subCommandCompletion.containsKey(cmd)) {
                    String comp = subCommandCompletion.get(cmd);
                    completeStr.addAll(Arrays.asList(comp.split(":", 2)[1].split(",")));
                }
                break;
        }
        return filtered(arguments, completeStr);
    }

    public static List<String> filtered(Arguments arguments, List<String> completeStr) {
        String[] rawArgs = arguments.getRawArgs();
        return completeStr.stream().filter(s -> s.startsWith(rawArgs[rawArgs.length - 1])).collect(Collectors.toList());
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }

    public static void msgs(CommandSender target, String template, Object... args) {
        I18n i18n = I18n.getInstance(target);
        target.sendMessage(i18n.getFormatted(template, args));
    }

    public static void msgs(CommandSender target, String template, Map<String, BaseComponent> map, Object... args) {
        new Message("").append(I18n.getInstance(target).getFormatted(template, args), map).send(target);
    }

    @SubCommand("power")
    public PowerCommands power;

    @SubCommand("gen-wiki")
    public WikiCommand wiki;

    @SubCommand("debug")
    public void debug(CommandSender sender, Arguments args) {
        Player player = asPlayer(sender);
        ItemStack item = player.getInventory().getItemInMainHand();
        player.sendMessage(ItemStackUtils.itemToJson(item).replace(ChatColor.COLOR_CHAR, '&'));
        if (item.getType() == Material.AIR) {
            player.sendMessage("empty");
            return;
        }
        if (!item.hasItemMeta()) {
            player.sendMessage("empty meta");
            return;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer tagContainer = Objects.requireNonNull(meta).getPersistentDataContainer();
        if (tagContainer.has(TAG_META, PersistentDataType.TAG_CONTAINER)) {
            int uid = getInt(getTag(tagContainer, TAG_META), TAG_ITEM_UID);
            player.sendMessage("new item: " + uid);
            Optional<RPGItem> rpgItem = ItemManager.getItem(uid);
            player.sendMessage("rpgItem: " + rpgItem.map(RPGItem::getName).orElse(null));
        }
    }

    @SubCommand("save-all")
    public void save(CommandSender sender, Arguments args) {
        ItemManager.save();
    }

    @SubCommand("reload")
    public void reload(CommandSender sender, Arguments args) {
        plugin.cfg = new Configuration(plugin);
        plugin.cfg.load();
        plugin.cfg.enabledLanguages.forEach(lang -> new I18n(plugin, lang));
        WGSupport.reload();
        ItemManager.reload(plugin);
        plugin.managedPlugins.forEach(Bukkit.getPluginManager()::disablePlugin);
        plugin.managedPlugins.clear();
        plugin.loadExtensions();
        plugin.managedPlugins.forEach(Bukkit.getPluginManager()::enablePlugin);
        sender.sendMessage(ChatColor.GREEN + "[RPGItems] Reloaded RPGItems.");
    }

    @SubCommand("loadfile")
    public void loadFile(CommandSender sender, Arguments args) {
        String path = args.nextString();
        File file = new File(path);
        if (!file.exists()) {
            file = new File(ItemManager.getItemsDir(), path);
            if (!file.exists()) {
                msgs(sender, "message.error.file_not_exists", path);
                return;
            }
        }
        ItemManager.load(file, sender);
    }

    @SubCommand(value = "reloaditem", tabCompleter = "itemCompleter")
    public void reloadItem(CommandSender sender, Arguments args) throws IOException {
        RPGItem item = getItem(args.nextString(), sender, true);
        File file = item.getFile();

        if (plugin.cfg.itemFsLock) {
            Pair<File, FileLock> backup = ItemManager.getBackup(item);
            if (backup == null) {
                msgs(sender, "message.error.reloading_locked");
                return;
            }
            FileLock fileLock = backup.getValue();
            ItemManager.remove(item, false);
            if (!file.exists() || file.isDirectory()) {
                ItemManager.removeLock(item);
                msgs(sender, "message.item.file_deleted");
                return;
            }
            boolean load = ItemManager.load(file, sender);
            if (!load) {
                recoverBackup(sender, item, file, fileLock);
            } else {
                backup.getKey().deleteOnExit();
                ItemManager.removeBackup(item);
                fileLock.release();
                fileLock.channel().close();
            }
        } else {
            ItemManager.remove(item, false);
            boolean load = ItemManager.load(file, sender);
            Pair<File, FileLock> backup = ItemManager.removeBackup(item);
            if (!load) {
                if (backup != null) {
                    recoverBackup(sender, item, file, backup.getValue());
                } else {
                    msgs(sender, "message.item.no_backup", item.getName());
                }
            } else {
                if (backup != null) {
                    backup.getKey().deleteOnExit();
                    backup.getValue().release();
                    backup.getValue().channel().close();
                }
            }
        }
    }

    private void recoverBackup(CommandSender sender, RPGItem item, File file, FileLock fileLock) {
        try {
            File edited = ItemManager.unlockAndBackup(item, true);
            msgs(sender, "message.item.recovering", edited.getPath());
            try (FileChannel backupChannel = fileLock.channel(); FileChannel fileChannel = new FileOutputStream(file).getChannel()) {
                fileChannel.transferFrom(backupChannel, 0, backupChannel.size());
            }
            ItemManager.load(file, sender);
        } catch (IOException e) {
            msgs(sender, "message.error.recovering", item.getName(), file.getPath(), e.getLocalizedMessage());
            plugin.getLogger().log(Level.SEVERE, "Error recovering backup for " + item.getName() + "." + file.getPath(), e);
            rethrow(e);
        }
    }

    @SubCommand(value = "backupitem", tabCompleter = "itemCompleter")
    public void unlockItem(CommandSender sender, Arguments args) throws IOException {
        RPGItem item = getItem(args.nextString(), sender);
        File backup = ItemManager.unlockAndBackup(item, false);
        boolean itemFsLock = plugin.cfg.itemFsLock;

        FileLock lock = ItemManager.lockFile(backup);
        if (itemFsLock && lock == null) {
            plugin.getLogger().severe("Error locking " + backup + ".");
            ItemManager.lock(item.getFile());
            throw new IllegalStateException();
        }
        ItemManager.addBackup(item, Pair.of(backup, lock));
        if (itemFsLock) {
            msgs(sender, "message.item.unlocked", item.getFile().getPath(), backup.getPath());
        } else {
            msgs(sender, "message.item.backedup", item.getFile().getPath(), backup.getPath());
        }
    }

    @SubCommand("cleanbackup")
    public void cleanBackup(CommandSender sender, Arguments args) throws IOException {
        if (!ItemManager.hasBackup()) {
            throw new BadCommandException("message.error.item_unlocked", ItemManager.getUnlockedItem().stream().findFirst().orElseThrow(IllegalStateException::new).getName());
        }
        Files.walkFileTree(ItemManager.getBackupsDir().toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toFile().isFile() && file.getFileName().toString().endsWith(".bak")) {
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        msgs(sender, "message.item.cleanbackup");
    }

    public static Pair<Integer, Integer> getPaging(int size, int perPage, Arguments args) {
        int max = (int) Math.ceil(size / (double) perPage);
        int page = args.top() == null ? 1 : args.nextInt();
        if (!(0 < page && page <= max)) {
            throw new BadCommandException("message.num_out_of_range", page, 0, max);
        }
        return Pair.of(max, page);
    }

    @SubCommand(value = "list", tabCompleter = "attrCompleter")
    @Completion("command:name:,display:,type:")
    public void listItems(CommandSender sender, Arguments args) {
        int perPage = RPGItems.plugin.cfg.itemPerPage;
        String nameSearch = args.argString("n", args.argString("name", ""));
        String displaySearch = args.argString("d", args.argString("display", ""));
        List<RPGItem> items = ItemManager.items()
                                         .stream()
                                         .filter(i -> i.getName().contains(nameSearch))
                                         .filter(i -> i.getDisplayName().contains(displaySearch))
                                         .sorted(Comparator.comparing(RPGItem::getName))
                                         .collect(Collectors.toList());
        if (items.size() == 0) {
            msgs(sender, "message.no_item");
            return;
        }
        Pair<Integer, Integer> maxPage = getPaging(items.size(), perPage, args);
        int page = maxPage.getValue();
        int max = maxPage.getKey();
        Stream<RPGItem> stream =
                items.stream()
                     .skip((page - 1) * perPage)
                     .limit(perPage);
        sender.sendMessage(ChatColor.AQUA + "RPGItems: " + page + " / " + max);

        stream.forEach(
                item -> new Message("")
                                .append(I18n.getInstance(sender).format("message.item.list", item.getName()), Collections.singletonMap("{item}", item.getComponent(sender)))
                                .send(sender)
        );
    }

    @SubCommand("worldguard")
    public void toggleWorldGuard(CommandSender sender, Arguments args) {
        if (!WGSupport.hasSupport()) {
            msgs(sender, "message.worldguard.error");
            return;
        }
        if (WGSupport.useWorldGuard) {
            msgs(sender, "message.worldguard.disable");
        } else {
            msgs(sender, "message.worldguard.enable");
        }
        WGSupport.useWorldGuard = !WGSupport.useWorldGuard;
        RPGItems.plugin.cfg.useWorldGuard = WGSupport.useWorldGuard;
        RPGItems.plugin.cfg.save();
    }

    @SubCommand("wgforcerefresh")
    public void toggleForceRefresh(CommandSender sender, Arguments args) {
        if (!WGSupport.hasSupport()) {
            msgs(sender, "message.worldguard.error");
            return;
        }
        if (WGSupport.forceRefresh) {
            msgs(sender, "message.wgforcerefresh.disable");
        } else {
            msgs(sender, "message.wgforcerefresh.enable");
        }
        WGSupport.forceRefresh = !WGSupport.forceRefresh;
        RPGItems.plugin.cfg.wgForceRefresh = WGSupport.forceRefresh;
        RPGItems.plugin.cfg.save();
    }

    @SubCommand(value = "wgignore", tabCompleter = "itemCompleter")
    public void itemToggleWorldGuard(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if (!WGSupport.hasSupport()) {
            msgs(sender, "message.worldguard.error");
            return;
        }
        item.setIgnoreWorldGuard(!item.isIgnoreWorldGuard());
        if (item.isIgnoreWorldGuard()) {
            msgs(sender, "message.worldguard.override.active");
        } else {
            msgs(sender, "message.worldguard.override.disabled");
        }
        ItemManager.save(item);
    }

    @SubCommand("create")
    public void createItem(CommandSender sender, Arguments args) {
        String itemName = args.nextString();
        RPGItem newItem = ItemManager.newItem(itemName.toLowerCase(), sender);
        if (newItem != null) {
            msgs(sender, "message.create.ok", itemName);
            ItemManager.save(newItem);
        } else {
            msgs(sender, "message.create.fail");
        }
    }

    @SubCommand("giveperms")
    public void givePerms(CommandSender sender, Arguments args) {
        RPGItems.plugin.cfg.givePerms = !RPGItems.plugin.cfg.givePerms;
        if (RPGItems.plugin.cfg.givePerms) {
            msgs(sender, "message.giveperms.required");
        } else {
            msgs(sender, "message.giveperms.canceled");
        }
        RPGItems.plugin.cfg.save();
    }

    @SubCommand(value = "give", tabCompleter = "itemCompleter")
    public void giveItem(CommandSender sender, Arguments args) {
        String str = args.nextString();
        Optional<RPGItem> optItem = ItemManager.getItem(str);
        if (optItem.isPresent()) {
            RPGItem item = optItem.get();
            if ((plugin.cfg.givePerms || !sender.hasPermission("rpgitem")) && (!plugin.cfg.givePerms || !sender.hasPermission("rpgitem.give." + item.getName()))) {
                msgs(sender, "message.error.permission", str);
                return;
            }
            if (args.length() == 2) {
                if (sender instanceof Player) {
                    item.give((Player) sender, 1, false);
                    msgs(sender, "message.give.ok", item.getDisplayName());
                } else {
                    msgs(sender, "message.give.console");
                }
            } else {
                Player player = args.nextPlayer();
                int count;
                try {
                    count = args.nextInt();
                } catch (BadCommandException e) {
                    count = 1;
                }
                item.give(player, count, false);
                msgs(sender, "message.give.to", item.getDisplayName() + ChatColor.AQUA, player.getName());
                msgs(player, "message.give.ok", item.getDisplayName());
            }
        } else {
            Optional<ItemGroup> optGroup = ItemManager.getGroup(str);
            if (!optGroup.isPresent()) {
                throw new BadCommandException("message.error.item", str);
            }
            ItemGroup group = optGroup.get();
            if ((plugin.cfg.givePerms || !sender.hasPermission("rpgitem")) && (!plugin.cfg.givePerms || !sender.hasPermission("rpgitem.give.group." + group.getName()))) {
                msgs(sender, "message.error.permission", str);
                return;
            }
            if (sender instanceof Player) {
                group.give(args.nextPlayerOrSender(), 1, true);
                msgs(sender, "message.give.ok", group.getName());
            } else {
                msgs(sender, "message.give.console");
            }
        }

    }

    @SubCommand(value = "remove", tabCompleter = "itemCompleter")
    public void removeItem(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        ItemManager.remove(item, true);
        msgs(sender, "message.remove.ok", item.getName());
    }

    @SubCommand(value = "display", tabCompleter = "itemCompleter")
    public void itemDisplay(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        String value = args.consume();
        if (value != null) {
            item.setDisplayName(value);
            msgs(sender, "message.display.set", item.getName(), item.getDisplayName());
            ItemManager.refreshItem();
            ItemManager.save(item);
        } else {
            msgs(sender, "message.display.get", item.getName(), item.getDisplayName());
        }
    }

    @SubCommand(value = "damage", tabCompleter = "itemCompleter")
    public void itemDamage(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        try {
            int damageMin = args.nextInt();
            int damageMax;
            if (damageMin > 32767) {
                msgs(sender, "message.error.damagetolarge");
                return;
            }
            try {
                damageMax = args.nextInt();
            } catch (BadCommandException e) {
                damageMax = damageMin;
            }
            item.setDamage(damageMin, damageMax);
            if (damageMin != damageMax) {
                msgs(sender, "message.damage.set.range", item.getName(), item.getDamageMin(), item.getDamageMax());
            } else {
                msgs(sender, "message.damage.set.value", item.getName(), item.getDamageMin());
            }
            ItemManager.refreshItem();
            ItemManager.save(item);
        } catch (BadCommandException e) {
            msgs(sender, "message.damage.get", item.getName(), item.getDamageMin(), item.getDamageMax());
        }
    }

    @SubCommand(value = "armour", tabCompleter = "itemCompleter")
    public void itemArmour(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        try {
            int armour = args.nextInt();
            item.setArmour(armour);
            msgs(sender, "message.armour.set", item.getName(), item.getArmour());
            ItemManager.refreshItem();
            ItemManager.save(item);
        } catch (BadCommandException e) {
            msgs(sender, "message.armour.get", item.getName(), item.getArmour());
        }
    }

    @SubCommand(value = "item", tabCompleter = "itemCompleter")
    public void itemItem(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if (args.length() == 2) {
            new Message("")
                    .append(I18n.getInstance(sender).format("message.item.get", item.getName(), item.getItem().name(), item.getDataValue()), new ItemStack(item.getItem()))
                    .send(sender);
        } else if (args.length() >= 3) {
            String materialName = args.nextString();
            Material material = MaterialUtils.getMaterial(materialName, sender);
            if (material == null || !material.isItem()) {
                msgs(sender, "message.error.material", materialName);
                return;
            }
            item.setItem(material);
            if (args.length() == 4) {
                int dataValue;
                try {
                    dataValue = Integer.parseInt(args.top());
                } catch (Exception e) {
                    String hexColour = "";
                    try {
                        hexColour = args.nextString();
                        dataValue = Integer.parseInt(hexColour, 16);
                    } catch (NumberFormatException e2) {
                        sender.sendMessage(ChatColor.RED + "Failed to parse " + hexColour);
                        return;
                    }
                }
                item.setDataValue(dataValue);
            }
            item.rebuild();
            ItemManager.refreshItem();

            new Message("")
                    .append(I18n.getInstance(sender).format("message.item.set", item.getName(), item.getItem().name(), item.getDataValue()), new ItemStack(item.getItem()))
                    .send(sender);
            ItemManager.save(item);
        }
    }

    @SubCommand(value = "print", tabCompleter = "itemCompleter")
    public void itemInfo(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender, true);
        item.print(sender);
    }

    @SubCommand(value = "enchantment", tabCompleter = "itemCompleter")
    @Completion("item:clone,clear")
    public void itemEnchant(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if (args.length() == 2) {
            if (item.getEnchantMap() != null) {
                msgs(sender, "message.enchantment.listing", item.getName());
                if (item.getEnchantMap().size() == 0) {
                    msgs(sender, "message.enchantment.empty_ench");
                } else {
                    for (Enchantment ench : item.getEnchantMap().keySet()) {
                        msgs(sender, "message.enchantment.item",
                                ench.getKey().toString(), item.getEnchantMap().get(ench));
                    }
                }
            } else {
                msgs(sender, "message.enchantment.no_ench");
            }
        }
        String command = args.nextString();
        switch (command) {
            case "clone": {
                if (sender instanceof Player) {
                    ItemStack hand = ((Player) sender).getInventory().getItemInMainHand();
                    if (hand.getType() == Material.AIR) {
                        msgs(sender, "message.enchantment.fail");
                    } else {
                        if (hand.getType() == Material.ENCHANTED_BOOK) {
                            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) hand.getItemMeta();
                            item.setEnchantMap(meta.getStoredEnchants());
                        } else if (hand.hasItemMeta()) {
                            item.setEnchantMap(new HashMap<>(hand.getItemMeta().getEnchants()));
                        } else {
                            item.setEnchantMap(Collections.emptyMap());
                        }
                        item.rebuild();
                        ItemManager.refreshItem();
                        ItemManager.save(item);
                        msgs(sender, "message.enchantment.success");
                    }
                } else {
                    msgs(sender, "message.enchantment.fail");
                }
            }
            break;
            case "clear": {
                item.setEnchantMap(null);
                item.rebuild();
                ItemManager.refreshItem();
                ItemManager.save(item);
                msgs(sender, "message.enchantment.removed");
            }
            break;
            default:
                throw new BadCommandException("message.error.invalid_option", command, "enchantment", "clone,clear");
        }
    }

    @SubCommand(value = "attributemode", tabCompleter = "itemCompleter")
    @Completion("item:FULL_UPDATE,PARTIAL_UPDATE")
    public void setAttributeMode(CommandSender sender, Arguments arguments) {
        RPGItem item = getItem(arguments.nextString(), sender);
        switch (arguments.top()) {
            case "FULL_UPDATE":
                item.setAttributeMode(FULL_UPDATE);
                new Message("").append(I18n.getInstance(sender).format("message.attributemode.set", "FULL_UPDATE"), item.toItemStack())
                               .send(sender);
                break;
            case "PARTIAL_UPDATE":
                item.setAttributeMode(PARTIAL_UPDATE);
                new Message("").append(I18n.getInstance(sender).format("message.attributemode.set", "PARTIAL_UPDATE"), item.toItemStack())
                               .send(sender);
                break;
            default:
                throw new BadCommandException("accepted value: FULL_UPDATE,PARTIAL_UPDATE");
        }
        ItemManager.save(item);
    }

    @SubCommand(value = "description", tabCompleter = "itemCompleter")
    @Completion("item:add,insert,set,remove")
    public void itemAddDescription(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        String command = args.nextString();
        switch (command) {
            case "add": {
                String line = args.consumeString();
                item.addDescription(ChatColor.WHITE + line);
                msgs(sender, "message.description.ok");
                ItemManager.refreshItem();
                ItemManager.save(item);
            }
            break;
            case "insert": {
                int lineNo = args.nextInt();
                String line = args.consumeString();
                int Length = item.getDescription().size();
                if (lineNo < 0 || lineNo >= Length) {
                    msgs(sender, "message.num_out_of_range", lineNo, 0, item.getDescription().size());
                    return;
                }
                item.getDescription().add(lineNo, ChatColor.translateAlternateColorCodes('&', ChatColor.WHITE + line));
                item.rebuild();
                ItemManager.refreshItem();
                msgs(sender, "message.description.ok");
                ItemManager.save(item);
            }
            break;
            case "set": {
                int lineNo = args.nextInt();
                String line = args.consumeString();
                if (lineNo < 0 || lineNo >= item.getDescription().size()) {
                    msgs(sender, "message.num_out_of_range", lineNo, 0, item.getDescription().size());
                    return;
                }
                item.getDescription().set(lineNo, ChatColor.translateAlternateColorCodes('&', ChatColor.WHITE + line));
                item.rebuild();
                ItemManager.refreshItem();
                msgs(sender, "message.description.change");
                ItemManager.save(item);
            }
            break;
            case "remove": {
                int lineNo = args.nextInt();
                if (lineNo < 0 || lineNo >= item.getDescription().size()) {
                    msgs(sender, "message.num_out_of_range", lineNo, 0, item.getDescription().size());
                    break;
                }
                item.getDescription().remove(lineNo);
                item.rebuild();
                ItemManager.refreshItem();
                msgs(sender, "message.description.remove");
                ItemManager.save(item);
            }
            break;
            default:
                throw new BadCommandException("message.error.invalid_option", command, "description", "add,set,remove");
        }
    }

    @SubCommand(value = "cost", tabCompleter = "itemCompleter")
    @Completion("item:breaking,hitting,hit,toggle")
    public void itemCost(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        String type = args.nextString();
        if (args.length() == 3) {
            switch (type) {
                case "breaking":
                    msgs(sender, "message.cost.get", item.getBlockBreakingCost());
                    break;
                case "hitting":
                    msgs(sender, "message.cost.get", item.getHittingCost());
                    break;
                case "hit":
                    msgs(sender, "message.cost.get", item.getHitCost());
                    break;
                case "toggle":
                    item.setHitCostByDamage(!item.isHitCostByDamage());
                    ItemManager.save(item);
                    msgs(sender, "message.cost.hit_toggle." + (item.isHitCostByDamage() ? "enable" : "disable"));
                    break;
                default:
                    throw new BadCommandException("message.error.invalid_option", type, "cost", "breaking,hitting,hit,toggle");
            }
        } else {
            int newValue = args.nextInt();
            switch (type) {
                case "breaking":
                    item.setBlockBreakingCost(newValue);
                    break;
                case "hitting":
                    item.setHittingCost(newValue);
                    break;
                case "hit":
                    item.setHitCost(newValue);
                    break;
                default:
                    throw new BadCommandException("message.error.invalid_option", type, "cost", "breaking,hitting,hit");
            }

            ItemManager.save(item);
            msgs(sender, "message.cost.change");
        }
    }

    @SubCommand(value = "durability", tabCompleter = "itemCompleter")
    @Completion("item:infinite,default,bound")
    public void itemDurability(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if (args.length() == 2) {
            msgs(sender, "message.durability.info", item.getMaxDurability(), item.getDefaultDurability(), item.getDurabilityLowerBound(), item.getDurabilityUpperBound());
            return;
        }
        String arg = args.nextString();
        try {
            int durability = Integer.parseInt(arg);
            item.setMaxDurability(durability);
            ItemManager.refreshItem();
            ItemManager.save(item);
            msgs(sender, "message.durability.max_and_default", String.valueOf(durability));
        } catch (NumberFormatException e) {
            switch (arg) {
                case "infinite": {
                    item.setMaxDurability(-1);
                    ItemManager.refreshItem();
                    ItemManager.save(item);
                    msgs(sender, "message.durability.max_and_default", "infinite");
                }
                break;
                case "default": {
                    int durability = args.nextInt();
                    if (durability <= 0) {
                        // Actually we don't check max here
                        throw new CommandException("message.num_out_of_range", durability, 0, item.getMaxDurability());
                    }
                    item.setDefaultDurability(durability);
                    ItemManager.refreshItem();
                    ItemManager.save(item);
                    msgs(sender, "message.durability.default", String.valueOf(durability));
                }
                break;
                case "bound": {
                    int min = args.nextInt();
                    int max = args.nextInt();
                    item.setDurabilityBound(min, max);
                    ItemManager.refreshItem();
                    ItemManager.save(item);
                    msgs(sender, "message.durability.bound", String.valueOf(min), String.valueOf(max));
                }
                break;
                default:
                    throw new BadCommandException("message.error.invalid_option", arg, "durability", "value,infinite,togglebar,default,bound");
            }
        }
    }

    @SubCommand(value = "permission", tabCompleter = "itemCompleter")
    public void setPermission(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        String permission = args.nextString();
        boolean enabled = args.nextBoolean();
        item.setPermission(permission);
        item.setHasPermission(enabled);
        ItemManager.save(item);
        msgs(sender, "message.permission.success");
    }

    @SubCommand(value = "togglepowerlore", tabCompleter = "itemCompleter")
    public void togglePowerLore(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        item.setShowPowerText(!item.isShowPowerText());
        item.rebuild();
        ItemManager.refreshItem();
        ItemManager.save(item);
        msgs(sender, "message.toggleLore." + (item.isShowPowerText() ? "show" : "hide"));
    }

    @SubCommand(value = "togglearmorlore", tabCompleter = "itemCompleter")
    public void toggleArmorLore(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        item.setShowArmourLore(!item.isShowArmourLore());
        item.rebuild();
        ItemManager.refreshItem();
        ItemManager.save(item);
        msgs(sender, "message.toggleLore." + (item.isShowArmourLore() ? "show" : "hide"));
    }

    @SubCommand(value = "additemflag", tabCompleter = "itemCompleter")
    public void addItemFlag(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        ItemFlag flag = args.nextEnum(ItemFlag.class);
        item.getItemFlags().add(ItemFlag.valueOf(flag.name()));
        item.rebuild();
        ItemManager.refreshItem();
        ItemManager.save(item);
        msgs(sender, "message.itemflag.add", flag.name());
    }

    @SubCommand(value = "removeitemflag", tabCompleter = "itemCompleter")
    public void removeItemFlag(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        ItemFlag flag = args.nextEnum(ItemFlag.class);
        ItemFlag itemFlag = ItemFlag.valueOf(flag.name());
        if (item.getItemFlags().contains(itemFlag)) {
            item.getItemFlags().remove(itemFlag);
            item.rebuild();
            ItemManager.refreshItem();
            ItemManager.save(item);
            msgs(sender, "message.itemflag.remove", flag.name());
        } else {
            msgs(sender, "message.itemflag.notfound", flag.name());
        }
    }

    @SubCommand(value = "customitemmodel", tabCompleter = "itemCompleter")
    public void toggleCustomItemModel(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        item.setCustomItemModel(!item.isCustomItemModel());
        item.rebuild();
        ItemManager.refreshItem();
        ItemManager.save(item);
        msgs(sender, "message.customitemmodel." + (item.isCustomItemModel() ? "enable" : "disable"));
    }

    @SubCommand(value = "togglebar", tabCompleter = "itemCompleter")
    public void toggleBar(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        item.toggleBar();
        ItemManager.refreshItem();
        ItemManager.save(item);
        msgs(sender, "message.durability.toggle");
    }

    @SubCommand(value = "barformat", tabCompleter = "itemCompleter")
    @Completion("item:DEFAULT,NUMERIC,NUMERIC_MINUS_ONE,NUMERIC_HEX,NUMERIC_HEX_MINUS_ONE,DEFAULT8")
    public void toggleBarFormat(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        item.setBarFormat(args.nextEnum(BarFormat.class));
        item.rebuild();
        ItemManager.refreshItem();
        ItemManager.save(item);
        msgs(sender, "message.barformat." + item.getBarFormat().name());
    }

    @SubCommand("version")
    public void printVersion(CommandSender sender, Arguments args) {
        msgs(sender, "message.version", RPGItems.plugin.getDescription().getVersion());
    }

    @SubCommand(value = "enchantmode", tabCompleter = "itemCompleter")
    @Completion("item:DISALLOW,PERMISSION,ALLOW")
    public void toggleItemEnchantMode(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if (args.top() != null) {
            item.setEnchantMode(args.nextEnum(RPGItem.EnchantMode.class));
            item.rebuild();
            ItemManager.refreshItem();
            ItemManager.save(item);
        }
        msgs(sender, "message.enchantmode." + item.getEnchantMode().name(), item.getName());
    }

    @SubCommand(value = "damagemode", tabCompleter = "itemCompleter")
    @Completion("item:FIXED,VANILLA,ADDITIONAL,MULTIPLY")
    public void toggleItemDamageMode(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if (args.top() != null) {
            item.setDamageMode(args.nextEnum(RPGItem.DamageMode.class));
            item.rebuild();
            ItemManager.refreshItem();
            ItemManager.save(item);
        }
        msgs(sender, "message.damagemode." + item.getDamageMode().name(), item.getName());
    }

    private Pair<NamespacedKey, Class<? extends Marker>> getMarkerClass(CommandSender sender, String markerStr) {
        try {
            NamespacedKey key = PowerManager.parseKey(markerStr);
            Class<? extends Marker> cls = PowerManager.getMarker(key);
            if (cls == null) {
                msgs(sender, "message.power.unknown", markerStr);
            }
            return Pair.of(key, cls);
        } catch (UnknownExtensionException e) {
            msgs(sender, "message.error.unknown.extension", e.getName());
            return null;
        }
    }

    private Pair<NamespacedKey, Class<? extends Condition>> getConditionClass(CommandSender sender, String conditionStr) {
        try {
            NamespacedKey key = PowerManager.parseKey(conditionStr);
            Class<? extends Condition> cls = PowerManager.getCondition(key);
            if (cls == null) {
                msgs(sender, "message.power.unknown", conditionStr);
            }
            return Pair.of(key, cls);
        } catch (UnknownExtensionException e) {
            msgs(sender, "message.error.unknown.extension", e.getName());
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    @SubCommand("trigger")
    @Completion("power")
    public void itemAddTrigger(CommandSender sender, Arguments args) {
        String itemStr = args.next();
        String powerStr = args.next();
        if (itemStr == null || itemStr.equals("help")) {
            msgs(sender, "manual.trigger.description");
            msgs(sender, "manual.trigger.usage");
            return;
        }
        if (powerStr == null || powerStr.equals("list")) {
            RPGItem item = getItem(itemStr, sender);
            for (Map.Entry<String, Trigger> t : item.getTriggers().entrySet()) {
                Trigger trigger = t.getValue();
                msgs(sender, "message.item.trigger", t.getKey(), trigger.getLocalizedName(plugin.cfg.language), trigger.getNamespacedKey().toString());
                if ("list".equals(powerStr)) {
                    PowerManager.getProperties(trigger.getNamespacedKey()).forEach(
                            (name, prop) -> showProp(sender, trigger.getNamespacedKey(), prop.getValue(), trigger)
                    );
                }
            }
            return;
        }
        String name = args.next();
        RPGItem item = getItem(itemStr, sender);
        Trigger base = Trigger.get(powerStr);
        if (base == null) {
            msgs(sender, "message.error.no_trigger", powerStr, String.join(", ", Trigger.keySet()));
            return;
        }
        Trigger trigger = base.copy(name);
        trigger.setItem(item);
        try {
            trigger = setPropertyHolder(sender, args, base.getClass(), trigger, true);
            item.addTrigger(name, trigger);
            ItemManager.refreshItem();
            ItemManager.save(item);
            msgs(sender, "message.power.ok");
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            plugin.getLogger().log(Level.WARNING, "Error adding power " + powerStr + " to item " + itemStr + " " + item, e);
            msgs(sender, "internal.error.command_exception");
        }
    }

    public static <T extends PropertyHolder> T initPropertyHolder(CommandSender sender, Arguments args, RPGItem item, Class<? extends T> cls) throws IllegalAccessException {
        T power = PowerManager.instantiate(cls);
        power.setItem(item);
        power.init(new YamlConfiguration());
        return setPropertyHolder(sender, args, cls, power, true);
    }

    public static <T extends PropertyHolder> T setPropertyHolder(CommandSender sender, Arguments args, Class<? extends T> cls, T power, boolean checkRequired) throws IllegalAccessException {
        Map<String, Pair<Method, PropertyInstance>> argMap = PowerManager.getProperties(cls);

        List<Field> required = argMap.values().stream()
                                     .map(Pair::getValue)
                                     .filter(PropertyInstance::required)
                                     .sorted(Comparator.comparing(PropertyInstance::order))
                                     .map(PropertyInstance::field)
                                     .collect(Collectors.toList());

        for (Map.Entry<String, Pair<Method, PropertyInstance>> prop : argMap.entrySet()) {
            Field field = prop.getValue().getValue().field();
            String name = prop.getKey();
            String value = args.argString(name, null);
            if (value != null) {
                Utils.setPowerProperty(sender, power, field, value);
                required.remove(field);
            }
        }
        if (checkRequired && !required.isEmpty()) {
            throw new BadCommandException("message.property.required",
                    required.stream().map(Field::getName).collect(Collectors.joining(", "))
            );
        }
        return power;
    }

    @SubCommand(value = "clone", tabCompleter = "itemCompleter")
    public void cloneItem(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender, true);
        String name = args.nextString();
        RPGItem i = ItemManager.cloneItem(item, name);
        if (i != null) {
            ItemManager.save(i);
            msgs(sender, "message.cloneitem.success", item.getName(), i.getName());
        } else {
            msgs(sender, "message.cloneitem.fail", item.getName(), name);
        }
    }

    @SubCommand(value = "import", tabCompleter = "attrCompleter")
    @Completion("command:GIST")
    public void download(CommandSender sender, Arguments args) {
        NetworkUtils.Location location = args.nextEnum(NetworkUtils.Location.class);
        String id = args.nextString();
        switch (location) {
            case GIST:
                downloadGist(sender, args, id);
                break;
            case URL:
                downloadUrl(sender, args, id);
                break;
            default:
                msgs(sender, "message.import.not_supported", location.name());
        }
    }

    @SubCommand(value = "export", tabCompleter = "itemCompleter")
    @Completion("item:GIST")
    public void publish(CommandSender sender, Arguments args) {
        String itemsStr = args.nextString();
        NetworkUtils.Location location = args.top() == null ? GIST : args.nextEnum(NetworkUtils.Location.class);
        Set<String> items = Stream.of(itemsStr.split(",")).collect(Collectors.toSet());

        switch (location) {
            case GIST:
                publishGist(sender, args, items);
                break;
            case URL:
                throw new NotImplementedException();
            default:
                msgs(sender, "message.export.not_supported", location.name());
        }
    }

    @SubCommand(value = "author", tabCompleter = "itemCompleter")
    public void setAuthor(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        String author = args.next();
        if (author != null) {
            BaseComponent authorComponent = new TextComponent(author);
            String authorName = author.startsWith("@") ? author.substring(1) : author;
            Optional<OfflinePlayer> maybeAuthor = Optional.ofNullable(OfflinePlayerUtils.lookupPlayer(authorName));
            if (maybeAuthor.isPresent()) {
                OfflinePlayer authorPlayer = maybeAuthor.get();
                author = authorPlayer.getUniqueId().toString();
                authorComponent = getAuthorComponent(authorPlayer, authorName);
            } else if (author.startsWith("@")) {
                msgs(sender, "message.error.player", author);
                return;
            }
            item.setAuthor(author);
            msgs(sender, "message.item.author.set", Collections.singletonMap("{author}", authorComponent), item.getName());
            ItemManager.save(item);
        } else {
            String authorText = item.getAuthor();
            if (Strings.isNullOrEmpty(authorText)) {
                msgs(sender, "message.item.author.na", item.getName());
                return;
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                BaseComponent authorComponent = new TextComponent(authorText);
                try {
                    UUID uuid = UUID.fromString(authorText);
                    OfflinePlayer authorPlayer = Bukkit.getOfflinePlayer(uuid);
                    String authorName = authorPlayer.getName() == null ? OfflinePlayerUtils.lookupPlayerNameByUuidOnline(uuid).get(2, TimeUnit.SECONDS) : authorPlayer.getName();
                    authorComponent = getAuthorComponent(authorPlayer, authorName);
                } catch (IllegalArgumentException | InterruptedException | ExecutionException | TimeoutException ignored) {
                }
                msgs(sender, "message.item.author.get", Collections.singletonMap("{author}", authorComponent), item.getName());
            });
        }
    }

    public static BaseComponent getAuthorComponent(OfflinePlayer authorPlayer, String authorName) {
        if (authorName == null) {
            authorName = authorPlayer.getUniqueId().toString();
        }
        BaseComponent authorComponent = new TextComponent(authorName);
        authorComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ENTITY,
                new ComponentBuilder(Message.getPlayerJson(authorPlayer)).create()
        ));
        return authorComponent;
    }

    @SubCommand(value = "note", tabCompleter = "itemCompleter")
    public void setNote(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        String note = args.next();
        if (note != null) {
            item.setNote(note);
            msgs(sender, "message.item.note.set", item.getName(), note);
            ItemManager.save(item);
        } else {
            msgs(sender, "message.item.note.get", item.getName(), item.getNote());
        }
    }

    @SubCommand(value = "license", tabCompleter = "itemCompleter")
    public void setLicense(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        String license = args.next();
        if (license != null) {
            item.setLicense(license);
            msgs(sender, "message.item.license.set", item.getName(), license);
            ItemManager.save(item);
        } else {
            msgs(sender, "message.item.license.get", item.getName(), item.getLicense());
        }
    }

    @SubCommand(value = "dump", tabCompleter = "itemCompleter")
    public void dumpItem(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender, true);
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        item.save(yamlConfiguration);
        String s = yamlConfiguration.saveToString();
        msgs(sender, "message.item.dump", item.getName(), s.replace(ChatColor.COLOR_CHAR + "", "\\u00A7"));
    }

    @SubCommand("creategroup")
    public void createGroup(CommandSender sender, Arguments args) {
        String groupName = args.nextString();
        ItemGroup group = null;
        if (args.top() == null || !args.top().contains("/")) {
            group = ItemManager.newGroup(groupName, sender);
            if (group == null) {
                msgs(sender, "message.create.fail");
                return;
            }
            while (args.top() != null) {
                RPGItem item = getItem(args.nextString(), sender, true);
                group.addItem(item);
            }
            ItemManager.save(group);
        } else {
            String regex = args.next();
            if (!regex.startsWith("/") || !regex.endsWith("/")) {
                msgs(sender, "message.error.invalid_regex");
                return;
            } else {
                regex = regex.substring(1, regex.length() - 1);
            }
            group = ItemManager.newGroup(groupName, regex, sender);
            if (group == null) {
                msgs(sender, "message.create.fail");
                return;
            }
            ItemManager.save(group);
        }
        Set<RPGItem> items = group.getItems();
        msgs(sender, "message.group.header", group.getName(), items.size());
        for (RPGItem item : items) {
            new Message("")
                    .append(I18n.getInstance(sender).format("message.item.list", item.getName()), Collections.singletonMap("{item}", item.getComponent(sender)))
                    .send(sender);
        }
    }

    @SubCommand(value = "addtogroup", tabCompleter = "itemCompleter")
    public void addToGroup(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender, true);
        String groupName = args.nextString();
        Optional<ItemGroup> optGroup = ItemManager.getGroup(groupName);
        if (!optGroup.isPresent()) {
            msgs(sender, "message.error.item", groupName);
            return;
        }
        ItemGroup group = optGroup.get();
        group.addItem(item);
        msgs(sender, "message.group.header", group.getName(), group.getItemUids().size());
        ItemManager.save(group);
    }

    @SubCommand(value = "removefromgroup", tabCompleter = "itemCompleter")
    public void removeFromGroup(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender, true);
        String groupName = args.nextString();
        Optional<ItemGroup> optGroup = ItemManager.getGroup(groupName);
        if (!optGroup.isPresent()) {
            msgs(sender, "message.error.item", groupName);
            return;
        }
        ItemGroup group = optGroup.get();
        group.removeItem(item);
        msgs(sender, "message.group.header", group.getName(), group.getItemUids().size());
        ItemManager.save(group);
    }

    @SubCommand("listgroup")
    public void listGroup(CommandSender sender, Arguments args) {
        String groupName = args.nextString();
        Optional<ItemGroup> optGroup = ItemManager.getGroup(groupName);
        if (!optGroup.isPresent()) {
            msgs(sender, "message.error.item", groupName);
            return;
        }
        ItemGroup group = optGroup.get();
        Set<RPGItem> items = group.getItems();
        msgs(sender, "message.group.header", group.getName(), items.size());
        if (!Strings.isNullOrEmpty(group.getNote())) {
            msgs(sender, "message.group.note", group.getNote());
        }
        for (RPGItem item : items) {
            new Message("")
                    .append(I18n.getInstance(sender).format("message.item.list", item.getName()), Collections.singletonMap("{item}", item.getComponent(sender)))
                    .send(sender);
        }
    }

    @SubCommand("removegroup")
    public void removeGroup(CommandSender sender, Arguments args) {
        String groupName = args.nextString();
        Optional<ItemGroup> optGroup = ItemManager.getGroup(groupName);
        if (!optGroup.isPresent()) {
            msgs(sender, "message.error.item", groupName);
            return;
        }
        ItemGroup group = optGroup.get();
        ItemManager.remove(group, true);
        msgs(sender, "message.group.removed", group.getName());
    }

    public static RPGItem getItem(String str, CommandSender sender) {
        return getItem(str, sender, false);
    }

    public static RPGItem getItem(String str, CommandSender sender, boolean readOnly) {
        Optional<RPGItem> item = ItemManager.getItem(str);
        if (!item.isPresent()) {
            try {
                item = ItemManager.getItem(Integer.parseInt(str));
            } catch (NumberFormatException ignored) {
            }
        }
        if (!item.isPresent() && sender instanceof Player && str.equalsIgnoreCase("hand")) {
            Player p = (Player) sender;
            item = ItemManager.toRPGItem(p.getInventory().getItemInMainHand(), false);
        }
        if (item.isPresent()) {
            if (ItemManager.isUnlocked(item.get()) && !readOnly) {
                throw new BadCommandException("message.error.item_unlocked", item.get().getName());
            }
            return item.get();
        } else {
            throw new BadCommandException("message.error.item", str);
        }
    }

    private void publishGist(CommandSender sender, Arguments args, Set<String> itemNames) {
        List<Pair<String, RPGItem>> items = itemNames.stream().map(i -> Pair.of(i, getItem(i, sender))).collect(Collectors.toList());
        Optional<Pair<String, RPGItem>> unknown = items.stream().filter(p -> p.getValue() == null).findFirst();
        if (unknown.isPresent()) {
            throw new BadCommandException("message.error.item", unknown.get().getKey());
        }
        String token = args.argString("token", plugin.cfg.githubToken);
        if (Strings.isNullOrEmpty(token)) {
            throw new BadCommandException("message.export.gist.token");
        }
        boolean isPublish = Boolean.parseBoolean(args.argString("publish", String.valueOf(plugin.cfg.publishGist)));
        String description = args.argString("description",
                "RPGItems exported item: " + String.join(",", itemNames)
        );
        Map<String, Map<String, String>> result = new HashMap<>(items.size());
        items.forEach(
                pair -> {
                    RPGItem item = pair.getValue();
                    String name = pair.getKey();
                    YamlConfiguration conf = new YamlConfiguration();
                    item.save(conf);
                    conf.set("id", null);
                    String itemConf = conf.saveToString();
                    String filename = ItemManager.getItemFilename(name, "-item") + ".yml";
                    Map<String, String> content = new HashMap<>();
                    content.put("content", itemConf);
                    result.put(filename, content);
                }
        );
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String id = NetworkUtils.publishGist(result, token, description, isPublish);
                Bukkit.getScheduler().runTask(plugin, () -> msgs(sender, "message.export.gist.ed", id));
            } catch (InterruptedException | ExecutionException e) {
                plugin.getLogger().log(Level.WARNING, "Error exporting gist", e);
                Bukkit.getScheduler().runTask(plugin, () -> msgs(sender, "message.export.gist.failed"));
            } catch (TimeoutException e) {
                plugin.getLogger().log(Level.WARNING, "Timeout exporting gist", e);
                Bukkit.getScheduler().runTask(plugin, () -> msgs(sender, "message.export.gist.timeout"));
            } catch (BadCommandException e) {
                sender.sendMessage(e.getLocalizedMessage());
            }
        });
    }

    private void downloadGist(CommandSender sender, Arguments args, String id) {
        new Message(I18n.getInstance(sender).format("message.import.gist.ing")).send(sender);
        String token = args.argString("token", plugin.cfg.githubToken);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, String> gist;
            try {
                gist = NetworkUtils.downloadGist(id, token);
                Bukkit.getScheduler().runTask(plugin, () -> loadItems(sender, gist, args));
            } catch (InterruptedException | ExecutionException e) {
                plugin.getLogger().log(Level.WARNING, "Error importing gist", e);
                Bukkit.getScheduler().runTask(plugin, () -> new Message(I18n.getInstance(sender).format("message.import.gist.failed")).send(sender));
            } catch (TimeoutException e) {
                plugin.getLogger().log(Level.WARNING, "Timeout importing gist", e);
                Bukkit.getScheduler().runTask(plugin, () -> new Message(I18n.getInstance(sender).format("message.import.gist.timeout")).send(sender));
            } catch (BadCommandException e) {
                sender.sendMessage(e.getLocalizedMessage());
            }
        });
    }

    private void downloadUrl(CommandSender sender, Arguments args, String url) {
        new Message(I18n.getInstance(sender).format("message.import.url.ing")).send(sender);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<String, String> itemConf = NetworkUtils.downloadUrl(url);
                Bukkit.getScheduler().runTask(plugin, () -> loadItems(sender, itemConf, args));
            } catch (InterruptedException | ExecutionException e) {
                plugin.getLogger().log(Level.WARNING, "Error importing url", e);
                Bukkit.getScheduler().runTask(plugin, () -> new Message(I18n.getInstance(sender).format("message.import.url.failed")).send(sender));
            } catch (TimeoutException e) {
                plugin.getLogger().log(Level.WARNING, "Timeout importing url", e);
                Bukkit.getScheduler().runTask(plugin, () -> new Message(I18n.getInstance(sender).format("message.import.url.timeout")).send(sender));
            } catch (BadCommandException e) {
                sender.sendMessage(e.getLocalizedMessage());
            }
            throw new NotImplementedException(url);
        });
    }

    private void loadItems(CommandSender sender, Map<String, String> confs, Arguments args) {
        List<RPGItem> items = new ArrayList<>(confs.size());
        for (Map.Entry<String, String> entry : confs.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            YamlConfiguration itemStorage = new YamlConfiguration();
            try {
                itemStorage.set("id", null);
                itemStorage.loadFromString(v);
                String origin = itemStorage.getString("name");
                int uid = itemStorage.getInt("uid");

                if (uid >= 0 || origin == null) {
                    throw new InvalidConfigurationException();
                }

                String name = args.argString(origin, origin);

                if (ItemManager.hasId(uid)) {
                    Optional<RPGItem> currentItem = ItemManager.getItem(uid);
                    if (currentItem.isPresent()) {
                        msgs(sender, "message.import.conflict_uid", origin, currentItem.get().getName(), uid);
                    } else {
                        Optional<ItemGroup> currentGroup = ItemManager.getGroup(uid);
                        msgs(sender, "message.import.conflict_uid", origin, currentGroup.get().getName(), uid);
                    }
                    return;
                }
                if (ItemManager.hasName(name)) {
                    msgs(sender, "message.import.conflict_name", name);
                    return;
                }

                RPGItem item = new RPGItem(itemStorage, name, uid);
                items.add(item);
            } catch (InvalidConfigurationException e) {
                plugin.getLogger().log(Level.WARNING, "Trying to load invalid config in " + k, e);
                msgs(sender, "message.import.invalid_conf", k);
                return;
            } catch (UnknownPowerException e) {
                msgs(sender, "message.power.unknown", e.getKey().toString());
                return;
            } catch (UnknownExtensionException e) {
                msgs(sender, "message.error.unknown.extension", e.getName());
                return;
            }
        }
        for (RPGItem item : items) {
            ItemManager.addItem(item);
            msgs(sender, "message.import.success", item.getName(), item.getUid());
        }
        ItemManager.save();
    }

    public static class CommandException extends BadCommandException {
        private final String msg_internal;

        public CommandException(String msg_internal, Object... args) {
            super(msg_internal, args);
            this.msg_internal = msg_internal;
        }

        public CommandException(String msg_internal, Throwable cause, Object... args) {
            super(msg_internal, cause, args);
            this.msg_internal = msg_internal;
        }

        @Override
        public String toString() {
            StringBuilder keyBuilder = new StringBuilder("CommandException<" + msg_internal + ">");
            if (objs != null) {
                for (Object obj : objs) {
                    keyBuilder.append("#<").append(obj).append(">");
                }
            }
            return keyBuilder.toString();
        }

        @Override
        public String getMessage() {
            return toString();
        }

        @Override
        public String getLocalizedMessage() {
            return I18n.getInstance(RPGItems.plugin.cfg.language).format(msg_internal, objs);
        }
    }
}
