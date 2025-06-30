package think.rpgitems;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.BadCommandException;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import cat.nyaa.nyaacore.utils.HexColorUtils;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import cat.nyaa.nyaacore.utils.OfflinePlayerUtils;
import com.google.common.base.Strings;
import com.udojava.evalex.Expression;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.item.ItemGroup;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;
import think.rpgitems.support.PlaceholderAPISupport;
import think.rpgitems.support.WGSupport;
import think.rpgitems.utils.InventoryUtils;
import think.rpgitems.utils.MaterialUtils;
import think.rpgitems.utils.NetworkUtils;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URISyntaxException;
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
    @SubCommand("power")
    public PowerCommands power;
    @SubCommand("condition")
    public ConditionCommands condition;
    @SubCommand("marker")
    public MarkerCommands marker;
    @SubCommand("trigger")
    public MarkerCommands trigger;
    @SubCommand("modifier")
    public ModifierCommands modifier;
    @SubCommand("gen-wiki")
    public WikiCommand wiki;
    @SubCommand("template")
    public TemplateCommands templateCommand;
    @SubCommand("meta")
    public MetaCommands metaCommands;

    AdminCommands(RPGItems plugin, I18n i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
        Arrays.stream(getClass().getDeclaredMethods()).forEach(method -> {
            if (method.getAnnotation(SubCommand.class) != null && method.getAnnotation(Completion.class) != null) {
                subCommandCompletion.put(method.getAnnotation(SubCommand.class).value(), method.getAnnotation(Completion.class).value());
            }
        });
    }

    public static List<String> filtered(Arguments arguments, List<String> completeStr) {
        String[] rawArgs = arguments.getRawArgs();
        return completeStr.stream().filter(s -> s.toLowerCase().startsWith(rawArgs[rawArgs.length - 1].toLowerCase())).collect(Collectors.toList());
    }

    public static Pair<Integer, Integer> getPaging(int size, int perPage, Arguments args) {
        int max = (int) Math.ceil(size / (double) perPage);
        int page = args.top() == null ? 1 : args.nextInt();
        if (!(0 < page && page <= max)) {
            throw new BadCommandException("message.num_out_of_range", page, 0, max);
        }
        return Pair.of(max, page);
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

    public static Component getAuthorComponent(OfflinePlayer authorPlayer, String authorName) {
        if (authorName == null) {
            authorName = authorPlayer.getUniqueId().toString();
        }
        Component authorComponent = Component.text(authorName);
        authorComponent = authorComponent.hoverEvent(authorPlayer.getPlayer());
        return authorComponent;
    }

    public static boolean testExpr(String expr,CommandSender sender) {
        Player player;
        if(!(sender instanceof Player)){
            player = null;
        }
        else{
            player = (Player) sender;
        }
        expr=expr.replaceAll("damager:","");
        if(PlaceholderAPISupport.hasSupport()){
            expr = PlaceholderAPI.setPlaceholders(player,expr);
        }
        try {
            Expression ex = new Expression(expr);
            ex
                    .and("armour",BigDecimal.valueOf(0))
                    .and("damage", BigDecimal.valueOf(100))
                    .and("finalDamage", Utils.lazyNumber(() -> 100d))
                    .and("isDamageByEntity", BigDecimal.ONE)
                    .and("playerYaw", Utils.lazyNumber(() -> 0d))
                    .and("playerPitch", Utils.lazyNumber(() -> 0d))
                    .and("playerX", Utils.lazyNumber(() -> 0d))
                    .and("playerY", Utils.lazyNumber(() -> 0d))
                    .and("playerZ", Utils.lazyNumber(() -> 0d))
                    .and("playerLastDamage", Utils.lazyNumber(() -> 0d))
                    .and("cause", "LAVA");
            ex.addLazyFunction(Utils.now());
            ex
                    .and("damagerType", "zombie")
                    .and("isDamageByProjectile", BigDecimal.ONE)
                    .and("damagerTicksLived", Utils.lazyNumber(() -> 0d))
                    .and("distance", Utils.lazyNumber(() -> 0d))
                    .and("entityType", "zombie")
                    .and("entityYaw", Utils.lazyNumber(() -> 0d))
                    .and("entityPitch", Utils.lazyNumber(() -> 0d))
                    .and("entityX", Utils.lazyNumber(() -> 0d))
                    .and("entityY", Utils.lazyNumber(() -> 0d))
                    .and("entityZ", Utils.lazyNumber(() -> 0d));
            BigDecimal result = ex.eval();
            return true;
        } catch (Exception e) {
            return false;
        }
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

    protected static String consume(Arguments arguments) {
        if (arguments.top() == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        while (true) {
            String next = arguments.next();
            sb.append(next);
            if (arguments.top() == null) {
                return sb.toString();
            }
            sb.append(" ");
        }
    }

    protected static String consumeString(Arguments arguments) {
        String str = consume(arguments);
        if (str == null) throw new CommandException("internal.error.no_more_string");
        return str;
    }
    @Completion("")
    public List<String> itemStackCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        if (arguments.remains() == 2) {
            completeStr.addAll(Arrays.stream(Material.values()).map(Material::name).toList());
        } else if (arguments.remains() == 1) {
            completeStr.addAll(ItemManager.itemNames());
        }
        return filtered(arguments, completeStr);
    }
    @Completion("")
    public List<String> itemCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(ItemManager.itemNames());
                break;
            case 2:
                String cmd = arguments.getRawArgs()[0];
                if (subCommandCompletion.containsKey(cmd)) {
                    String comp = subCommandCompletion.get(cmd);
                    completeStr.addAll(Arrays.asList(comp.split(":", 2)[1].split(",")));
                }
                break;
            default:
                String base = arguments.at(0);
                String operation = arguments.at(2);
                if(arguments.remains()==3&&base.equals("enchantment")&&operation.equals("add")){
                    completeStr.addAll(RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).stream().map(Enchantment->Enchantment.getKey().value()).toList());
                }
                if(arguments.remains()==4&&base.equals("enchantment")&&operation.equals("add")){
                    completeStr.add(String.valueOf(RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(NamespacedKey.minecraft(arguments.at(3))).getMaxLevel()));
                }
                if(arguments.remains()==3&&base.equals("enchantment")&&operation.equals("remove")){
                    completeStr.addAll(getItem(arguments.at(1), sender).getEnchantMap().keySet().stream().map(Enchantment->Enchantment.getKey().value()).toList());
                }
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

    @Override
    public String getHelpPrefix() {
        return "";
    }

    @SubCommand("debug")
    public void debug(CommandSender sender, Arguments args) {
        Player player = asPlayer(sender);
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage("empty");
            return;
        }
        if (!item.hasItemMeta()) {
            player.sendMessage("empty meta");
            return;
        }
        Bukkit.dispatchCommand(sender,"paper dumpitem");
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer tagContainer = Objects.requireNonNull(meta).getPersistentDataContainer();
        if (tagContainer.has(TAG_META, PersistentDataType.TAG_CONTAINER)) {
            int uid = getInt(getTag(tagContainer, TAG_META), TAG_ITEM_UID);
            player.sendMessage("rpgitem uid: " + uid);
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
        plugin.loadPowers();
        WGSupport.reload();
        plugin.managedPlugins.forEach(Bukkit.getPluginManager()::disablePlugin);
        plugin.managedPlugins.clear();
        plugin.loadExtensions();
        plugin.managedPlugins.forEach(Bukkit.getPluginManager()::enablePlugin);
        ItemManager.reload(plugin);
        sender.sendMessage(ChatColor.GREEN + "[RPGItems] Reloaded RPGItems.");
    }
    @SubCommand("loadfile")
    public void loadFile(CommandSender sender, Arguments args) {
        String path = args.nextString();
        File file = new File(path);
        if (!file.exists()) {
            file = new File(ItemManager.getItemsDir(), path);
            if (!file.exists()) {
                I18n.sendMessage(sender, "message.error.file_not_exists", path);
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
                I18n.sendMessage(sender, "message.error.reloading_locked");
                return;
            }
            FileLock fileLock = backup.getValue();
            ItemManager.remove(item, false);
            if (!file.exists() || file.isDirectory()) {
                ItemManager.removeLock(item);
                I18n.sendMessage(sender, "message.item.file_deleted");
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
                    I18n.sendMessage(sender, "message.item.no_backup", item.getName());
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
            I18n.sendMessage(sender, "message.item.recovering", edited.getPath());
            try (FileChannel backupChannel = fileLock.channel(); FileChannel fileChannel = new FileOutputStream(file).getChannel()) {
                fileChannel.transferFrom(backupChannel, 0, backupChannel.size());
            }
            ItemManager.load(file, sender);
        } catch (IOException e) {
            I18n.sendMessage(sender, "message.error.recovering", item.getName(), file.getPath(), e.getLocalizedMessage());
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
            I18n.sendMessage(sender, "message.item.unlocked", item.getFile().getPath(), backup.getPath());
        } else {
            I18n.sendMessage(sender, "message.item.backedup", item.getFile().getPath(), backup.getPath());
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
        I18n.sendMessage(sender, "message.item.cleanbackup");
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
            I18n.sendMessage(sender, "message.no_item");
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
                        .append(I18n.getInstance(sender).getFormatted("message.item.list", item.getName()), Collections.singletonMap("{item}", item.getComponent(sender)))
                        .send(sender)
        );
    }

    @SubCommand("worldguard")
    public void toggleWorldGuard(CommandSender sender, Arguments args) {
        if (!WGSupport.hasSupport()) {
            I18n.sendMessage(sender, "message.worldguard.error");
            return;
        }
        if (WGSupport.useWorldGuard) {
            I18n.sendMessage(sender, "message.worldguard.disable");
        } else {
            I18n.sendMessage(sender, "message.worldguard.enable");
        }
        WGSupport.useWorldGuard = !WGSupport.useWorldGuard;
        RPGItems.plugin.cfg.useWorldGuard = WGSupport.useWorldGuard;
        RPGItems.plugin.cfg.save();
    }

    @SubCommand("wgforcerefresh")
    public void toggleForceRefresh(CommandSender sender, Arguments args) {
        if (!WGSupport.hasSupport()) {
            I18n.sendMessage(sender, "message.worldguard.error");
            return;
        }
        if (WGSupport.forceRefresh) {
            I18n.sendMessage(sender, "message.wgforcerefresh.disable");
        } else {
            I18n.sendMessage(sender, "message.wgforcerefresh.enable");
        }
        WGSupport.forceRefresh = !WGSupport.forceRefresh;
        RPGItems.plugin.cfg.wgForceRefresh = WGSupport.forceRefresh;
        RPGItems.plugin.cfg.save();
    }

    @SubCommand(value = "wgignore", tabCompleter = "itemCompleter")
    public void itemToggleWorldGuard(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if (!WGSupport.hasSupport()) {
            I18n.sendMessage(sender, "message.worldguard.error");
            return;
        }
        item.setIgnoreWorldGuard(!item.isIgnoreWorldGuard());
        if (item.isIgnoreWorldGuard()) {
            I18n.sendMessage(sender, "message.worldguard.override.active");
        } else {
            I18n.sendMessage(sender, "message.worldguard.override.disabled");
        }
        ItemManager.save(item);
    }

    @SubCommand("create")
    public void createItem(CommandSender sender, Arguments args) {
        String itemName = args.nextString();
        RPGItem newItem = ItemManager.newItem(itemName.toLowerCase(), sender);
        if (newItem != null) {
            I18n.sendMessage(sender, "message.create.ok", itemName);
            ItemManager.save(newItem);
        } else {
            I18n.sendMessage(sender, "message.create.fail");
        }
    }
    @SubCommand(value = "gui", tabCompleter = "attrCompleter")
    @Completion(value = "command:page:,displayFilter:,loreFilter:,nameFilter:")
    public void itemsGUI(CommandSender sender, Arguments args) {
        if (!(sender instanceof Player player)) {
            return;
        }

        int page = 1;
        String displayFilter = null;
        String loreFilter = null;
        String nameFilter = null;

        int pageArg = args.argInt("p", args.argInt("page",-1));
        if (pageArg != -1) {
            page = pageArg;
        }

        displayFilter = args.argString("d", args.argString("displayFilter",null));
        loreFilter = args.argString("l", args.argString("loreFilter",null));
        nameFilter = args.argString("n", args.argString("nameFilter",null));

        InventoryUtils.openMenu(player, page, displayFilter, loreFilter, nameFilter);
    }
    @SubCommand("giveperms")
    public void givePerms(CommandSender sender, Arguments args) {
        RPGItems.plugin.cfg.givePerms = !RPGItems.plugin.cfg.givePerms;
        if (RPGItems.plugin.cfg.givePerms) {
            I18n.sendMessage(sender, "message.giveperms.required");
        } else {
            I18n.sendMessage(sender, "message.giveperms.canceled");
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
                I18n.sendMessage(sender, "message.error.permission", str);
                return;
            }
            if (args.length() == 2) {
                if (sender instanceof Player) {
                    item.give((Player) sender, 1, false);
                    I18n.sendMessage(sender, "message.give.ok", item.getDisplayName());
                    refreshPlayer((Player) sender);
                } else {
                    I18n.sendMessage(sender, "message.give.console");
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
                refreshPlayer(player);
                I18n.sendMessage(sender, "message.give.to", item.getDisplayName() + ChatColor.AQUA, player.getName());
                I18n.sendMessage(player, "message.give.ok", item.getDisplayName());
            }
        } else {
            Optional<ItemGroup> optGroup = ItemManager.getGroup(str);
            if (optGroup.isEmpty()) {
                throw new BadCommandException("message.error.item", str);
            }
            ItemGroup group = optGroup.get();
            if ((plugin.cfg.givePerms || !sender.hasPermission("rpgitem")) && (!plugin.cfg.givePerms || !sender.hasPermission("rpgitem.give.group." + group.getName()))) {
                I18n.sendMessage(sender, "message.error.permission", str);
                return;
            }
            if (sender instanceof Player) {
                Player player = args.nextPlayerOrSender();
                group.give(player, 1, true);
                refreshPlayer(player);
                I18n.sendMessage(sender, "message.give.ok", group.getName());
            } else {
                I18n.sendMessage(sender, "message.give.console");
            }
        }

    }

    private void refreshPlayer(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ItemStack item : player.getInventory()) {
                    Optional<RPGItem> rpgItem = ItemManager.toRPGItemByMeta(item);
                    rpgItem.ifPresent(r -> r.updateItem(item,player));
                }
            }
        }.runTaskLater(RPGItems.plugin, 1);
    }

    @SubCommand(value = "remove", tabCompleter = "itemCompleter")
    public void removeItem(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        ItemManager.remove(item, true);
        I18n.sendMessage(sender, "message.remove.ok", item.getName());
    }

    @SubCommand(value = "display", tabCompleter = "itemCompleter")
    public void itemDisplay(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        String value = consume(args);
        if (value != null) {
            item.setDisplayName(value);
            I18n.sendMessage(sender, "message.display.set", item.getName(), item.getDisplayName());
            ItemManager.refreshItem();
            ItemManager.save(item);
        } else {
            I18n.sendMessage(sender, "message.display.get", item.getName(), item.getDisplayName());
        }
    }

    @SubCommand(value = "customModel", tabCompleter = "itemCompleter")
    public void itemCustomModel(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(),sender);
        CustomModelData.Builder customData = CustomModelData.customModelData();

        String floatsArg = args.argString("floats", null);
        if (floatsArg != null) {
            String[] floatValues = floatsArg.split(";");
            for (String value : floatValues) {
                try {
                    customData.addFloat(Float.parseFloat(value));
                } catch (NumberFormatException e) {
                    I18n.sendMessage(sender, "message.custom_model_data.invalid_float", value);
                }
            }
        }

        String stringsArg = args.argString("strings", null);
        if (stringsArg != null) {
            String[] stringValues = stringsArg.split("(?<!\\\\);"); // 支持转义分号
            for (String value : stringValues) {
                customData.addString(value.replace("\\;", ";").replace("\"", "")); // 还原转义分号并去掉双引号
            }
        }

        String flagsArg = args.argString("flags", null);
        if (flagsArg != null) {
            String[] booleanValues = flagsArg.split(";");
            for (String value : booleanValues) {
                customData.addFlag(Boolean.parseBoolean(value));
            }
        }

        String colorsArg = args.argString("colors", null);
        if (colorsArg != null) {
            String[] colorValues = colorsArg.split(";");
            for (String value : colorValues) {
                String[] components = value.split(",");
                if (components.length == 3) {
                    try {
                        int r = Integer.parseInt(components[0]);
                        int g = Integer.parseInt(components[1]);
                        int b = Integer.parseInt(components[2]);
                        customData.addColor(Color.fromRGB(r, g, b));
                    } catch (NumberFormatException e) {
                        I18n.sendMessage(sender, "message.custom_model_data.invalid_color", value);
                    }
                } else {
                    I18n.sendMessage(sender, "message.custom_model_data.invalid_color", value);
                }
            }
        }

        item.setCustomModelData(customData);
        ItemManager.refreshItem();
        ItemManager.save(item);
        I18n.sendMessage(sender, "message.custom_model_data.set");
    }

    @SubCommand(value = "damage", tabCompleter = "itemCompleter")
    public void itemDamage(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        try {
            int damageMin = args.nextInt();
            int damageMax;
            if (damageMin > 32767) {
                I18n.sendMessage(sender, "message.error.damagetolarge");
                return;
            }
            try {
                damageMax = args.nextInt();
            } catch (BadCommandException e) {
                damageMax = damageMin;
            }
            item.setDamage(damageMin, damageMax);
            if (damageMin != damageMax) {
                I18n.sendMessage(sender, "message.damage.set.range", item.getName(), item.getDamageMin(), item.getDamageMax());
            } else {
                I18n.sendMessage(sender, "message.damage.set.value", item.getName(), item.getDamageMin());
            }
            ItemManager.refreshItem();
            ItemManager.save(item);
        } catch (BadCommandException e) {
            I18n.sendMessage(sender, "message.damage.get", item.getName(), item.getDamageMin(), item.getDamageMax());
        }
    }

    @SubCommand(value = "armour", tabCompleter = "itemCompleter")
    public void itemArmour(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        try {
            int armour = args.nextInt();
            item.setArmour(armour);
            I18n.sendMessage(sender, "message.armour.set", item.getName(), item.getArmour());
            ItemManager.refreshItem();
            ItemManager.save(item);
        } catch (BadCommandException e) {
            I18n.sendMessage(sender, "message.armour.get", item.getName(), item.getArmour());
        }
    }

    @SubCommand(value = "item", tabCompleter = "itemStackCompleter")
    public void itemItem(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if (args.length() == 2) {
            new Message("")
                    .append(I18n.getInstance(sender).getFormatted("message.item.get", item.getName(), item.getItem().name()), new ItemStack(item.getItem()))
                    .send(sender);
        } else if (args.length() >= 3) {
            String materialName = args.nextString();
            Material material = MaterialUtils.getMaterial(materialName, sender);
            if (material == null || !material.isItem()) {
                I18n.sendMessage(sender, "message.error.material", materialName);
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
                    .append(I18n.getInstance(sender).getFormatted("message.item.set", item.getName(), item.getItem().name(), item.getDataValue()), new ItemStack(item.getItem()))
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
    @Completion("item:clone,clear,add,remove")
    public void itemEnchant(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if (args.length() == 2) {
            if (item.getEnchantMap() != null) {
                I18n.sendMessage(sender, "message.enchantment.listing", item.getName());
                if (item.getEnchantMap().isEmpty()) {
                    I18n.sendMessage(sender, "message.enchantment.empty_ench");
                } else {
                    for (Enchantment ench : item.getEnchantMap().keySet()) {
                        I18n.sendMessage(sender, "message.enchantment.item",
                                ench.getKey().toString(), item.getEnchantMap().get(ench));
                    }
                }
            } else {
                I18n.sendMessage(sender, "message.enchantment.no_ench");
            }
            return;
        }
        String command = args.nextString();
        switch (command) {
            case "clone": {
                if (sender instanceof Player) {
                    ItemStack hand = ((Player) sender).getInventory().getItemInMainHand();
                    if (hand.getType() == Material.AIR) {
                        I18n.sendMessage(sender, "message.enchantment.fail");
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
                        I18n.sendMessage(sender, "message.enchantment.success");
                    }
                } else {
                    I18n.sendMessage(sender, "message.enchantment.fail");
                }
                break;
            }
            case "clear": {
                item.setEnchantMap(null);
                item.rebuild();
                ItemManager.refreshItem();
                ItemManager.save(item);
                I18n.sendMessage(sender, "message.enchantment.removed");
                break;
            }
            case "add":{
                if(args.length()<4){
                    I18n.sendMessage(sender, "internal.error.no_more_enum");
                    return;
                }
                String ench = args.nextString();
                int level = 1;
                Enchantment enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(Key.key(ench));
                if(enchantment==null){
                    I18n.sendMessage(sender, "message.enchantment.bad_ench",ench);
                    return;
                }
                if(args.length()>=5){
                    level = args.nextInt();
                }
                if(item.getEnchantMap()==null){
                    item.setEnchantMap(new HashMap<>());
                }
                item.getEnchantMap().put(enchantment, level);
                item.rebuild();
                ItemManager.refreshItem();
                ItemManager.save(item);
                I18n.sendMessage(sender, "message.enchantment.success");
                break;
            }
            case "remove":{
                if(args.length()<4){
                    I18n.sendMessage(sender, "internal.error.no_more_enum");
                    return;
                }
                String ench = args.nextString();
                if(item.getEnchantMap()==null){
                    item.setEnchantMap(new HashMap<>());
                }
                if(!item.getEnchantMap().containsKey(RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(Key.key(ench)))){
                    I18n.sendMessage(sender, "message.enchantment.no_such_ench");
                    return;
                }
                item.getEnchantMap().remove(RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(Key.key(ench)));
                item.rebuild();
                ItemManager.refreshItem();
                ItemManager.save(item);
                I18n.sendMessage(sender, "message.enchantment.removed");
                break;
            }
            default:
                throw new BadCommandException("message.error.invalid_option", command, "enchantment", "clone,clear,add,remove");
        }
    }
    @SubCommand(value = "updatemode", tabCompleter = "itemCompleter")
    @Completion("item:FULL_UPDATE,DISPLAY_ONLY,LORE_ONLY,ENCHANT_ONLY,NO_DISPLAY,NO_LORE,NO_ENCHANT,NO_UPDATE")
    public void setUpdateMode(CommandSender sender, Arguments arguments) {
        RPGItem item = getItem(arguments.nextString(), sender);
        String mode = arguments.top();
        try{
            UpdateMode updateMode = UpdateMode.valueOf(mode.toUpperCase());
            item.setUpdateMode(updateMode);
            new Message("").append(I18n.getInstance(sender).getFormatted("message.updatemode.set", "FULL_UPDATE"), item.toItemStack())
                    .send(sender);
        }catch(IllegalArgumentException e){
            throw new BadCommandException("internal.error.bad_enum","Update Mode","FULL_UPDATE,DISPLAY_ONLY,LORE_ONLY,ENCHANT_ONLY,NO_DISPLAY,NO_LORE,NO_ENCHANT,NO_UPDATE");
        }
        ItemManager.save(item);
    }

    @SubCommand(value = "attributemode", tabCompleter = "itemCompleter")
    @Completion("item:FULL_UPDATE,PARTIAL_UPDATE")
    public void setAttributeMode(CommandSender sender, Arguments arguments) {
        RPGItem item = getItem(arguments.nextString(), sender);
        switch (arguments.top()) {
            case "FULL_UPDATE":
                item.setAttributeMode(FULL_UPDATE);
                new Message("").append(I18n.getInstance(sender).getFormatted("message.attributemode.set", "FULL_UPDATE"), item.toItemStack())
                        .send(sender);
                break;
            case "PARTIAL_UPDATE":
                item.setAttributeMode(PARTIAL_UPDATE);
                new Message("").append(I18n.getInstance(sender).getFormatted("message.attributemode.set", "PARTIAL_UPDATE"), item.toItemStack())
                        .send(sender);
                break;
            default:
                throw new BadCommandException("internal.error.bad_enum","Attribute Mode","FULL_UPDATE,PARTIAL_UPDATE");
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
                String line = consumeString(args);
                item.addDescription(ChatColor.WHITE + line);
                I18n.sendMessage(sender, "message.description.ok");
                ItemManager.refreshItem();
                ItemManager.save(item);
            }
            break;
            case "insert": {
                int lineNo = args.nextInt();
                String line = consumeString(args);
                int Length = item.getDescription().size();
                if (lineNo < 0 || lineNo >= Length) {
                    I18n.sendMessage(sender, "message.num_out_of_range", lineNo, 0, item.getDescription().size());
                    return;
                }
                item.getDescription().add(lineNo, HexColorUtils.hexColored(ChatColor.WHITE + line));
                item.rebuild();
                ItemManager.refreshItem();
                I18n.sendMessage(sender, "message.description.ok");
                ItemManager.save(item);
            }
            break;
            case "set": {
                int lineNo = args.nextInt();
                String line = consumeString(args);
                if (lineNo < 0 || lineNo >= item.getDescription().size()) {
                    I18n.sendMessage(sender, "message.num_out_of_range", lineNo, 0, item.getDescription().size());
                    return;
                }
                item.getDescription().set(lineNo, HexColorUtils.hexColored(ChatColor.WHITE + line));
                item.rebuild();
                ItemManager.refreshItem();
                I18n.sendMessage(sender, "message.description.change");
                ItemManager.save(item);
            }
            break;
            case "remove": {
                int lineNo = args.nextInt();
                if (lineNo < 0 || lineNo >= item.getDescription().size()) {
                    I18n.sendMessage(sender, "message.num_out_of_range", lineNo, 0, item.getDescription().size());
                    break;
                }
                item.getDescription().remove(lineNo);
                item.rebuild();
                ItemManager.refreshItem();
                I18n.sendMessage(sender, "message.description.remove");
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
                case "breaking" -> I18n.sendMessage(sender, "message.cost.get", item.getBlockBreakingCost());
                case "hitting" -> I18n.sendMessage(sender, "message.cost.get", item.getHittingCost());
                case "hit" -> I18n.sendMessage(sender, "message.cost.get", item.getHitCost());
                case "toggle" -> {
                    item.setHitCostByDamage(!item.isHitCostByDamage());
                    ItemManager.save(item);
                    if (item.isHitCostByDamage()) {
                        I18n.sendMessage(sender, "message.cost.hit_toggle.enable");
                    } else {
                        I18n.sendMessage(sender, "message.cost.hit_toggle.disable");
                    }
                }
                default -> throw new BadCommandException("message.error.invalid_option", type, "cost", "breaking,hitting,hit,toggle");
            }
        } else {
            int newValue = args.nextInt();
            switch (type) {
                case "breaking" -> item.setBlockBreakingCost(newValue);
                case "hitting" -> item.setHittingCost(newValue);
                case "hit" -> item.setHitCost(newValue);
                default -> throw new BadCommandException("message.error.invalid_option", type, "cost", "breaking,hitting,hit");
            }

            ItemManager.save(item);
            I18n.sendMessage(sender, "message.cost.change");
        }
    }

    @SubCommand(value = "durability", tabCompleter = "itemCompleter")
    @Completion("item:infinite,default,bound,togglebar,barformat")
    public void itemDurability(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if (args.length() == 2) {
            I18n.sendMessage(sender, "message.durability.info", item.getMaxDurability(), item.getDefaultDurability(), item.getDurabilityLowerBound(), item.getDurabilityUpperBound());
            return;
        }
        String arg = args.nextString();
        try {
            int durability = Integer.parseInt(arg);
            item.setMaxDurability(durability);
            ItemManager.refreshItem();
            ItemManager.save(item);
            I18n.sendMessage(sender, "message.durability.max_and_default", String.valueOf(durability));
        } catch (NumberFormatException e) {
            switch (arg) {
                case "infinite" -> {
                    item.setMaxDurability(-1);
                    ItemManager.refreshItem();
                    ItemManager.save(item);
                    I18n.sendMessage(sender, "message.durability.max_and_default", "infinite");
                }
                case "default" -> {
                    int durability = args.nextInt();
                    if (durability <= 0) {
                        // Actually we don't check max here
                        throw new CommandException("message.num_out_of_range", durability, 0, item.getMaxDurability());
                    }
                    item.setDefaultDurability(durability);
                    ItemManager.refreshItem();
                    ItemManager.save(item);
                    I18n.sendMessage(sender, "message.durability.default", String.valueOf(durability));
                }
                case "bound" -> {
                    int min = args.nextInt();
                    int max = args.nextInt();
                    item.setDurabilityBound(min, max);
                    ItemManager.refreshItem();
                    ItemManager.save(item);
                    I18n.sendMessage(sender, "message.durability.bound", String.valueOf(min), String.valueOf(max));
                }
                case "togglebar" -> toggleBar(sender, args, item);
                case "barformat" -> toggleBarFormat(sender, args, item);
                default -> throw new BadCommandException("message.error.invalid_option", arg, "durability", "value,infinite,togglebar,default,bound");
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
        I18n.sendMessage(sender, "message.permission.success");
    }

    @SubCommand(value = "togglepowerlore", tabCompleter = "itemCompleter")
    public void togglePowerLore(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        item.setShowPowerText(!item.isShowPowerText());
        item.rebuild();
        ItemManager.refreshItem();
        ItemManager.save(item);
        if (item.isShowPowerText()) {
            I18n.sendMessage(sender, "message.toggleLore.show");
        } else {
            I18n.sendMessage(sender, "message.toggleLore.hide");
        }
    }

    @SubCommand(value = "togglearmorlore", tabCompleter = "itemCompleter")
    public void toggleArmorLore(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        item.setShowArmourLore(!item.isShowArmourLore());
        item.rebuild();
        ItemManager.refreshItem();
        ItemManager.save(item);
        if (item.isShowArmourLore()) {
            I18n.sendMessage(sender, "message.toggleLore.show");
        } else {
            I18n.sendMessage(sender, "message.toggleLore.hide");
        }
    }

    @SubCommand(value = "additemflag", tabCompleter = "itemCompleter")
    @Completion("item:HIDE_ADDITIONAL_TOOLTIP,HIDE_ARMOR_TRIM,HIDE_ATTRIBUTES,HIDE_DESTROYS,HIDE_DYE,HIDE_ENCHANTS,HIDE_PLACED_ON,HIDE_STORED_ENCHANTS,HIDE_UNBREAKABLE")
    public void addItemFlag(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        ItemFlag flag = args.nextEnum(ItemFlag.class);
        item.getItemFlags().add(ItemFlag.valueOf(flag.name()));
        item.rebuild();
        ItemManager.refreshItem();
        ItemManager.save(item);
        I18n.sendMessage(sender, "message.itemflag.add", flag.name());
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
            I18n.sendMessage(sender, "message.itemflag.remove", flag.name());
        } else {
            I18n.sendMessage(sender, "message.itemflag.notfound", flag.name());
        }
    }
    @SubCommand(value = "canUse", tabCompleter = "itemCompleter")
    @Completion("command:true,false")
    public void canUse(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if(args.remains()>0) {
            item.setCanUse(args.nextBoolean());
        }
        item.rebuild();
        ItemManager.refreshItem();
        ItemManager.save(item);
    }
    @SubCommand(value = "canPlace", tabCompleter = "itemCompleter")
    @Completion("command:true,false")
    public void canPlace(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if(args.remains()>0) {
            item.setCanPlace(args.nextBoolean());
        }
        item.rebuild();
        ItemManager.refreshItem();
        ItemManager.save(item);
    }
    @SubCommand(value = "itemmodel", tabCompleter = "itemCompleter")
    public void setItemModel(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if(args.remains()==0){
            item.setItemModel(null);
            I18n.sendMessage(sender, "message.itemmodel.unset");
        }else{
            NamespacedKey namespacedKey = NamespacedKey.fromString(args.nextString());
            if(namespacedKey==null){
                I18n.sendMessage(sender, "message.itemmodel.invalid");
            }else{
                item.setItemModel(namespacedKey);
                I18n.sendMessage(sender, "message.itemmodel.set", namespacedKey.asString());
            }
        }
        item.rebuild();
        ItemManager.refreshItem();
        ItemManager.save(item);
    }

    //    @SubCommand(value = "togglebar", tabCompleter = "itemCompleter")
    public void toggleBar(CommandSender sender, Arguments args, RPGItem item) {
        item.toggleBar();
        ItemManager.refreshItem();
        ItemManager.save(item);
        I18n.sendMessage(sender, "message.durability.toggle");
    }

    //    @SubCommand(value = "barformat", tabCompleter = "itemCompleter")
    @Completion("item:DEFAULT,NUMERIC,NUMERIC_MINUS_ONE,NUMERIC_HEX,NUMERIC_HEX_MINUS_ONE,DEFAULT8")
    public void toggleBarFormat(CommandSender sender, Arguments args, RPGItem item) {
        item.setBarFormat(args.nextEnum(BarFormat.class));
        item.rebuild();
        ItemManager.refreshItem();
        ItemManager.save(item);
        switch (item.getBarFormat()) {
            case DEFAULT -> I18n.sendMessage(sender, "message.barformat.default");
            case NUMERIC -> I18n.sendMessage(sender, "message.barformat.numeric");
            case NUMERIC_BIN -> I18n.sendMessage(sender, "message.barformat.numeric_bin");
            case NUMERIC_HEX -> I18n.sendMessage(sender, "message.barformat.numeric_hex");
            case NUMERIC_MINUS_ONE -> I18n.sendMessage(sender, "message.barformat.numeric_minus_one");
            case NUMERIC_BIN_MINUS_ONE -> I18n.sendMessage(sender, "message.barformat.numeric_bin_minus_one");
            case NUMERIC_HEX_MINUS_ONE -> I18n.sendMessage(sender, "message.barformat.numeric_hex_minus_one");
            default -> plugin.getLogger().warning("missing barformat " + item.getBarFormat().name());
        }//todo I18N
    }

    @SubCommand("version")
    public void printVersion(CommandSender sender, Arguments args) {
        I18n.sendMessage(sender, "message.version", RPGItems.plugin.getDescription().getVersion());
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
        switch (item.getEnchantMode()) {
            case ALLOW -> I18n.sendMessage(sender, "message.enchantmode.allow", item.getName());
            case PERMISSION -> I18n.sendMessage(sender, "message.enchantmode.permission", item.getName());
            case DISALLOW -> I18n.sendMessage(sender, "message.enchantmode.disallow", item.getName());
            default -> plugin.getLogger().warning("missing enchantmode " + item.getEnchantMode().name());
        }//todo I18N
    }

    @SubCommand(value = "damagemode", tabCompleter = "itemCompleter")
    @Completion("item:FIXED,FIXED_WITHOUT_EFFECT,FIXED_RESPECT_VANILLA,FIXED_WITHOUT_EFFECT_RESPECT_VANILLA,VANILLA,ADDITIONAL,ADDITIONAL_RESPECT_VANILLA,MULTIPLY")
    public void toggleItemDamageMode(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        if (args.top() != null) {
            item.setDamageMode(args.nextEnum(RPGItem.DamageMode.class));
            item.rebuild();
            ItemManager.refreshItem();
            ItemManager.save(item);
        }
        switch (item.getDamageMode()) {
            case FIXED -> I18n.sendMessage(sender, "message.damagemode.fixed", item.getName());
            case FIXED_WITHOUT_EFFECT -> I18n.sendMessage(sender, "message.damagemode.fixed_without_effect", item.getName());
            case FIXED_RESPECT_VANILLA -> I18n.sendMessage(sender, "message.damagemode.fixed_respect_vanilla", item.getName());
            case FIXED_WITHOUT_EFFECT_RESPECT_VANILLA -> I18n.sendMessage(sender, "message.damagemode.fixed_without_effect_respect_vanilla", item.getName());
            case VANILLA -> I18n.sendMessage(sender, "message.damagemode.vanilla", item.getName());
            case MULTIPLY -> I18n.sendMessage(sender, "message.damagemode.multiply", item.getName());
            case ADDITIONAL -> I18n.sendMessage(sender, "message.damagemode.additional", item.getName());
            case ADDITIONAL_RESPECT_VANILLA -> I18n.sendMessage(sender, "message.damagemode.additional_respect_vanilla", item.getName());
            default -> plugin.getLogger().warning("missing damagemode " + item.getDamageMode().name());
        }//todo I18N
    }

    @SubCommand(value = "clone", tabCompleter = "itemCompleter")
    public void cloneItem(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender, true);
        String name = args.nextString();
        RPGItem i = ItemManager.cloneItem(item, name);
        if (i != null) {
            ItemManager.save(i);
            I18n.sendMessage(sender, "message.cloneitem.success", item.getName(), i.getName());
        } else {
            I18n.sendMessage(sender, "message.cloneitem.fail", item.getName(), name);
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
                I18n.sendMessage(sender, "message.import.not_supported", location.name());
        }
    }

    @SubCommand(value = "export", tabCompleter = "itemCompleter")
    @Completion("item:GIST")
    public void publish(CommandSender sender, Arguments args) {
        String itemsStr = args.nextString();
        NetworkUtils.Location location = args.top() == null ? GIST : args.nextEnum(NetworkUtils.Location.class);
        Set<String> items = Stream.of(itemsStr.split(",")).collect(Collectors.toSet());

        switch (location) {
            case GIST -> publishGist(sender, args, items);
            case URL -> throw new UnsupportedOperationException();
            default -> I18n.sendMessage(sender, "message.export.not_supported", location.name());
        }
    }

    @SubCommand(value = "author", tabCompleter = "itemCompleter")
    public void setAuthor(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        String author = args.next();
        if (author != null) {
            Component authorComponent = Component.text(author);
            String authorName = author.startsWith("@") ? author.substring(1) : author;
            Optional<OfflinePlayer> maybeAuthor = Optional.ofNullable(OfflinePlayerUtils.lookupPlayer(authorName));
            if (maybeAuthor.isPresent()) {
                OfflinePlayer authorPlayer = maybeAuthor.get();
                author = authorPlayer.getUniqueId().toString();
                authorComponent = getAuthorComponent(authorPlayer, authorName);
            } else if (author.startsWith("@")) {
                I18n.sendMessage(sender, "message.error.player", author);
                return;
            }
            item.setAuthor(author);
            I18n.sendMessage(sender, "message.item.author.set", Collections.singletonMap("{author}", authorComponent), item.getName());
            ItemManager.save(item);
        } else {
            String authorText = item.getAuthor();
            if (Strings.isNullOrEmpty(authorText)) {
                I18n.sendMessage(sender, "message.item.author.na", item.getName());
                return;
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                Component authorComponent = Component.text(authorText);
                try {
                    UUID uuid = UUID.fromString(authorText);
                    OfflinePlayer authorPlayer = Bukkit.getOfflinePlayer(uuid);
                    String authorName = authorPlayer.getName() == null ? OfflinePlayerUtils.lookupPlayerNameByUuidOnline(uuid).get(2, TimeUnit.SECONDS) : authorPlayer.getName();
                    authorComponent = getAuthorComponent(authorPlayer, authorName);
                } catch (IllegalArgumentException | InterruptedException | ExecutionException | TimeoutException ignored) {
                }
                I18n.sendMessage(sender, "message.item.author.get", Collections.singletonMap("{author}", authorComponent), item.getName());
            });
        }
    }

    @SubCommand(value = "note", tabCompleter = "itemCompleter")
    public void setNote(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        String note = args.next();
        if (note != null) {
            item.setNote(note);
            I18n.sendMessage(sender, "message.item.note.set", item.getName(), note);
            ItemManager.save(item);
        } else {
            I18n.sendMessage(sender, "message.item.note.get", item.getName(), item.getNote());
        }
    }

    @SubCommand(value = "license", tabCompleter = "itemCompleter")
    public void setLicense(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        String license = args.next();
        if (license != null) {
            item.setLicense(license);
            I18n.sendMessage(sender, "message.item.license.set", item.getName(), license);
            ItemManager.save(item);
        } else {
            I18n.sendMessage(sender, "message.item.license.get", item.getName(), item.getLicense());
        }
    }

    @SubCommand(value = "dump", tabCompleter = "itemCompleter")
    public void dumpItem(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender, true);
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        item.save(yamlConfiguration);
        String s = yamlConfiguration.saveToString();
        I18n.sendMessage(sender, "message.item.dump", item.getName(), s.replace(ChatColor.COLOR_CHAR + "", "\\u00A7"));
    }

    @SubCommand("creategroup")
    public void createGroup(CommandSender sender, Arguments args) {
        String groupName = args.nextString();
        ItemGroup group = null;
        if (args.top() == null || !args.top().contains("/")) {
            group = ItemManager.newGroup(groupName, sender);
            if (group == null) {
                I18n.sendMessage(sender, "message.create.fail");
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
                I18n.sendMessage(sender, "message.error.invalid_regex");
                return;
            } else {
                regex = regex.substring(1, regex.length() - 1);
            }
            group = ItemManager.newGroup(groupName, regex, sender);
            if (group == null) {
                I18n.sendMessage(sender, "message.create.fail");
                return;
            }
            ItemManager.save(group);
        }
        Set<RPGItem> items = group.getItems();
        I18n.sendMessage(sender, "message.group.header", group.getName(), items.size());
        for (RPGItem item : items) {
            new Message("")
                    .append(I18n.getInstance(sender).getFormatted("message.item.list", item.getName()), Collections.singletonMap("{item}", item.getComponent(sender)))
                    .send(sender);
        }
    }

    @SubCommand(value = "addtogroup", tabCompleter = "itemCompleter")
    public void addToGroup(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender, true);
        String groupName = args.nextString();
        Optional<ItemGroup> optGroup = ItemManager.getGroup(groupName);
        if (!optGroup.isPresent()) {
            I18n.sendMessage(sender, "message.error.item", groupName);
            return;
        }
        ItemGroup group = optGroup.get();
        group.addItem(item);
        I18n.sendMessage(sender, "message.group.header", group.getName(), group.getItemUids().size());
        ItemManager.save(group);
    }

    @SubCommand(value = "removefromgroup", tabCompleter = "itemCompleter")
    public void removeFromGroup(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender, true);
        String groupName = args.nextString();
        Optional<ItemGroup> optGroup = ItemManager.getGroup(groupName);
        if (!optGroup.isPresent()) {
            I18n.sendMessage(sender, "message.error.item", groupName);
            return;
        }
        ItemGroup group = optGroup.get();
        group.removeItem(item);
        I18n.sendMessage(sender, "message.group.header", group.getName(), group.getItemUids().size());
        ItemManager.save(group);
    }

    @SubCommand("listgroup")
    public void listGroup(CommandSender sender, Arguments args) {
        String groupName = args.nextString();
        Optional<ItemGroup> optGroup = ItemManager.getGroup(groupName);
        if (!optGroup.isPresent()) {
            I18n.sendMessage(sender, "message.error.item", groupName);
            return;
        }
        ItemGroup group = optGroup.get();
        Set<RPGItem> items = group.getItems();
        I18n.sendMessage(sender, "message.group.header", group.getName(), items.size());
        if (!Strings.isNullOrEmpty(group.getNote())) {
            I18n.sendMessage(sender, "message.group.note", group.getNote());
        }
        for (RPGItem item : items) {
            new Message("")
                    .append(I18n.getInstance(sender).getFormatted("message.item.list", item.getName()), Collections.singletonMap("{item}", item.getComponent(sender)))
                    .send(sender);
        }
    }

    @SubCommand("removegroup")
    public void removeGroup(CommandSender sender, Arguments args) {
        String groupName = args.nextString();
        Optional<ItemGroup> optGroup = ItemManager.getGroup(groupName);
        if (!optGroup.isPresent()) {
            I18n.sendMessage(sender, "message.error.item", groupName);
            return;
        }
        ItemGroup group = optGroup.get();
        ItemManager.remove(group, true);
        I18n.sendMessage(sender, "message.group.removed", group.getName());
    }

    @SubCommand(value = "damageType", tabCompleter = "damageTypeCompleter")
    public void damageType(CommandSender sender, Arguments args) {
        String item = args.nextString();

        RPGItem rpgItem = ItemManager.getItem(item).orElse(null);
        if (rpgItem == null) {
            I18n.sendMessage(sender, "message.error.item", item);
            return;
        }

        String damageType = args.top();
        if (damageType == null) {
            I18n.sendMessage(sender, "message.damagetype.set", rpgItem.getDamageType());
        }
        rpgItem.setDamageType(damageType);
        ItemManager.save(rpgItem);
        rpgItem.rebuild();
        I18n.sendMessage(sender, "message.damagetype.set", damageType);
    }

    public List<String> damageTypeCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(ItemManager.itemNames());
                break;
            case 2:
                completeStr.add("melee");
                completeStr.add("ranged");
                completeStr.add("magic");
                completeStr.add("summon");
                break;
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "armorExpression", tabCompleter = "itemCompleter")
    public void armorExpression(CommandSender sender, Arguments args) {
        String item = args.nextString();

        RPGItem rpgItem = ItemManager.getItem(item).orElse(null);
        if (rpgItem == null) {
            I18n.sendMessage(sender, "message.error.item", item);
            return;
        }

        String expr = args.top();
        if (expr == null) {
            I18n.sendMessage(sender, "message.armor_expression.set", rpgItem.getDamageType());
        }
        if (testExpr(expr,sender)) {
            rpgItem.setArmourExpression(expr);
            ItemManager.save(rpgItem);
            rpgItem.rebuild();
            I18n.sendMessage(sender, "message.armor_expression.set", expr);
        } else {
            I18n.sendMessage(sender, "message.error.invalid_expression", expr);
        }
    }

    @SubCommand(value = "playerArmorExpression", tabCompleter = "itemCompleter")
    public void playerArmorExpression(CommandSender sender, Arguments args) {
        String item = args.nextString();

        RPGItem rpgItem = ItemManager.getItem(item).orElse(null);
        if (rpgItem == null) {
            I18n.sendMessage(sender, "message.error.item", item);
            return;
        }

        String expr = args.top();
        if (expr == null) {
            I18n.sendMessage(sender, "message.armor_expression.set", rpgItem.getDamageType());
        }
        if (testExpr(expr,sender)) {
            rpgItem.setPlayerArmourExpression(expr);
            ItemManager.save(rpgItem);
            rpgItem.rebuild();
            I18n.sendMessage(sender, "message.armor_expression.set", expr);
        } else {
            I18n.sendMessage(sender, "message.error.invalid_expression", expr);
        }
    }

    public List<String> damageExpressionCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(ItemManager.itemNames());
                break;
            case 2:
                completeStr.add("melee");
                completeStr.add("ranged");
                completeStr.add("magic");
                completeStr.add("summon");
                break;
        }
        return filtered(arguments, completeStr);
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
                Bukkit.getScheduler().runTask(plugin, () -> I18n.sendMessage(sender, "message.export.gist.ed", id));
            } catch (InterruptedException | ExecutionException | URISyntaxException | IOException e) {
                plugin.getLogger().log(Level.WARNING, "Error exporting gist", e);
                Bukkit.getScheduler().runTask(plugin, () -> I18n.sendMessage(sender, "message.export.gist.failed"));
            } catch (TimeoutException e) {
                plugin.getLogger().log(Level.WARNING, "Timeout exporting gist", e);
                Bukkit.getScheduler().runTask(plugin, () -> I18n.sendMessage(sender, "message.export.gist.timeout"));
            } catch (BadCommandException e) {
                sender.sendMessage(e.getLocalizedMessage());
            }
        });
    }

    private void downloadGist(CommandSender sender, Arguments args, String id) {
        new Message(I18n.getInstance(sender).getFormatted("message.import.gist.ing")).send(sender);
        String token = args.argString("token", plugin.cfg.githubToken);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, String> gist;
            try {
                gist = NetworkUtils.downloadGist(id, token);
                Bukkit.getScheduler().runTask(plugin, () -> loadItems(sender, gist, args));
            } catch (InterruptedException | ExecutionException | URISyntaxException | IOException e) {
                plugin.getLogger().log(Level.WARNING, "Error importing gist", e);
                Bukkit.getScheduler().runTask(plugin, () -> new Message(I18n.getInstance(sender).getFormatted("message.import.gist.failed")).send(sender));
            } catch (TimeoutException e) {
                plugin.getLogger().log(Level.WARNING, "Timeout importing gist", e);
                Bukkit.getScheduler().runTask(plugin, () -> new Message(I18n.getInstance(sender).getFormatted("message.import.gist.timeout")).send(sender));
            } catch (BadCommandException e) {
                sender.sendMessage(e.getLocalizedMessage());
            }
        });
    }

    private void downloadUrl(CommandSender sender, Arguments args, String url) {
        new Message(I18n.getInstance(sender).getFormatted("message.import.url.ing")).send(sender);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<String, String> itemConf = NetworkUtils.downloadUrl(url);
                Bukkit.getScheduler().runTask(plugin, () -> loadItems(sender, itemConf, args));
            } catch (InterruptedException | ExecutionException | URISyntaxException | IOException e) {
                plugin.getLogger().log(Level.WARNING, "Error importing url", e);
                Bukkit.getScheduler().runTask(plugin, () -> new Message(I18n.getInstance(sender).getFormatted("message.import.url.failed")).send(sender));
            } catch (TimeoutException e) {
                plugin.getLogger().log(Level.WARNING, "Timeout importing url", e);
                Bukkit.getScheduler().runTask(plugin, () -> new Message(I18n.getInstance(sender).getFormatted("message.import.url.timeout")).send(sender));
            } catch (BadCommandException e) {
                sender.sendMessage(e.getLocalizedMessage());
            }
            throw new UnsupportedOperationException(url);
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
                        I18n.sendMessage(sender, "message.import.conflict_uid", origin, currentItem.get().getName(), uid);
                    } else {
                        Optional<ItemGroup> currentGroup = ItemManager.getGroup(uid);
                        I18n.sendMessage(sender, "message.import.conflict_uid", origin, currentGroup.get().getName(), uid);
                    }
                    return;
                }
                if (ItemManager.hasName(name)) {
                    I18n.sendMessage(sender, "message.import.conflict_name", name);
                    return;
                }

                RPGItem item = new RPGItem(itemStorage, name, uid);
                items.add(item);
            } catch (InvalidConfigurationException e) {
                plugin.getLogger().log(Level.WARNING, "Trying to load invalid config in " + k, e);
                I18n.sendMessage(sender, "message.import.invalid_conf", k);
                return;
            } catch (UnknownPowerException e) {
                I18n.sendMessage(sender, "message.power.unknown", e.getKey().toString());
                return;
            } catch (UnknownExtensionException e) {
                I18n.sendMessage(sender, "message.error.unknown.extension", e.getName());
                return;
            }
        }
        for (RPGItem item : items) {
            ItemManager.addItem(item);
            I18n.sendMessage(sender, "message.import.success", item.getName(), item.getUid());
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
            return I18n.getInstance(RPGItems.plugin.cfg.language).getFormatted(msg_internal, objs);
        }
    }
}
