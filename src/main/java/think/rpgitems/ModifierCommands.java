package think.rpgitems;

import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.BadCommandException;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;
import think.rpgitems.power.propertymodifier.Modifier;
import think.rpgitems.utils.ItemTagUtils;
import think.rpgitems.utils.ItemTagUtils.SubItemTagContainer;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static think.rpgitems.AdminCommands.*;
import static think.rpgitems.item.RPGItem.TAG_MODIFIER;
import static think.rpgitems.item.RPGItem.TAG_VERSION;
import static think.rpgitems.utils.ItemTagUtils.*;

public class ModifierCommands extends RPGCommandReceiver {
    private final RPGItems plugin;

    public ModifierCommands(RPGItems plugin, I18n i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }

    private static Pair<NamespacedKey, Class<? extends Modifier>> getModifierClass(CommandSender sender, String modifierStr) {
        try {
            NamespacedKey key = PowerManager.parseKey(modifierStr);
            Class<? extends Modifier> cls = PowerManager.getModifier(key);
            if (cls == null) {
                msgs(sender, "message.modifier.unknown", modifierStr);
            }
            return Pair.of(key, cls);
        } catch (UnknownExtensionException e) {
            msgs(sender, "message.error.unknown.extension", e.getName());
            return null;
        }
    }

    @Completion("")
    public List<String> addCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(Bukkit.getOnlinePlayers().stream().map(HumanEntity::getName).collect(Collectors.toList()));
                break;
            case 2:
                completeStr.addAll(PowerManager.getModifiers().keySet().stream().map(s -> PowerManager.hasExtension() ? s : s.getKey()).map(Object::toString).collect(Collectors.toList()));
                break;
            default:
                arguments.next();
                String mod = arguments.next();
                NamespacedKey namespacedKey = PowerManager.parseKey(mod);
                Class<? extends Modifier> modifier = PowerManager.getModifier(namespacedKey);
                return resolveProperties(sender, null, modifier, namespacedKey, arguments.getRawArgs()[arguments.getRawArgs().length - 1], arguments, false);
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "add", tabCompleter = "addCompleter")
    public void add(CommandSender sender, Arguments args) {
        String baseStr = args.top();
        if (baseStr == null || baseStr.equals("help") || args.remains() < 2) {
            msgs(sender, "manual.modifier.add.description");
            msgs(sender, "manual.modifier.add.usage");
            return;
        }
        PersistentDataContainer container = getRootContainer(sender, args, baseStr);
        String modifierStr = args.nextString();

        Pair<NamespacedKey, Class<? extends Modifier>> keyClass = getModifierClass(sender, modifierStr);
        if (keyClass == null || keyClass.getValue() == null) return;
        Class<? extends Modifier> cls = keyClass.getValue();
        try {
            Modifier modifier = initPropertyHolder(sender, args, null, cls);
            SubItemTagContainer modifierContainer = ItemTagUtils.makeTag(container, TAG_MODIFIER);
            set(modifierContainer, TAG_VERSION, UUID.randomUUID());
            NamespacedKey seq = nextAvailable(modifierContainer);
            SubItemTagContainer modifierTag = ItemTagUtils.makeTag(modifierContainer, seq);
            modifier.save(modifierTag);
            modifierTag.commit();
            msg(sender, "message.modifier.ok", modifierStr);
        } catch (Exception e) {
            if (e instanceof BadCommandException) {
                throw (BadCommandException) e;
            }
            plugin.getLogger().log(Level.WARNING, "Error adding modifier " + modifierStr + " to " + baseStr + " ", e);
            msgs(sender, "internal.error.command_exception");
        }
    }

    private NamespacedKey nextAvailable(PersistentDataContainer modifierContainer) {
        int i = 0;
        for (NamespacedKey key = PowerManager.parseKey(String.valueOf(i)); modifierContainer.has(key, PersistentDataType.TAG_CONTAINER); key = PowerManager.parseKey(String.valueOf(i))) {
            ++i;
        }
        return PowerManager.parseKey(String.valueOf(i));
    }

    private PersistentDataContainer getRootContainer(CommandSender sender, Arguments arguments, String baseStr) {
        PersistentDataContainer container;
        if (baseStr.toLowerCase(Locale.ROOT).equals("hand") && sender instanceof Player) {
            arguments.next();
            ItemStack item = ((Player) sender).getInventory().getItemInMainHand();
            ItemMeta meta = item.getItemMeta();
            container = meta.getPersistentDataContainer();
        } else {
            Player player = arguments.nextPlayer();
            container = player.getPersistentDataContainer();
        }
        return container;
    }

    @Completion("")
    public List<String> propCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.add("HAND");
                completeStr.addAll(Bukkit.getOnlinePlayers().stream().map(HumanEntity::getName).collect(Collectors.toList()));
                break;
            case 2: {
                String baseStr = arguments.top();
                PersistentDataContainer container = getRootContainer(sender, arguments, baseStr);
                SubItemTagContainer modifierContainer = ItemTagUtils.makeTag(container, TAG_MODIFIER);
                List<Modifier> modifiers = RPGItem.getModifiers(modifierContainer);
                completeStr.addAll(modifiers.stream().map(Modifier::id).collect(Collectors.toList()));
                break;
            }
            default: {
                String baseStr = arguments.top();
                PersistentDataContainer container = getRootContainer(sender, arguments, baseStr);
                Pair<Integer, Modifier> nextModifier = nextModifier(container, sender, arguments);
                Modifier modifier = nextModifier.getValue();
                return resolveProperties(sender, null, modifier.getClass(), modifier.getNamespacedKey(), arguments.getRawArgs()[arguments.getRawArgs().length - 1], arguments, false);
            }
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "prop", tabCompleter = "propCompleter")
    public void prop(CommandSender sender, Arguments args) throws IllegalAccessException {
        String baseStr = args.top();
        PersistentDataContainer container = getRootContainer(sender, args, baseStr);
        try {
            Pair<Integer, Modifier> modifierPair = nextModifier(container, sender, args);
            Modifier modifier = modifierPair.getValue();
            if (args.top() == null) {
                showModifier(sender, modifier);
                return;
            }
            setPropertyHolder(sender, args, modifier.getClass(), modifier, false);
            SubItemTagContainer modifierContainer = ItemTagUtils.makeTag(container, TAG_MODIFIER);
            set(modifierContainer, TAG_VERSION, UUID.randomUUID());
            NamespacedKey namespacedKey = PowerManager.parseKey(String.valueOf(modifierPair.getKey()));
            modifierContainer.remove(namespacedKey);
            SubItemTagContainer m = ItemTagUtils.makeTag(modifierContainer, namespacedKey);
            modifier.save(m);
            m.commit();
            msgs(sender, "message.marker.change");
        } catch (UnknownExtensionException e) {
            msgs(sender, "message.error.unknown.extension", e.getName());
        }
    }

    public void showModifier(CommandSender sender, Modifier modifier) {
        msgs(sender, "message.modifier.show", modifier.getLocalizedName(sender), modifier.getNamespacedKey().toString(), modifier.id());
        NamespacedKey modifierKey = modifier.getNamespacedKey();
        PowerManager.getProperties(modifierKey).forEach(
                (name, prop) -> showProp(sender, modifierKey, prop.getValue(), modifier)
        );
    }

    public Pair<Integer, Modifier> nextModifier(PersistentDataContainer container, CommandSender sender, Arguments args) {
        SubItemTagContainer modifierContainer = ItemTagUtils.makeTag(container, TAG_MODIFIER);
        List<Modifier> modifiers = RPGItem.getModifiers(modifierContainer);
        String next = args.nextString();
        OptionalInt index = IntStream.range(0, modifiers.size()).filter(i -> modifiers.get(i).id().equals((next))).findFirst();
        if (!index.isPresent()) {
            throw new BadCommandException("message.modifier.unknown", next);
        }
        return Pair.of(index.getAsInt(), modifiers.get(index.getAsInt()));
    }

    @SubCommand(value = "remove", tabCompleter = "propCompleter")
    public void remove(CommandSender sender, Arguments args) {
        String baseStr = args.top();
        PersistentDataContainer container = getRootContainer(sender, args, baseStr);
        try {
            Pair<Integer, Modifier> modifierPair = nextModifier(container, sender, args);
            SubItemTagContainer modifierContainer = makeTag(container, TAG_MODIFIER);
            set(modifierContainer, TAG_VERSION, UUID.randomUUID());
            NamespacedKey currentKey = PowerManager.parseKey(String.valueOf(modifierPair.getKey()));
            int i = 0;
            for (NamespacedKey key = PowerManager.parseKey(String.valueOf(i)); modifierContainer.has(key, PersistentDataType.TAG_CONTAINER); key = PowerManager.parseKey(String.valueOf(i))) {
                ++i;
            }
            --i;
            modifierContainer.remove(currentKey);
            NamespacedKey lastKey = PowerManager.parseKey(String.valueOf(i));
            PersistentDataContainer lastContainer = getTag(modifierContainer, lastKey);
            if (lastContainer != null){
                set(modifierContainer, currentKey, lastContainer);
            }
            modifierContainer.remove(lastKey);
            modifierContainer.commit();
            msgs(sender, "message.modifier.remove");
        } catch (UnknownExtensionException e) {
            msgs(sender, "message.error.unknown.extension", e.getName());
        }
    }

    @SubCommand("list")
    public void list(CommandSender sender, Arguments args) {
        int perPage = RPGItems.plugin.cfg.powerPerPage;
        String nameSearch = args.argString("n", args.argString("name", ""));
        List<NamespacedKey> modifiers = PowerManager.getModifiers()
                                                    .keySet()
                                                    .stream()
                                                    .filter(i -> i.getKey().contains(nameSearch))
                                                    .sorted(Comparator.comparing(NamespacedKey::getKey))
                                                    .collect(Collectors.toList());
        if (modifiers.size() == 0) {
            msgs(sender, "message.modifier.not_found", nameSearch);
            return;
        }
        Stream<NamespacedKey> stream = modifiers.stream();
        Pair<Integer, Integer> maxPage = getPaging(modifiers.size(), perPage, args);
        int page = maxPage.getValue();
        int max = maxPage.getKey();
        stream = stream
                         .skip((page - 1) * perPage)
                         .limit(perPage);
        sender.sendMessage(ChatColor.AQUA + "Modifiers: " + page + " / " + max);

        stream.forEach(
                modifier -> {
                    msgs(sender, "message.modifier.key", modifier.toString());
                    msgs(sender, "message.modifier.description", PowerManager.getDescription(modifier, null));
                    PowerManager.getProperties(modifier).forEach(
                            (name, mp) -> showProp(sender, modifier, mp.getValue(), null)
                    );
                    msgs(sender, "message.line_separator");
                });
        sender.sendMessage(ChatColor.AQUA + "Modifiers: " + page + " / " + max);
    }
}
