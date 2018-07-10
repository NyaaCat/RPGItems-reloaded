package think.rpgitems.commands;

import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.LanguageRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import think.rpgitems.RPGItems;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;
import think.rpgitems.utils.Pair;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class RPGCommandReceiver extends CommandReceiver {
    private final Map<String, String> subCommandAttribute = new HashMap<>();

    public RPGCommandReceiver(RPGItems plugin, LanguageRepository i18n) {
        super(plugin, i18n);
        subCommands.forEach(
                (s, method) -> {
                    Attribute attr = method.getAnnotation(Attribute.class);
                    if (attr == null) return;
                    subCommandAttribute.put(s, attr.value());
                }
        );
    }

    private static List<String> resolvePropertyValueSuggestion(Class<? extends Power> power, String propertyName, boolean hasNamePrefix) {
        try {
            return resolvePropertyValueSuggestion(power.getField(propertyName), hasNamePrefix);
        } catch (NoSuchFieldException e) {
            return Collections.emptyList();
        }
    }

    private static List<String> resolvePropertyValueSuggestion(Field propertyField, boolean hasNamePrefix) {
        BooleanChoice bc = propertyField.getAnnotation(BooleanChoice.class);
        if (bc != null) {
            return Stream.of(bc.trueChoice(), bc.falseChoice()).map(s -> (hasNamePrefix ? propertyField.getName() + ":" : "") + s).collect(Collectors.toList());
        }
        AcceptedValue as = propertyField.getAnnotation(AcceptedValue.class);
        if (as != null) {
            return Arrays.stream(as.value()).map(s -> (hasNamePrefix ? propertyField.getName() + ":" : "") + s).collect(Collectors.toList());
        }
        if (propertyField.getType() == boolean.class) {
            return Stream.of(true, false).map(s -> (hasNamePrefix ? propertyField.getName() + ":" : "") + s).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<String> resolvePowerOrPropertySuggestion(CommandSender sender, String[] args) {
        if (args.length < 4) return null;
        String last = args[args.length - 1];
        String[] arg = Arrays.copyOf(args, args.length - 1);
        Arguments cmd = Arguments.parse(arg);
        Pair<RPGItem, String> itemCommand = resolveItemCommand(cmd.next(), cmd.next());
        if (itemCommand == null) return null;
        switch (itemCommand.getValue()) {
            case "get":
            case "set": {
                return resolveProperty(last, cmd, itemCommand);
            }
            case "power": {
                Class<? extends Power> power = Power.powers.get(cmd.next());
                if (power == null) return null;
                SortedMap<PowerProperty, Field> argMap = Power.propertyOrders.get(power);
                Set<Field> settled = new HashSet<>();
                Optional<PowerProperty> req = argMap.keySet()
                                                    .stream()
                                                    .filter(PowerProperty::required)
                                                    .reduce((first, second) -> second); //findLast

                List<Field> required = req.map(r -> argMap.entrySet()
                                                          .stream()
                                                          .filter(entry -> entry.getKey().order() <= r.order())
                                                          .map(Map.Entry::getValue)
                                                          .collect(Collectors.toList())).orElse(new ArrayList<>());

                for (Field field : argMap.values()) {
                    String name = field.getName();
                    String value = cmd.argString(name, null);
                    if (value != null) {
                        required.remove(field);
                        settled.add(field);
                    }
                }
                return resolvePowerSuggestions(last, cmd, power, argMap, settled, required);
            }
            default: return null;
        }
    }

    private List<String> resolvePowerSuggestions(String last, Arguments cmd, Class<? extends Power> power, SortedMap<PowerProperty, Field> argMap, Set<Field> settled, List<Field> required) {
        for (Field field : argMap.entrySet()
                                 .stream()
                                 .filter(p -> p.getKey().order() != Integer.MAX_VALUE)
                                 .map(Map.Entry::getValue)
                                 .collect(Collectors.toList())) {
            if (settled.contains(field)) continue;
            String value = cmd.next();
            if (value == null) {
                if (argMap.values().stream().anyMatch(f -> last.startsWith(f.getName() + ":"))) {//we are suggesting a value as we have the complete property name
                    String currentPropertyName = last.split(":")[0];
                    return resolvePropertyValueSuggestion(power, currentPropertyName, true).stream().filter(s -> s.startsWith(last)).collect(Collectors.toList());
                }
                List<String> suggestions;
                if (!required.isEmpty()) {
                    Field current = required.stream().findFirst().orElseThrow(RuntimeException::new);
                    if (last.isEmpty()) {
                        suggestions = resolvePropertyValueSuggestion(current, true);
                        if (!suggestions.isEmpty()) return suggestions; // current possible value
                        return required.stream().map(s -> s.getName() + ":").collect(Collectors.toList());
                    } else {
                        suggestions = required.stream().map(s -> s.getName() + ":").filter(s -> s.startsWith(last)).collect(Collectors.toList());
                        if (!suggestions.isEmpty()) return suggestions; //required property
                        suggestions = argMap.values().stream().filter(s -> !settled.contains(s)).map(s -> s.getName() + ":").filter(s -> s.startsWith(last)).collect(Collectors.toList());
                        if (!suggestions.isEmpty()) return suggestions; //unsettled property
                        return resolvePropertyValueSuggestion(current, false).stream().filter(s -> s.startsWith(last)).collect(Collectors.toList());
                    }
                } else {
                    suggestions = argMap.values().stream().filter(s -> !settled.contains(s)).map(s -> s.getName() + ":").collect(Collectors.toList());
                    if (!last.isEmpty()) {
                        suggestions = suggestions.stream().filter(s -> s.startsWith(last)).collect(Collectors.toList());
                    }
                    if (!suggestions.isEmpty()) return suggestions; //unsettled property
                }
            }
            required.remove(field);
            settled.add(field);
        }
        return null;
    }

    private List<String> resolveProperty(String last, Arguments cmd, Pair<RPGItem, String> itemCommand) {
        RPGItem item = itemCommand.getKey();
        String powerName = cmd.next();
        List<Power> powers = item.powers.stream().filter(p -> p.getName().equals(powerName)).collect(Collectors.toList());
        if (powers.isEmpty()) return null;
        Class<? extends Power> powerClass = powers.get(0).getClass();
        if (cmd.top() == null) {
            return IntStream.rangeClosed(1, powers.size()).mapToObj(Integer::toString).collect(Collectors.toList());
        } else {
            cmd.next();
        }
        List<String> suggestions;
        if (cmd.top() == null) {
            suggestions = Power.propertyOrders.get(powerClass).keySet().stream().map(PowerProperty::name).collect(Collectors.toList());
            if (last.isEmpty()) return suggestions;
            return suggestions.stream().filter(s -> s.startsWith(last)).collect(Collectors.toList());
        }
        if (itemCommand.getValue().equals("get")) return null;
        suggestions = resolvePropertyValueSuggestion(powerClass, cmd.next(), false);
        if (last.isEmpty()) return suggestions;
        return suggestions.stream().filter(s -> s.startsWith(last)).collect(Collectors.toList());
    }

    private static Pair<RPGItem, String> resolveItemCommand(String f, String s) {
        RPGItem rpgItem = ItemManager.getItemByName(f);
        if (rpgItem != null) {
            return new Pair<>(rpgItem, s);
        }
        rpgItem = ItemManager.getItemByName(s);
        if (rpgItem != null) {
            return new Pair<>(rpgItem, f);
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && ItemManager.getItemByName(args[0]) != null) {
            if (args.length > 1) {
                String cmd = args[1];
                args[1] = args[0];
                args[0] = cmd;
            } else {
                String name = args[0];
                args = new String[args.length + 1];
                args[1] = name;
                args[0] = "print";
            }
        }
        Arguments cmd = Arguments.parse(args);
        if (cmd == null) return false;
        acceptCommand(sender, cmd);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean suggestion = args[args.length - 1].isEmpty();
        CommandReceiver.Arguments cmd = CommandReceiver.Arguments.parse(args);
        if (cmd == null) return null;
        switch (cmd.length()) {
            case 0:
                return subCommandAttribute.entrySet().stream().filter(entry -> entry.getValue().startsWith("command")).map(Map.Entry::getKey).collect(Collectors.toList());
            case 1: {
                String str = cmd.next();
                if (suggestion) {
                    if (ItemManager.getItemByName(str) != null) {
                        // we have a `/rpgitem item` and waiting a proper command
                        return subCommandAttribute.entrySet().stream().filter(entry -> Stream.of("item", "power", "property").anyMatch(entry.getValue()::startsWith)).map(Map.Entry::getKey).collect(Collectors.toList());
                    } else {
                        // we have a `/rpgitem command` and waiting a argument
                        String attr = subCommandAttribute.get(str);
                        if (attr == null) return null;
                        if (attr.startsWith("command")) {
                            // it's a global command and we have suggestion in attr
                            String[] att = attr.split(":");
                            if (att.length > 1) {
                                return Arrays.asList(att[1].split(","));
                            }
                            return null;
                        } else {
                            // it's a item command, just items
                            return new ArrayList<>(ItemManager.itemByName.keySet());
                        }
                    }
                } else {
                    // trying to complete a `/rpgitem com` or `/rpgitem ite`
                    List<String> list = subCommands.keySet().stream().filter(s -> s.startsWith(str)).collect(Collectors.toList());
                    if (!list.isEmpty()) return list;
                    return ItemManager.itemByName.keySet().stream().filter(s -> s.startsWith(str)).collect(Collectors.toList());
                }
            }
            case 2: {
                String first = cmd.next();
                String second = cmd.next();
                if (suggestion) {
                    // may be `/rpgitem item command` or `/rpgitem command item`
                    Pair<RPGItem, String> itemCommand = resolveItemCommand(first, second);
                    if (itemCommand == null) return null; // neither
                    String attr = subCommandAttribute.get(itemCommand.getValue());
                    if (attr == null) return null;
                    String[] att = attr.split(":");
                    switch (att[0]) {
                        case "property":
                        case "power":
                            // four case below
                            switch (itemCommand.getValue()) {
                                case "power":
                                    return new ArrayList<>(Power.powers.keySet()); // all powers
                                case "set":
                                case "get":
                                case "removepower":
                                    return itemCommand.getKey().powers.stream().map(Power::getName).collect(Collectors.toList()); // current powers
                                default:
                                    return null;
                            }
                        case "item":
                        case "command": {
                            return att.length > 1 ? Arrays.asList(att[1].split(",")) : null; // suggestion bundled in attr
                        }
                        default: return null;
                    }
                } else {
                    if (ItemManager.getItemByName(first) != null) {
                        // trying to complete `/rpgitem item com`
                        return subCommandAttribute.entrySet().stream()
                                                  .filter(entry -> Stream.of("item", "power", "property").anyMatch(entry.getValue()::startsWith))
                                                  .map(Map.Entry::getKey)
                                                  .filter(s -> s.startsWith(second))
                                                  .collect(Collectors.toList());
                    } else {
                        // trying to complete `/rpgitem commmand argu`
                        String attr = subCommandAttribute.get(first);
                        if (attr == null) return null;
                        String[] att = attr.split(":");
                        switch (att[0]) {
                            case "property":
                            case "power":
                            case "item": {
                                return ItemManager.itemByName.keySet().stream().filter(s -> s.startsWith(second)).collect(Collectors.toList()); // items
                            }
                            case "command": {
                                return att.length > 1 ? Arrays.stream(att[1].split(",")).filter(s -> s.startsWith(second)).collect(Collectors.toList()) : null; // bundled in attr
                            }
                            default: return null;
                        }
                    }
                }
            }
            case 3: {
                String first = cmd.next();
                String second = cmd.next();
                String third = cmd.next();
                Pair<RPGItem, String> itemCommand = resolveItemCommand(first, second);
                if (itemCommand == null) return null;
                String attr = subCommandAttribute.get(itemCommand.getValue());
                if (attr == null) return null;
                String[] att = attr.split(":");
                if (suggestion) {
                    return resolvePowerOrPropertySuggestion(sender, args); // only case is `/rpgitem power item somepower`
                } else {
                    switch (att[0]) {
                        case "property":
                        case "power":
                            switch (itemCommand.getValue()) {
                                case "power":
                                    return Power.powers.keySet().stream().filter(s -> s.startsWith(third)).collect(Collectors.toList()); // only case is `/rpgitem power item somepow`
                                case "set":
                                case "get":
                                case "removepower":
                                    return itemCommand.getKey().powers.stream().map(Power::getName).filter(s -> s.startsWith(third)).collect(Collectors.toList()); // complete current powers
                                default:
                                    return null;
                            }
                        case "item": {
                            return att.length > 1 ? Arrays.stream(att[1].split(",")).filter(s -> s.startsWith(third)).collect(Collectors.toList()) : null; // bundled
                        }
                        default:
                            return null;
                    }
                }
            }
            default: {
                return resolvePowerOrPropertySuggestion(sender,args);
            }
        }
    }
}
