package think.rpgitems;

import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.BadCommandException;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.Trigger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static think.rpgitems.AdminCommands.*;

public class PowerCommands extends RPGCommandReceiver {
    private final RPGItems plugin;

    public PowerCommands(RPGItems plugin, I18n i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }

    private static Pair<NamespacedKey, Class<? extends Power>> getPowerClass(CommandSender sender, String powerStr) {
        try {
            NamespacedKey key = PowerManager.parseKey(powerStr);
            Class<? extends Power> cls = PowerManager.getPower(key);
            if (cls == null) {
                msgs(sender, "message.power.unknown", powerStr);
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
                completeStr.addAll(ItemManager.itemNames());
                break;
            case 2:
                completeStr.addAll(PowerManager.getPowers().keySet().stream().map(s -> PowerManager.hasExtension() ? s : s.getKey()).map(Object::toString).collect(Collectors.toList()));
                break;
            default:
                RPGItem item = getItem(arguments.nextString(), sender);
                String last = arguments.getRawArgs()[arguments.getRawArgs().length - 1];
                String powerKey = arguments.nextString();
                Pair<NamespacedKey, Class<? extends Power>> powerClass = getPowerClass(sender, powerKey);
                if (powerClass != null) {
                    return resolveProperties(sender, item, powerClass.getValue(), powerClass.getKey(), last, arguments, true);
                }
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "add", tabCompleter = "addCompleter")
    public void add(CommandSender sender, Arguments args) {
        String itemStr = args.next();
        String powerStr = args.next();
        if (itemStr == null || itemStr.equals("help") || powerStr == null) {
            msgs(sender, "manual.power.add.description");
            msgs(sender, "manual.power.add.usage");
            return;
        }
        RPGItem item = getItem(itemStr, sender);
        Pair<NamespacedKey, Class<? extends Power>> keyClass = getPowerClass(sender, powerStr);
        if (keyClass == null || keyClass.getValue() == null) return;
        Power power;
        Class<? extends Power> cls = keyClass.getValue();
        NamespacedKey key = keyClass.getKey();
        try {
            power = initPropertyHolder(sender, args, item, cls);
            item.addPower(key, power);
            ItemManager.refreshItem();
            ItemManager.save(item);
            msg(sender, "message.power.ok", powerStr, item.getPowers().size() - 1);
        } catch (Exception e) {
            if (e instanceof BadCommandException) {
                throw (BadCommandException) e;
            }
            plugin.getLogger().log(Level.WARNING, "Error adding power " + powerStr + " to item " + itemStr + " " + item, e);
            msgs(sender, "internal.error.command_exception");
        }
    }

    @Completion("")
    public List<String> propCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(ItemManager.itemNames());
                break;
            case 2:
                RPGItem item = getItem(arguments.nextString(), sender);
                completeStr.addAll(IntStream.range(0, item.getPowers().size()).mapToObj(i -> i + "-" + item.getPowers().get(i).getNamespacedKey()).collect(Collectors.toList()));
                break;
            default:
                item = getItem(arguments.nextString(), sender);
                Power nextPower = nextPower(item, sender, arguments);
                return resolveProperties(sender, item, nextPower.getClass(), nextPower.getNamespacedKey(), arguments.getRawArgs()[arguments.getRawArgs().length - 1], arguments, false);
        }
        return filtered(arguments, completeStr);
    }

    private static Power nextPower(RPGItem item, CommandSender sender, Arguments args) {
        String next = args.top();
        if (next.contains("-")) {
            next = args.nextString();
            String p1 = next.split("-", 2)[0];
            String p2 = next.split("-", 2)[1];
            try {
                int nth = Integer.parseInt(p1);
                Power power = item.getPowers().get(nth);
                if (power == null) {
                    throw new BadCommandException("message.power.unknown", nth);
                }
                Pair<NamespacedKey, Class<? extends Power>> keyClass = getPowerClass(sender, p2);
                if (keyClass == null || !power.getNamespacedKey().equals(keyClass.getKey())) {
                    throw new BadCommandException("message.power.unknown", p2);
                }
                return power;
            } catch (NumberFormatException ignore) {
                Pair<NamespacedKey, Class<? extends Power>> keyClass = getPowerClass(sender, p1);
                if (keyClass == null) {
                    throw new BadCommandException("message.power.unknown", p1);
                }
                try {
                    int nth = Integer.parseInt(p2);
                    Power power = item.getPower(keyClass.getKey(), keyClass.getValue()).get(nth);
                    if (power == null) {
                        throw new BadCommandException("message.power.unknown", nth);
                    }
                    return power;
                } catch (NumberFormatException ignored) {
                    throw new BadCommandException("message.power.unknown", p2);
                }
            }
        } else {
            int nth = args.nextInt();
            Power power = item.getPowers().get(nth);
            if (power == null) {
                throw new BadCommandException("message.power.unknown", nth);
            }
            return power;
        }
    }

    private static int nextNth(RPGItem item, CommandSender sender, Arguments args) {
        String next = args.top();
        int nth;
        if (next.contains("-")) {
            next = args.top();
            String p1 = next.split("-", 2)[0];
            String p2 = next.split("-", 2)[1];
            try {
                nth = Integer.parseInt(p1);
            } catch (NumberFormatException ignore) {
                Pair<NamespacedKey, Class<? extends Power>> keyClass = getPowerClass(sender, p1);
                if (keyClass == null) {
                    throw new BadCommandException("message.power.unknown", p1);
                }
                try {
                    nth = Integer.parseInt(p2);
                } catch (NumberFormatException ignored) {
                    throw new BadCommandException("message.power.unknown", p2);
                }
            }
        } else {
            nth = Integer.parseInt(args.top());
        }
        return nth;
    }

    @SubCommand(value = "prop", tabCompleter = "propCompleter")
    public void prop(CommandSender sender, Arguments args) throws IllegalAccessException {
        RPGItem item = getItem(args.nextString(), sender);
        if (args.top() == null) {
            for (int i = 0; i < item.getPowers().size(); i++) {
                Power power = item.getPowers().get(i);
                showPower(sender, i, item, power);
            }
            return;
        }
        try {
            Power power = nextPower(item, sender, args);
            if (args.top() == null) {
                showPower(sender, item.getPowers().indexOf(power), item, power);
                return;
            }
            setPropertyHolder(sender, args, power.getClass(), power, false);
            item.rebuild();
            ItemManager.refreshItem();
            ItemManager.save(item);
            msgs(sender, "message.power.change");
        } catch (UnknownExtensionException e) {
            msgs(sender, "message.error.unknown.extension", e.getName());
        }
    }

    public static void showPower(CommandSender sender, int nth, RPGItem item, Power power) {
        msgs(sender, "message.marker.show", nth, power.getLocalizedName(sender), power.getNamespacedKey().toString(), power.displayText() == null ? I18n.getInstance(sender).format("message.power.no_display") : power.displayText(), power.getTriggers().stream().map(Trigger::name).collect(Collectors.joining(",")));
        NamespacedKey powerKey = item.getPropertyHolderKey(power);
        PowerManager.getProperties(powerKey).forEach(
                (name, prop) -> showProp(sender, powerKey, prop.getValue(), power)
        );
    }

    @Completion("")
    public List<String> removeCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(ItemManager.itemNames());
                break;
            case 2:
                RPGItem item = getItem(arguments.nextString(), sender);
                completeStr.addAll(IntStream.range(0, item.getPowers().size()).mapToObj(i -> i + "-" + item.getPowers().get(i).getNamespacedKey()).collect(Collectors.toList()));
                break;
        }
        return filtered(arguments, completeStr);
    }

    @Completion("")
    public List<String> reorderCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(ItemManager.itemNames());
                break;
            case 2:
                RPGItem item = getItem(arguments.nextString(), sender);
                completeStr.addAll(IntStream.range(0, item.getPowers().size()).mapToObj(i -> i + "-" + item.getPowers().get(i).getNamespacedKey()).collect(Collectors.toList()));
                break;
            case 3:
                RPGItem item1 = getItem(arguments.nextString(), sender);
                int i1 = nextNth(item1, sender, arguments);
                completeStr.addAll(IntStream.range(0, item1.getPowers().size()).filter(i -> i != i1).mapToObj(i -> i + "-" + item1.getPowers().get(i).getNamespacedKey()).collect(Collectors.toList()));
                break;
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "remove", tabCompleter = "removeCompleter")
    public void remove(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        int nth = -1;
        Power power = nextPower(item, sender, args);
        try {
            List<Power> powers = item.getPowers();
            for (int i = 0; i < powers.size(); i++) {
                Power pi = powers.get(i);
                if (power.equals(pi)) {
                    nth = i;
                    break;
                }
            }
            if (nth <= 0 || nth >= powers.size()) {
                msg(sender, "message.num_out_of_range", nth, 0, powers.size());
                return;
            }
            Power power1 = item.getPowers().get(nth);
            if (power == null) {
                msgs(sender, "message.power.unknown", nth);
                return;
            }
            power.deinit();
            item.getPowers().remove(nth);
            NamespacedKey key = item.removePropertyHolderKey(power);
            item.rebuild();
            ItemManager.save(item);
            msgs(sender, "message.power.removed", key.toString(), nth);
        } catch (UnknownExtensionException e) {
            msgs(sender, "message.error.unknown.extension", e.getName());
        }
    }

    @SubCommand(value = "reorder", tabCompleter = "reorderCompleter")
    public void reorder(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        int origin = -1;
        int next = -1;
        int size = item.getPowers().size();
        Power originPower = nextPower(item, sender, args);
        Power nextPower = nextPower(item, sender, args);
        List<Power> powers = item.getPowers();
        for (int i = 0; i < powers.size(); i++) {
            Power pi = powers.get(i);
            if (origin == -1 && originPower.equals(pi)) {
                origin = i;
                continue;
            }
            if (next == -1 && nextPower.equals(pi)){
                next = i;
            }
        }

        if (next < 0 || next >= size) {
            msg(sender, "message.num_out_of_range", next, 0, size);
            return;
        }
        if (origin < 0 || origin >= size) {
            msg(sender, "message.num_out_of_range", origin, 0, size);
            return;
        }
        Power remove = item.getPowers().remove(origin);
        item.getPowers().add(next, remove);
        ItemManager.refreshItem();
        ItemManager.save(item);
        msg(sender, "message.power.reorder", remove.getName(), next);
    }

    @SubCommand("list")
    public void list(CommandSender sender, Arguments args) {
        int perPage = RPGItems.plugin.cfg.powerPerPage;
        String nameSearch = args.argString("n", args.argString("name", ""));
        List<NamespacedKey> powers = PowerManager.getPowers()
                .keySet()
                .stream()
                .filter(i -> i.getKey().contains(nameSearch))
                .sorted(Comparator.comparing(NamespacedKey::getKey))
                .collect(Collectors.toList());
        if (powers.size() == 0) {
            msgs(sender, "message.power.not_found", nameSearch);
            return;
        }
        Stream<NamespacedKey> stream = powers.stream();
        Pair<Integer, Integer> maxPage = getPaging(powers.size(), perPage, args);
        int page = maxPage.getValue();
        int max = maxPage.getKey();
        stream = stream
                .skip((page - 1) * perPage)
                .limit(perPage);
        sender.sendMessage(ChatColor.AQUA + "Powers: " + page + " / " + max);

        stream.forEach(
                power -> {
                    msgs(sender, "message.power.key", power.toString());
                    msgs(sender, "message.power.description", PowerManager.getDescription(power, null));
                    PowerManager.getProperties(power).forEach(
                            (name, mp) -> showProp(sender, power, mp.getValue(), null)
                    );
                    msgs(sender, "message.line_separator");
                });
        sender.sendMessage(ChatColor.AQUA + "Powers: " + page + " / " + max);
    }
}
