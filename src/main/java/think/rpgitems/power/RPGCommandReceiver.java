package think.rpgitems.power;

import cat.nyaa.nyaacore.LanguageRepository;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import think.rpgitems.RPGItems;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.marker.Selector;
import think.rpgitems.power.trigger.Trigger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static think.rpgitems.power.PowerManager.powers;

public abstract class RPGCommandReceiver extends CommandReceiver {
    private final LanguageRepository i18n;

    public RPGCommandReceiver(RPGItems plugin, LanguageRepository i18n) {
        super(plugin, i18n);
        this.i18n = i18n;
    }

    private static List<String> resolvePropertyValueSuggestion(RPGItem item, Class<? extends Power> power, String propertyName, String last, boolean hasNamePrefix) {
        try {
            return resolvePropertyValueSuggestion(item, power, power.getField(propertyName), last, hasNamePrefix);
        } catch (NoSuchFieldException e) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> resolvePropertyValueSuggestion(RPGItem item, Class<? extends Power> power, Field propertyField, String last, boolean hasNamePrefix) {
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
                if (propertyField.getName().equalsIgnoreCase("conditions")) {
                    List<Condition<?>> conditions = item.getConditions();
                    List<String> conditionIds = conditions.stream().map(Condition::id).collect(Collectors.toList());
                    return resolveEnumListValue(power, propertyField, conditionIds, last, hasNamePrefix);
                }
                if (propertyField.getName().equalsIgnoreCase("selectors")) {
                    List<Selector> selectors = item.getMarker(Selector.class);
                    List<String> selectorIds = selectors.stream().map(Selector::id).collect(Collectors.toList());
                    return resolveEnumListValue(power, propertyField, selectorIds, last, hasNamePrefix);
                }
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
        return resolveEnumCompletion(enumValues, last, hasNamePrefix, currentVaules, incompleteValue);
    }

    private static List<String> resolveEnumCompletion(Collection<String> enumValues, String last, boolean hasNamePrefix, List<String> currentVaules, String incompleteValue) {
        String base = incompleteValue.isEmpty() ? last : last.replaceAll(incompleteValue + "$", "");
        boolean next = (currentVaules.isEmpty() && !hasNamePrefix) || base.endsWith(":") || base.endsWith(",");
        return enumValues.stream().filter(n -> n.startsWith(incompleteValue)).map(n -> base + (next ? "" : ",") + n).collect(Collectors.toList());
    }

    private List<String> resolvePowerProperties(CommandSender sender, RPGItem item, String last, Arguments cmd) {
        String powName = cmd.next();
        NamespacedKey powerKey;
        try {
            powerKey = PowerManager.parseKey(powName);
        } catch (UnknownExtensionException e) {
            return Collections.emptyList();
        }
        Class<? extends Power> power = powers.get(powerKey);
        if (power == null) return Collections.emptyList();
        Map<String, Pair<Method, PropertyInstance>> argMap = PowerManager.getProperties(power);
        Set<Field> settled = new HashSet<>();

        List<Field> required = argMap.values().stream()
                                     .map(Pair::getValue)
                                     .filter(PropertyInstance::required)
                                     .sorted(Comparator.comparing(PropertyInstance::order))
                                     .map(PropertyInstance::field)
                                     .collect(Collectors.toList());

        Meta meta = power.getAnnotation(Meta.class);

        for (Map.Entry<String, Pair<Method, PropertyInstance>> prop : argMap.entrySet()) {
            Field field = prop.getValue().getValue().field();
            String name = prop.getKey();
            String value = cmd.argString(name, null);
            if (value != null
                        || isTrivialProperty(meta, name)
            ) {
                required.remove(field);
                settled.add(field);
            }
        }
        if (settled.isEmpty()) {
            actionBarTip(sender, powerKey, null);
        }
        return resolvePropertiesSuggestions(sender, item, last, power, argMap, settled, required);
    }

    protected static boolean isTrivialProperty(Meta meta, String name) {
        return (meta.immutableTrigger() && name.equals("triggers"))
                       || (meta.marker() && name.equals("triggers"))
                       || (meta.marker() && name.equals("conditions") && !meta.withConditions())
                       || (!meta.withSelectors() && name.equals("selectors"))
                       || (!meta.withContext() && name.equals("requiredContext"))
                       || name.equals("displayName");
    }

    private List<String> resolvePropertiesSuggestions(CommandSender sender, RPGItem item, String last, Class<? extends Power> power, Map<String, Pair<Method, PropertyInstance>> argMap, Set<Field> settled, List<Field> required) {
        if (argMap.keySet().stream().anyMatch(f -> last.startsWith(f + ":"))) {//we are suggesting a value as we have the complete property name
            String currentPropertyName = last.split(":")[0];
            actionBarTip(sender, powers.inverse().get(power), currentPropertyName);
            return resolvePropertyValueSuggestion(item, power, currentPropertyName, last, true);
        }
        List<String> suggestions;
        suggestions = required.stream().map(s -> s.getName() + ":").filter(s -> s.startsWith(last)).collect(Collectors.toList());
        if (!suggestions.isEmpty()) return suggestions; //required property
        suggestions = argMap.values().stream().filter(s -> !settled.contains(s.getValue().field())).map(s -> s.getValue().name() + ":").filter(s -> s.startsWith(last)).collect(Collectors.toList());
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

    private static Pair<RPGItem, String> resolveItemCommand(String f, String s) {
        Optional<RPGItem> rpgItem = ItemManager.getItem(f);
        if (rpgItem.isPresent()) {
            return new Pair<>(rpgItem.get(), s);
        }
        rpgItem = ItemManager.getItem(s);
        return rpgItem.map(r -> new Pair<>(r, f)).orElse(null);
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
        return resolveEnumCompletion(values, last, false, currentVaules, incompleteValue);
    }

    @Override
    protected boolean showCompleteMessage() {
        return false;
    }
}