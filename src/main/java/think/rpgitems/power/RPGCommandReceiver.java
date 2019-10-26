package think.rpgitems.power;

import cat.nyaa.nyaacore.LanguageRepository;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import com.google.common.base.Strings;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.marker.Selector;
import think.rpgitems.power.trigger.Trigger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static think.rpgitems.AdminCommands.msgs;

public abstract class RPGCommandReceiver extends CommandReceiver {
    public final LanguageRepository i18n;

    public RPGCommandReceiver(RPGItems plugin, LanguageRepository i18n) {
        super(plugin, i18n);
        this.i18n = i18n;
    }

    public static List<String> resolvePropertyValueSuggestion(RPGItem item, Class<? extends Power> power, String propertyName, String last, boolean hasNamePrefix) {
        try {
            return resolvePropertyValueSuggestion(item, power, power.getField(propertyName), last, hasNamePrefix);
        } catch (NoSuchFieldException e) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> resolvePropertyValueSuggestion(RPGItem item, Class<? extends Power> power, Field propertyField, String last, boolean hasNamePrefix) {
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

    public static List<String> resolveEnumListValue(Class<? extends Power> power, Field propertyField, List<String> enumValues, String last, boolean hasNamePrefix) {
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

    public static List<String> resolveEnumCompletion(Collection<String> enumValues, String last, boolean hasNamePrefix, List<String> currentVaules, String incompleteValue) {
        String base = incompleteValue.isEmpty() ? last : last.replaceAll(incompleteValue + "$", "");
        boolean next = (currentVaules.isEmpty() && !hasNamePrefix) || base.endsWith(":") || base.endsWith(",");
        return enumValues.stream().filter(n -> n.startsWith(incompleteValue)).map(n -> base + (next ? "" : ",") + n).collect(Collectors.toList());
    }

    public static List<String> resolveProperties(CommandSender sender, RPGItem item, Class<? extends PropertyHolder> power, NamespacedKey powerKey, String last, Arguments cmd, boolean newPower) {
        if (power == null) return Collections.emptyList();
        Map<String, Pair<Method, PropertyInstance>> argMap = PowerManager.getProperties(power);
        Set<Field> settled = new HashSet<>();

        List<Field> required = newPower ? argMap.values().stream()
                                                .map(Pair::getValue)
                                                .filter(PropertyInstance::required)
                                                .sorted(Comparator.comparing(PropertyInstance::order))
                                                .map(PropertyInstance::field)
                                                .collect(Collectors.toList()) : new ArrayList<>();

        Meta meta = power.getAnnotation(Meta.class);

        for (Map.Entry<String, Pair<Method, PropertyInstance>> prop : argMap.entrySet()) {
            Field field = prop.getValue().getValue().field();
            String name = prop.getKey();
            String value = cmd.argString(name, null);
            if (value != null
                        || isTrivialProperty(meta, name)
            ) {
                required.remove(field);
            }
            if (value != null) {
                settled.add(field);
            }
        }
        if (settled.isEmpty()) {
            actionBarTip(sender, powerKey, null);
        }
        return resolvePropertiesSuggestions(sender, item, last, powerKey, argMap, settled, required);
    }

    protected static boolean isTrivialProperty(Meta meta, String name) {
        return (meta.immutableTrigger() && name.equals("triggers"))
                       || (meta.marker() && name.equals("triggers"))
                       || (meta.marker() && name.equals("conditions") && !meta.withConditions())
                       || (!meta.withSelectors() && name.equals("selectors"))
                       || (!meta.withContext() && name.equals("requiredContext"))
                       || name.equals("displayName");
    }

    public static List<String> resolvePropertiesSuggestions(CommandSender sender, RPGItem item, String last, NamespacedKey powerKey, Map<String, Pair<Method, PropertyInstance>> argMap, Set<Field> settled, List<Field> required) {
        if (argMap.keySet().stream().anyMatch(f -> last.startsWith(f + ":"))) {//we are suggesting a value as we have the complete property name
            String currentPropertyName = last.split(":")[0];
            actionBarTip(sender, powerKey, currentPropertyName);
            return resolvePropertyValueSuggestion(item, PowerManager.getPower(powerKey), currentPropertyName, last, true);
        }
        List<String> suggestions;
        suggestions = required.stream().map(s -> s.getName() + ":").filter(s -> s.startsWith(last)).collect(Collectors.toList());
        if (!suggestions.isEmpty()) return suggestions; //required property
        suggestions = argMap.values().stream().filter(s -> !settled.contains(s.getValue().field())).map(s -> s.getValue().name() + ":").filter(s -> s.startsWith(last)).collect(Collectors.toList());
        return suggestions; //unsettled property
    }

    public static void actionBarTip(CommandSender sender, NamespacedKey power, String property) {
        if (sender instanceof Player) {
            Bukkit.getScheduler().runTask(RPGItems.plugin, () -> {
                String description = PowerManager.getDescription(((Player) sender).getLocale(), power, property);
                if (description == null) {
                    return;
                }
                new Message(description).send((Player) sender, Message.MessageType.ACTION_BAR);
            });
        }
    }

    public static void showProp(CommandSender sender, NamespacedKey powerKey, PropertyInstance prop, PropertyHolder powerObj) {
        String name = prop.name();
        String locale = RPGItems.plugin.cfg.language;
        if (sender instanceof Player) {
            locale = ((Player) sender).getLocale();
        }
        Meta meta = PowerManager.getMeta(powerKey);
        if (isTrivialProperty(meta, name)) {
            return;
        }
        String desc = PowerManager.getDescription(locale, powerKey, name);
        msgs(sender, "message.propertyHolder.property", name, Strings.isNullOrEmpty(desc) ? I18n.getInstance(locale).format("message.propertyHolder.no_description") : desc);
        if (powerObj != null) {
            msgs(sender, "message.propertyHolder.property_value", Utils.getProperty(powerObj, name, prop.field()));
        }
    }

    @Override
    protected boolean showCompleteMessage() {
        return false;
    }
}