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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static think.rpgitems.AdminCommands.*;

public class MarkerCommands extends RPGCommandReceiver {
    private final RPGItems plugin;

    public MarkerCommands(RPGItems plugin, I18n i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    private static Pair<NamespacedKey, Class<? extends Marker>> getMarkerClass(CommandSender sender, String markerStr) {
        try {
            NamespacedKey key = PowerManager.parseKey(markerStr);
            Class<? extends Marker> cls = PowerManager.getMarker(key);
            if (cls == null) {
                I18n.sendMessage(sender, "message.marker.unknown", markerStr);
            }
            return Pair.of(key, cls);
        } catch (UnknownExtensionException e) {
            I18n.sendMessage(sender, "message.error.unknown.extension", e.getName());
            return null;
        }
    }

    private static Marker nextMarker(RPGItem item, CommandSender sender, Arguments args) {
        String next = args.top();
        if (next.contains("-")) {
            next = args.nextString();
            String p1 = next.split("-", 2)[0];
            String p2 = next.split("-", 2)[1];
            try {
                int nth = Integer.parseInt(p1);
                if (nth < 0 || nth >= item.getMarkers().size()) {
                    throw new BadCommandException("message.num_out_of_range", nth, 0, item.getMarkers().size()-1);
                }
                Marker marker = item.getMarkers().get(nth);
                Pair<NamespacedKey, Class<? extends Marker>> keyClass = getMarkerClass(sender, p2);
                if (keyClass == null || !marker.getNamespacedKey().equals(keyClass.getKey())) {
                    throw new BadCommandException("message.marker.unknown", p2);
                }
                return marker;
            } catch (NumberFormatException ignore) {
                Pair<NamespacedKey, Class<? extends Marker>> keyClass = getMarkerClass(sender, p1);
                if (keyClass == null) {
                    throw new BadCommandException("message.marker.unknown", p1);
                }
                try {
                    int nth = Integer.parseInt(p2);
                    List<Marker> markers = item.getMarkers();
                    if (nth < 0 || nth >= markers.size()) {
                        throw new BadCommandException("message.num_out_of_range", nth, 0, markers.size()-1);
                    }
                    return item.getMarker(keyClass.getKey(), keyClass.getValue()).get(nth);
                } catch (NumberFormatException ignored) {
                    throw new BadCommandException("message.marker.unknown", p2);
                }
            }
        } else {
            int nth = args.nextInt();
            if (nth < 0 || nth >= item.getMarkers().size()) {
                throw new BadCommandException("message.num_out_of_range", nth, 0, item.getMarkers().size()-1);
            }
            return item.getMarkers().get(nth);
        }
    }


    @Override
    public String getHelpPrefix() {
        return "marker";
    }

    @Completion("")
    public List<String> addCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(ItemManager.itemNames());
                break;
            case 2:
                completeStr.addAll(PowerManager.getMarkers().keySet().stream().map(s -> PowerManager.hasExtension() ? s : s.getKey()).map(Object::toString).toList());
                break;
            default:
                RPGItem item = getItem(arguments.nextString(), sender);
                String last = arguments.getRawArgs()[arguments.getRawArgs().length - 1];
                String conditionKey = arguments.nextString();
                Pair<NamespacedKey, Class<? extends Marker>> keyClass = getMarkerClass(sender, conditionKey);
                if (keyClass != null) {
                    return resolveProperties(sender, item, keyClass.getValue(), keyClass.getKey(), last, arguments, true);
                }
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "add", tabCompleter = "addCompleter")
    public void add(CommandSender sender, Arguments args) {
        String itemStr = args.next();
        String markerStr = args.next();
        if (itemStr == null || itemStr.equals("help") || markerStr == null) {
            I18n.sendMessage(sender, "manual.marker.add.description");
            I18n.sendMessage(sender, "manual.marker.add.usage");
            return;
        }
        RPGItem item = getItem(itemStr, sender);
        Pair<NamespacedKey, Class<? extends Marker>> keyClass = getMarkerClass(sender, markerStr);
        if (keyClass == null || keyClass.getValue() == null) return;
        Marker marker;
        Class<? extends Marker> cls = keyClass.getValue();
        NamespacedKey key = keyClass.getKey();
        try {
            marker = initPropertyHolder(sender, args, item, cls);
            item.addMarker(key, marker);
            ItemManager.refreshItem();
            ItemManager.save(item);
            msg(sender, "message.marker.ok", markerStr);
        } catch (Exception e) {
            if (e instanceof BadCommandException) {
                throw (BadCommandException) e;
            }
            plugin.getLogger().log(Level.WARNING, "Error adding marker " + markerStr + " to item " + itemStr + " " + item, e);
            I18n.sendMessage(sender, "internal.error.command_exception");
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
                completeStr.addAll(IntStream.range(0, item.getMarkers().size()).mapToObj(i -> i + "-" + item.getMarkers().get(i).getNamespacedKey()).toList());
                break;
            default:
                item = getItem(arguments.nextString(), sender);
                Marker nextMarker = nextMarker(item, sender, arguments);
                return resolveProperties(sender, item, nextMarker.getClass(), nextMarker.getNamespacedKey(), arguments.getRawArgs()[arguments.getRawArgs().length - 1], arguments, false);
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "prop", tabCompleter = "propCompleter")
    public void prop(CommandSender sender, Arguments args) throws IllegalAccessException {
        RPGItem item = getItem(args.nextString(), sender);
        if (args.top() == null) {
            for (int i = 0; i < item.getMarkers().size(); i++) {
                Marker marker = item.getMarkers().get(i);
                showMarker(sender, item, marker);
            }
            return;
        }
        try {
            Marker marker = nextMarker(item, sender, args);
            if (args.top() == null) {
                showMarker(sender, item, marker);
                return;
            }
            setPropertyHolder(sender, args, marker.getClass(), marker, false);
            item.rebuild();
            ItemManager.refreshItem();
            ItemManager.save(item);
            I18n.sendMessage(sender, "message.marker.change");
        } catch (UnknownExtensionException e) {
            I18n.sendMessage(sender, "message.error.unknown.extension", e.getName());
        }
    }

    public void showMarker(CommandSender sender, RPGItem item, Marker marker) {
        I18n.sendMessage(sender, "message.marker.show", marker.getLocalizedName(sender), marker.getNamespacedKey().toString(), marker.displayText() == null ? I18n.getInstance(sender).getFormatted("message.marker.no_display") : marker.displayText());
        NamespacedKey markerKey = item.getPropertyHolderKey(marker);
        PowerManager.getProperties(markerKey).forEach(
                (name, prop) -> showProp(sender, markerKey, prop.getValue(), marker)
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
                completeStr.addAll(IntStream.range(0, item.getMarkers().size()).mapToObj(i -> i + "-" + item.getMarkers().get(i).getNamespacedKey()).toList());
                break;
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "remove", tabCompleter = "removeCompleter")
    public void remove(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        Marker marker = nextMarker(item, sender, args);
        try {
            int nth = -1;
            List<Marker> markers = item.getMarkers();
            for (int i = 0; i < markers.size(); i++) {
                Marker marker1 = markers.get(i);
                if (marker1.equals(marker)) {
                    nth = i;
                    break;
                }
            }
            if (nth < 0) {
                I18n.sendMessage(sender, "message.num_out_of_range", nth, 0, markers.size());
                return;
            }
            item.getMarkers().remove(nth);
            item.rebuild();
            ItemManager.save(item);
            I18n.sendMessage(sender, "message.marker.removed", String.valueOf(nth));
        } catch (UnknownExtensionException e) {
            I18n.sendMessage(sender, "message.error.unknown.extension", e.getName());
        }
    }

    @SubCommand("list")
    public void list(CommandSender sender, Arguments args) {
        int perPage = RPGItems.plugin.cfg.powerPerPage;
        String nameSearch = args.argString("n", args.argString("name", ""));
        List<NamespacedKey> markers = PowerManager.getMarkers()
                .keySet()
                .stream()
                .filter(i -> i.getKey().contains(nameSearch))
                .sorted(Comparator.comparing(NamespacedKey::getKey))
                .toList();
        if (markers.isEmpty()) {
            I18n.sendMessage(sender, "message.marker.not_found", nameSearch);
            return;
        }
        Stream<NamespacedKey> stream = markers.stream();
        Pair<Integer, Integer> maxPage = getPaging(markers.size(), perPage, args);
        int page = maxPage.getValue();
        int max = maxPage.getKey();
        stream = stream
                .skip((long) (page - 1) * perPage)
                .limit(perPage);
        sender.sendMessage(ChatColor.AQUA + "Markers: " + page + " / " + max);

        stream.forEach(
                marker -> {
                    I18n.sendMessage(sender, "message.marker.key", marker.toString());
                    I18n.sendMessage(sender, "message.marker.description", PowerManager.getDescription(marker, null));
                    PowerManager.getProperties(marker).forEach(
                            (name, mp) -> showProp(sender, marker, mp.getValue(), null)
                    );
                    I18n.sendMessage(sender, "message.line_separator");
                });
        sender.sendMessage(ChatColor.AQUA + "Markers: " + page + " / " + max);
    }
}
