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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static think.rpgitems.AdminCommands.*;

public class ConditionCommands extends RPGCommandReceiver {
    private final RPGItems plugin;

    public ConditionCommands(RPGItems plugin, I18n i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }

    private static Pair<NamespacedKey, Class<? extends Condition<?>>> getConditionClass(CommandSender sender, String conditionStr) {
        try {
            NamespacedKey key = PowerManager.parseKey(conditionStr);
            Class<? extends Condition<?>> cls = PowerManager.getCondition(key);
            if (cls == null) {
                msgs(sender, "message.condition.unknown", conditionStr);
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
                completeStr.addAll(PowerManager.getConditions().keySet().stream().map(s -> PowerManager.hasExtension() ? s : s.getKey()).map(Object::toString).collect(Collectors.toList()));
                break;
            default:
                RPGItem item = getItem(arguments.nextString(), sender);
                String last = arguments.getRawArgs()[arguments.getRawArgs().length - 1];
                String conditionKey = arguments.nextString();
                Pair<NamespacedKey, Class<? extends Condition<?>>> keyClass = getConditionClass(sender, conditionKey);
                if (keyClass != null) {
                    return resolveProperties(sender, item, keyClass.getValue(), keyClass.getKey(), last, arguments, true);
                }
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "add", tabCompleter = "addCompleter")
    public void add(CommandSender sender, Arguments args) {
        String itemStr = args.next();
        String conditionStr = args.next();
        if (itemStr == null || itemStr.equals("help") || conditionStr == null) {
            msgs(sender, "manual.condition.add.description");
            msgs(sender, "manual.condition.add.usage");
            return;
        }
        RPGItem item = getItem(itemStr, sender);
        Pair<NamespacedKey, Class<? extends Condition<?>>> keyClass = getConditionClass(sender, conditionStr);
        if (keyClass == null || keyClass.getValue() == null) return;
        Condition condition;
        Class<? extends Condition> cls = keyClass.getValue();
        NamespacedKey key = keyClass.getKey();
        try {
            condition = initPropertyHolder(sender, args, item, cls);
            item.addCondition(key, condition);
            ItemManager.refreshItem();
            ItemManager.save(item);
            msg(sender, "message.condition.ok", conditionStr, condition.getName());
        } catch (Exception e) {
            if (e instanceof BadCommandException) {
                throw (BadCommandException) e;
            }
            plugin.getLogger().log(Level.WARNING, "Error adding condition " + conditionStr + " to item " + itemStr + " " + item, e);
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
                completeStr.addAll(IntStream.range(0, item.getConditions().size()).mapToObj(i -> i + "-" + item.getConditions().get(i).getNamespacedKey()).collect(Collectors.toList()));
                break;
            default:
                item = getItem(arguments.nextString(), sender);
                Condition<?> nextCondition = nextCondition(item, sender, arguments);
                return resolveProperties(sender, item, nextCondition.getClass(), nextCondition.getNamespacedKey(), arguments.getRawArgs()[arguments.getRawArgs().length - 1], arguments, false);
        }
        return filtered(arguments, completeStr);
    }

    private Condition<?> nextCondition(RPGItem item, CommandSender sender, Arguments args) {
        String next = args.top();
        if (next.contains("-")) {
            next = args.nextString();
            String p1 = next.split("-", 2)[0];
            String p2 = next.split("-", 2)[1];
            try {
                int nth = Integer.parseInt(p1);
                Condition<?> condition = item.getConditions().get(nth);
                if (condition == null) {
                    throw new BadCommandException("message.condition.unknown", nth);
                }
                Pair<NamespacedKey, Class<? extends Condition<?>>> keyClass = getConditionClass(sender, p2);
                if (keyClass == null || !condition.getNamespacedKey().equals(keyClass.getKey())) {
                    throw new BadCommandException("message.condition.unknown", p2);
                }
                return condition;
            } catch (NumberFormatException ignore) {
                Pair<NamespacedKey, Class<? extends Condition<?>>> keyClass = getConditionClass(sender, p1);
                if (keyClass == null) {
                    throw new BadCommandException("message.condition.unknown", p1);
                }
                try {
                    int nth = Integer.parseInt(p2);
                    Condition<?> condition = item.getCondition(keyClass.getKey(), keyClass.getValue()).get(nth);
                    if (condition == null) {
                        throw new BadCommandException("message.condition.unknown", nth);
                    }
                    return condition;
                } catch (NumberFormatException ignored) {
                    throw new BadCommandException("message.condition.unknown", p2);
                }
            }
        } else {
            int nth = args.nextInt();
            Condition<?> condition = item.getConditions().get(nth);
            if (condition == null) {
                throw new BadCommandException("message.condition.unknown", nth);
            }
            return condition;
        }
    }

    @SubCommand(value = "prop", tabCompleter = "propCompleter")
    public void prop(CommandSender sender, Arguments args) throws IllegalAccessException {
        RPGItem item = getItem(args.nextString(), sender);
        if (args.top() == null) {
            for (int i = 0; i < item.getConditions().size(); i++) {
                Condition<?> condition = item.getConditions().get(i);
                showCondition(sender, item, condition);
            }
            return;
        }
        try {
            Condition<?> condition = nextCondition(item, sender, args);
            if (args.top() == null) {
                showCondition(sender, item, condition);
                return;
            }
            setPropertyHolder(sender, args, condition.getClass(), condition, false);
            item.rebuild();
            ItemManager.refreshItem();
            ItemManager.save(item);
            msgs(sender, "message.condition.change");
        } catch (UnknownExtensionException e) {
            msgs(sender, "message.error.unknown.extension", e.getName());
        }
    }

    public static void showCondition(CommandSender sender, RPGItem item, Condition condition) {
        msgs(sender, "message.condition.show", condition.id(), condition.getLocalizedName(sender), condition.getNamespacedKey().toString(), condition.displayText() == null ? I18n.getInstance(sender).format("message.power.no_display") : condition.displayText());
        PowerManager.getProperties(item.getPropertyHolderKey(condition)).forEach(
                (name, prop) -> showProp(sender, condition.getNamespacedKey(), prop.getValue(), condition)
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
                completeStr.addAll(IntStream.range(0, item.getConditions().size()).mapToObj(i -> i + "-" + item.getConditions().get(i).getNamespacedKey()).collect(Collectors.toList()));
                break;
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "remove", tabCompleter = "removeCompleter")
    public void remove(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        Condition<?> condition = nextCondition(item, sender, args);
        try {
            int nth = -1;
            List<Condition<?>> conditions = item.getConditions();
            for (int i = 0; i < conditions.size(); i++) {
                Condition<?> condition1 = conditions.get(i);
                if (condition1.equals(condition)){
                    nth = i;
                }
            }
            if (nth < 0 || nth > conditions.size()){
                msgs(sender, "message.num_out_of_range", nth, 0, conditions.size());
                return;
            }
            item.getConditions().remove(nth);
            msgs(sender, "message.condition.removed", String.valueOf(nth));
            item.rebuild();
            ItemManager.save(item);
        } catch (UnknownExtensionException e) {
            msgs(sender, "message.error.unknown.extension", e.getName());
        }
    }

    @SubCommand("list")
    public void list(CommandSender sender, Arguments args) {
        int perPage = RPGItems.plugin.cfg.powerPerPage;
        String nameSearch = args.argString("n", args.argString("name", ""));
        List<NamespacedKey> conditions = PowerManager.getConditions()
                                                     .keySet()
                                                     .stream()
                                                     .filter(i -> i.getKey().contains(nameSearch))
                                                     .sorted(Comparator.comparing(NamespacedKey::getKey))
                                                     .collect(Collectors.toList());
        if (conditions.size() == 0) {
            msgs(sender, "message.condition.not_found", nameSearch);
            return;
        }
        Stream<NamespacedKey> stream = conditions.stream();
        Pair<Integer, Integer> maxPage = getPaging(conditions.size(), perPage, args);
        int page = maxPage.getValue();
        int max = maxPage.getKey();
        stream = stream
                         .skip((page - 1) * perPage)
                         .limit(perPage);
        sender.sendMessage(ChatColor.AQUA + "Conditions: " + page + " / " + max);

        stream.forEach(
                condition -> {
                    msgs(sender, "message.condition.key", condition.toString());
                    msgs(sender, "message.condition.description", PowerManager.getDescription(condition, null));
                    PowerManager.getProperties(condition).forEach(
                            (name, mp) -> showProp(sender, condition, mp.getValue(), null)
                    );
                    msgs(sender, "message.line_separator");
                });
        sender.sendMessage(ChatColor.AQUA + "Conditions: " + page + " / " + max);
    }
}
