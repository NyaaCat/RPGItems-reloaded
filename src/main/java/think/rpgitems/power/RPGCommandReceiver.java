package think.rpgitems.power;

import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.LanguageRepository;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.Pair;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.RPGItems;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static think.rpgitems.power.PowerManager.powers;

public abstract class RPGCommandReceiver extends CommandReceiver {
    private final Map<String, String> subCommandAttribute = new HashMap<>();
    private final LanguageRepository i18n;

    public RPGCommandReceiver(RPGItems plugin, LanguageRepository i18n) {
        super(plugin, i18n);
        this.i18n = i18n;
        subCommands.forEach(
                (s, method) -> {
                    Attribute attr = method.getAnnotation(Attribute.class);
                    if (attr == null) return;
                    subCommandAttribute.put(s, attr.value());
                }
        );
    }

    private static List<String> resolvePropertyValueSuggestion(Class<? extends Power> power, String propertyName, String last, boolean hasNamePrefix) {
        try {
            return resolvePropertyValueSuggestion(power, power.getField(propertyName), last, hasNamePrefix);
        } catch (NoSuchFieldException e) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> resolvePropertyValueSuggestion(Class<? extends Power> power, Field propertyField, String last, boolean hasNamePrefix) {
        BooleanChoice bc = propertyField.getAnnotation(BooleanChoice.class);
        if (bc != null) {
            return Stream.of(bc.trueChoice(), bc.falseChoice()).map(s -> (hasNamePrefix ? propertyField.getName() + ":" : "") + s).filter(s -> s.startsWith(last)).collect(Collectors.toList());
        }
        if (Collection.class.isAssignableFrom(propertyField.getType())) {
            ParameterizedType listType = (ParameterizedType) propertyField.getGenericType();
            Class<?> listArg = (Class<?>) listType.getActualTypeArguments()[0];
            if (listArg.equals(Trigger.class)) {
                return resolveEnumListValue(power, propertyField, new ArrayList<>(Trigger.keySet()), last, hasNamePrefix);
            }
            if (!listArg.isEnum()) {
                return Collections.emptyList();
            }
            List<String> enumValues = Stream.of(((Class<? extends Enum>) listArg).getEnumConstants()).map(Enum::name).collect(Collectors.toList());
            return resolveEnumListValue(power, propertyField, enumValues, last, hasNamePrefix);
        }
        AcceptedValue as = propertyField.getAnnotation(AcceptedValue.class);
        if (as != null) {
            return PowerManager.getAcceptedValue(power, as).stream().map(s -> (hasNamePrefix ? propertyField.getName() + ":" : "") + s).filter(s -> s.startsWith(last)).collect(Collectors.toList());
        }
        if (propertyField.getType().equals(boolean.class) || propertyField.getType().equals(Boolean.class)) {
            return Stream.of(true, false).map(s -> (hasNamePrefix ? propertyField.getName() + ":" : "") + s).filter(s -> s.startsWith(last)).collect(Collectors.toList());
        }
        if (propertyField.getType().isEnum()) {
            return Stream.of(propertyField.getType().getEnumConstants()).map(s -> (hasNamePrefix ? propertyField.getName() + ":" : "") + s.toString()).filter(s -> s.startsWith(last)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static List<String> resolveEnumListValue(Class<? extends Power> power, Field propertyField, List<String> enumValues, String last, boolean hasNamePrefix) {
        String currentValuesStr;
        if (hasNamePrefix) {
            currentValuesStr = last.replace(propertyField.getName() + ":", "");
        } else {
            currentValuesStr = last;
        }
        List<String> currentVaules = Stream.of(currentValuesStr.split(",")).collect(Collectors.toList());
        int size = currentVaules.size();
        String lastVaule = size > 0 ? currentVaules.get(size - 1) : "";
        if (enumValues.contains(lastVaule)) {
            lastVaule = "";
        } else {
            currentVaules.remove(size - 1);
        }
        AcceptedValue as = propertyField.getAnnotation(AcceptedValue.class);
        if (as != null) {
            List<String> acceptedValue = PowerManager.getAcceptedValue(power, as);
            enumValues.retainAll(acceptedValue);
        }
        String incompleteValue = lastVaule;

        if (Set.class.isAssignableFrom(propertyField.getType()) || (as != null && as.preset() == Preset.TRIGGERS)) {
            enumValues.removeAll(currentVaules);
        }
        String base = incompleteValue.isEmpty() ? last : last.replaceAll(incompleteValue + "$", "");
        boolean next = (currentVaules.isEmpty() && !hasNamePrefix) || base.endsWith(":") || base.endsWith(",");
        return enumValues.stream().filter(n -> n.startsWith(incompleteValue)).map(n -> base + (next ? "" : ",") + n).collect(Collectors.toList());
    }

    private List<String> resolvePowerOrPropertySuggestion(CommandSender sender, String[] args) {
        if (args.length < 4) return Collections.emptyList();
        String last = args[args.length - 1];
        String[] arg = Arrays.copyOf(args, args.length - 1);
        Arguments cmd = Arguments.parse(arg);
        if (cmd == null) return Collections.emptyList();
        Pair<RPGItem, String> itemCommand = resolveItemCommand(cmd.next(), cmd.next());
        if (itemCommand == null) return Collections.emptyList();
        switch (itemCommand.getValue()) {
            case "get":
            case "set": {
                return resolveGetSet(last, cmd, itemCommand);
            }
            case "power": {
                return resolvePowerProperties(sender, last, cmd);
            }
            default:
                return Collections.emptyList();
        }
    }

    private List<String> resolvePowerProperties(CommandSender sender, String last, Arguments cmd) {
        @LangKey(skipCheck = true) String powName = cmd.next();
        NamespacedKey powerKey;
        try {
            powerKey = PowerManager.parseKey(powName);
        } catch (UnknownExtensionException e) {
            return Collections.emptyList();
        }
        Class<? extends Power> power = powers.get(powerKey);
        if (power == null) return Collections.emptyList();
        SortedMap<PowerProperty, Field> argMap = PowerManager.getProperties(power);
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

        PowerMeta powerMeta = power.getAnnotation(PowerMeta.class);

        for (Field field : argMap.values()) {
            String name = field.getName();
            String value = cmd.argString(name, null);
            if (value != null
                        || (powerMeta.immutableTrigger() && name.equals("triggers"))
                        || (powerMeta.marker() && name.equals("triggers"))
                        || (powerMeta.marker() && name.equals("conditions") && !powerMeta.withConditions())
                        || (!powerMeta.withSelectors() && name.equals("selectors"))
            ) {
                required.remove(field);
                settled.add(field);
            }
        }
        if (settled.isEmpty()) {
            actionBarTip(sender, powerKey, null);
        }
        return resolvePropertiesSuggestions(sender, last, power, argMap, settled, required);
    }

    private List<String> resolvePropertiesSuggestions(CommandSender sender, String last, Class<? extends Power> power, SortedMap<PowerProperty, Field> argMap, Set<Field> settled, List<Field> required) {
        if (argMap.values().stream().anyMatch(f -> last.startsWith(f.getName() + ":"))) {//we are suggesting a value as we have the complete property name
            String currentPropertyName = last.split(":")[0];
            actionBarTip(sender, powers.inverse().get(power), currentPropertyName);
            return resolvePropertyValueSuggestion(power, currentPropertyName, last, true);
        }
        List<String> suggestions;
        suggestions = required.stream().map(s -> s.getName() + ":").filter(s -> s.startsWith(last)).collect(Collectors.toList());
        if (!suggestions.isEmpty()) return suggestions; //required property
        suggestions = argMap.values().stream().filter(s -> !settled.contains(s)).map(s -> s.getName() + ":").filter(s -> s.startsWith(last)).collect(Collectors.toList());
        return suggestions; //unsettled property
    }

    private void actionBarTip(CommandSender sender, NamespacedKey power, String property) {
        if (sender instanceof Player) {
            Bukkit.getScheduler().runTask(RPGItems.plugin, () -> {
                String description = PowerManager.getDescription(power, property);
                if (description == null) {
                    return;
                }
                new Message(description).send((Player) sender, Message.MessageType.ACTION_BAR);
            });
        }
    }

    private List<String> resolveGetSet(String last, Arguments cmd, Pair<RPGItem, String> itemCommand) {
        RPGItem item = itemCommand.getKey();
        String powerName = cmd.next();
        NamespacedKey key;
        try {
            key = PowerManager.parseKey(powerName);
        } catch (UnknownExtensionException e) {
            return Collections.emptyList();
        }
        List<Power> powers = item.powers.stream().filter(p -> p.getNamespacedKey().equals(key)).collect(Collectors.toList());
        if (powers.isEmpty()) return Collections.emptyList();
        Class<? extends Power> powerClass = powers.get(0).getClass();
        if (cmd.top() == null) {
            // rpgitem item get/set power
            return IntStream.rangeClosed(1, powers.size()).mapToObj(Integer::toString).collect(Collectors.toList());
        } else {
            // rpgitem item get/set power 1 ...
            cmd.next();
        }
        if (cmd.top() == null) {
            // rpgitem item get/set power 1
            return PowerManager.getProperties(powerClass).keySet().stream().map(PowerProperty::name).filter(s -> s.startsWith(last)).collect(Collectors.toList());
        }
        if (itemCommand.getValue().equals("get")) return Collections.emptyList();
        // rpgitem item set power 1 property
        return resolvePropertyValueSuggestion(powerClass, cmd.next(), last, false);
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

    private static List<String> resolveSet(Set<String> values, String last) {
        List<String> currentVaules = Stream.of(last.split(",")).collect(Collectors.toList());
        String lastVaule = currentVaules.get(currentVaules.size() - 1);
        if (values.contains(lastVaule)) {
            lastVaule = "";
        } else {
            currentVaules.remove(currentVaules.size() - 1);
        }
        values.removeAll(currentVaules);
        String incompleteValue = lastVaule;
        String base = incompleteValue.isEmpty() ? last : last.replaceAll(incompleteValue + "$", "");
        boolean next = currentVaules.isEmpty() || base.endsWith(",");
        return values.stream().filter(n -> n.startsWith(incompleteValue)).map(n -> base + (next ? "" : ",") + n).collect(Collectors.toList());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (
                (args.length > 0 && ItemManager.getItemByName(args[0]) != null)
                        ||
                        (args.length > 1 && args[1].equals("create") && ItemManager.getItemByName(args[0]) == null)
        ) {
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
        if (cmd == null) return Collections.emptyList();
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
                        if (attr == null) return Collections.emptyList();
                        if (attr.startsWith("command")) {
                            // it's a global command and we have suggestion in attr
                            String[] att = attr.split(":", 2);
                            if (att.length > 1) {
                                return Arrays.asList(att[1].split(","));
                            }
                            return Collections.emptyList();
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
                    if (itemCommand == null) return Collections.emptyList(); // neither
                    String attr = subCommandAttribute.get(itemCommand.getValue());
                    if (attr == null) return Collections.emptyList();
                    String[] att = attr.split(":", 2);
                    switch (att[0]) {
                        case "property":
                        case "power":
                            // four case below
                            switch (itemCommand.getValue()) {
                                case "power":
                                    return powers.keySet().stream().map(s -> PowerManager.hasExtension() ? s : s.getKey()).map(Object::toString).collect(Collectors.toList()); // all powers
                                case "set":
                                case "get":
                                case "removepower":
                                    return itemCommand.getKey().powers.stream().map(Power::getNamespacedKey).map(s -> PowerManager.hasExtension() ? s : s.getKey()).map(Object::toString).collect(Collectors.toList()); // current powers
                                default:
                                    return Collections.emptyList();
                            }
                        case "item":
                        case "command": {
                            return att.length > 1 ? Arrays.asList(att[1].split(",")) : null; // suggestion bundled in attr
                        }
                        default:
                            return null;
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
                        if (attr == null) return Collections.emptyList();
                        String[] att = attr.split(":", 2);
                        switch (att[0]) {
                            case "property":
                            case "power":
                            case "item": {
                                return ItemManager.itemByName.keySet().stream().filter(s -> s.startsWith(second)).collect(Collectors.toList()); // items
                            }
                            case "items": {
                                return resolveSet(ItemManager.itemByName.keySet(), second); // items
                            }
                            case "command": {
                                return att.length > 1 ? Arrays.stream(att[1].split(",")).filter(s -> s.startsWith(second)).collect(Collectors.toList()) : null; // bundled in attr
                            }
                            default:
                                return Collections.emptyList();
                        }
                    }
                }
            }
            case 3: {
                String first = cmd.next();
                String second = cmd.next();
                String third = cmd.next();
                Pair<RPGItem, String> itemCommand = resolveItemCommand(first, second);
                if (itemCommand == null) return Collections.emptyList();
                String attr = subCommandAttribute.get(itemCommand.getValue());
                if (attr == null) return Collections.emptyList();
                String[] att = attr.split(":", 2);
                if (suggestion) {
                    return resolvePowerOrPropertySuggestion(sender, args); // only case is `/rpgitem power item somepower`
                } else {
                    switch (att[0]) {
                        case "property":
                        case "power":
                            switch (itemCommand.getValue()) {
                                case "power":
                                    return powers.keySet().stream()
                                                 .filter(s -> s.getKey().startsWith(third) || s.toString().startsWith(third))
                                                 .map(s -> PowerManager.hasExtension() ? s : s.getKey())
                                                 .map(Object::toString).collect(Collectors.toList()); // only case is `/rpgitem power item somepow`
                                case "set":
                                case "get":
                                case "removepower":
                                    return itemCommand.getKey()
                                                   .powers
                                                   .stream()
                                                   .map(Power::getNamespacedKey)
                                                   .filter(s -> s.getKey().startsWith(third) || s.toString().startsWith(third))
                                                   .map(s -> PowerManager.hasExtension() ? s : s.getKey())
                                                   .map(Object::toString)
                                                   .collect(Collectors.toList()); // complete current powers
                                default:
                                    return Collections.emptyList();
                            }
                        case "item": {
                            return att.length > 1 ? Arrays.stream(att[1].split(",")).filter(s -> s.startsWith(third)).collect(Collectors.toList()) : null; // bundled
                        }
                        default:
                            return Collections.emptyList();
                    }
                }
            }
            default: {
                return resolvePowerOrPropertySuggestion(sender, args);
            }
        }
    }

    protected void msg(CommandSender target, @LangKey(varArgsPosition = 1) String template, Map<String, BaseComponent> map, Object... args) {
        new Message("").append(i18n.getFormatted(template, args), map).send(target);
    }

    @Override
    protected boolean showCompleteMessage() {
        return false;
    }
}
