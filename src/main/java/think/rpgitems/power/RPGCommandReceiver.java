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
import think.rpgitems.api.firing.FiringLocation;
import think.rpgitems.api.firing.FiringLocations;
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

public abstract class RPGCommandReceiver extends CommandReceiver {
    public final LanguageRepository i18n;

    public RPGCommandReceiver(RPGItems plugin, LanguageRepository i18n) {
        super(plugin, i18n);
        this.i18n = i18n;
    }

    /**
     * Case-insensitive prefix test shared by the completers. Suggestions always keep their canonical spelling
     * (enum constants, trigger names such as {@code minion_OnAttack}), because that is what the setters accept.
     */
    public static boolean startsWithIgnoreCase(String candidate, String prefix) {
        return candidate.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Suggests registry keys (powers, conditions, markers) for the token being typed. A token without a
     * namespace also matches the key part, so {@code sen} offers {@code rgminion:sentry}.
     */
    public static List<String> suggestKeys(Collection<NamespacedKey> keys, String token) {
        boolean namespaced = PowerManager.hasExtension();
        return keys.stream()
                .filter(k -> startsWithIgnoreCase(k.toString(), token) || (!token.contains(":") && startsWithIgnoreCase(k.getKey(), token)))
                .map(k -> namespaced ? k.toString() : k.getKey())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Runs a completer, answering "nothing to suggest" if it fails (unknown item, bad power index, ...). NyaaCore
     * turns a failing completer into {@code null}, and Bukkit answers {@code null} with online player names.
     */
    public static List<String> quietly(java.util.function.Supplier<List<String>> completer) {
        try {
            List<String> result = completer.get();
            return result == null ? new ArrayList<>() : result;
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    /**
     * @return the property field as registered by {@link PowerManager}, which also finds the non-public
     * {@code @Property} fields extensions use
     */
    static Field propertyField(Class<? extends PropertyHolder> holder, String propertyName) {
        if (holder == null) return null;
        Map<String, Pair<Method, PropertyInstance>> properties = PowerManager.getProperties().get(holder);
        Pair<Method, PropertyInstance> property = properties == null ? null : properties.get(propertyName);
        if (property != null) {
            return property.getValue().field();
        }
        try {
            return holder.getField(propertyName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    public static List<String> resolvePropertyValueSuggestion(RPGItem item, Class<? extends PropertyHolder> power, String propertyName, String last, boolean hasNamePrefix) {
        Field field = propertyField(power, propertyName);
        if (field == null) {
            return Collections.emptyList();
        }
        return resolvePropertyValueSuggestion(item, power, field, last, hasNamePrefix);
    }

    @SuppressWarnings("unchecked")
    public static List<String> resolvePropertyValueSuggestion(RPGItem item, Class<? extends PropertyHolder> power, Field propertyField, String last, boolean hasNamePrefix) {
        BooleanChoice bc = propertyField.getAnnotation(BooleanChoice.class);
        if (bc != null) {
            return resolveSingleValue(propertyField, List.of(bc.trueChoice(), bc.falseChoice()), last, hasNamePrefix);
        }
        if (Collection.class.isAssignableFrom(propertyField.getType())) {
            if (!(propertyField.getGenericType() instanceof ParameterizedType listType)
                    || !(listType.getActualTypeArguments()[0] instanceof Class<?> listArg)) {
                return Collections.emptyList();
            }
            if (listArg.equals(Trigger.class)) {
                return resolveEnumListValue(power, propertyField, Trigger.keySet().stream().sorted().collect(Collectors.toList()), last, hasNamePrefix);
            }
            if (!listArg.isEnum()) {
                if (propertyField.getName().equalsIgnoreCase("conditions") && item != null) {
                    List<String> conditionIds = item.getConditions().stream().map(Condition::id).filter(Objects::nonNull).collect(Collectors.toList());
                    return resolveEnumListValue(power, propertyField, conditionIds, last, hasNamePrefix);
                }
                if (propertyField.getName().equalsIgnoreCase("selectors") && item != null) {
                    List<String> selectorIds = item.getMarker(Selector.class).stream().map(Selector::id).filter(Objects::nonNull).collect(Collectors.toList());
                    return resolveEnumListValue(power, propertyField, selectorIds, last, hasNamePrefix);
                }
                return Collections.emptyList();
            }
            List<String> enumValues = Stream.of(((Class<? extends Enum>) listArg).getEnumConstants()).map(Enum::name).collect(Collectors.toList());
            return resolveEnumListValue(power, propertyField, enumValues, last, hasNamePrefix);
        }
        AcceptedValue as = propertyField.getAnnotation(AcceptedValue.class);

        if (as != null) {
            return resolveSingleValue(propertyField, PowerManager.getAcceptedValue(power, as), last, hasNamePrefix);
        }
        if (propertyField.getType().equals(boolean.class) || propertyField.getType().equals(Boolean.class)) {
            return resolveSingleValue(propertyField, List.of("true", "false"), last, hasNamePrefix);
        }
        if (propertyField.getType().equals(FiringLocation.class)) {
            // live view of the provider registry: extension names come and go with their plugins
            return resolveSingleValue(propertyField, FiringLocations.names(), last, hasNamePrefix);
        }
        if (propertyField.getType().isEnum()) {
            return resolveSingleValue(propertyField, Stream.of(propertyField.getType().getEnumConstants()).map(Object::toString).collect(Collectors.toList()), last, hasNamePrefix);
        }

        return Collections.emptyList();
    }

    private static String valuePrefix(Field propertyField, boolean hasNamePrefix) {
        return hasNamePrefix ? propertyField.getName() + ":" : "";
    }

    private static String typedValue(String last, String prefix) {
        return startsWithIgnoreCase(last, prefix) ? last.substring(prefix.length()) : last;
    }

    private static List<String> resolveSingleValue(Field propertyField, Collection<String> values, String last, boolean hasNamePrefix) {
        String prefix = valuePrefix(propertyField, hasNamePrefix);
        String typed = typedValue(last, prefix);
        return values.stream().filter(v -> startsWithIgnoreCase(v, typed)).map(v -> prefix + v).distinct().collect(Collectors.toList());
    }

    /**
     * Completes a comma separated list value ({@code triggers:RIGHT_CLICK,MINION_ATTACK}). Only the entry after the
     * last comma is completed; entries before it are kept as typed. Set-valued properties and triggers do not
     * offer entries that are already in the list. A complete entry is also offered followed by each further entry.
     */
    public static List<String> resolveEnumListValue(Class<? extends PropertyHolder> power, Field propertyField, List<String> enumValues, String last, boolean hasNamePrefix) {
        String prefix = valuePrefix(propertyField, hasNamePrefix);
        String typed = typedValue(last, prefix);
        String head = typed.substring(0, typed.lastIndexOf(',') + 1);
        String partial = typed.substring(head.length());
        List<String> chosen = Stream.of(head.split(",")).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        List<String> candidates = enumValues.stream().filter(Objects::nonNull).distinct().collect(Collectors.toCollection(ArrayList::new));
        AcceptedValue as = propertyField.getAnnotation(AcceptedValue.class);
        if (as != null) {
            candidates.retainAll(PowerManager.getAcceptedValue(power, as));
        }
        boolean unique = Set.class.isAssignableFrom(propertyField.getType()) || (as != null && as.preset() == Preset.TRIGGERS);
        if (unique) {
            candidates.removeIf(c -> chosen.stream().anyMatch(c::equalsIgnoreCase));
        }

        Set<String> suggestions = new LinkedHashSet<>();
        for (String candidate : candidates) {
            if (startsWithIgnoreCase(candidate, partial)) {
                suggestions.add(prefix + head + candidate);
            }
        }
        if (candidates.contains(partial)) {
            for (String candidate : candidates) {
                if (!unique || !candidate.equals(partial)) {
                    suggestions.add(prefix + head + partial + "," + candidate);
                }
            }
        }
        return new ArrayList<>(suggestions);
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
        return resolvePropertiesSuggestions(sender, item, last, powerKey, power, argMap, settled, required);
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
        return resolvePropertiesSuggestions(sender, item, last, powerKey, holderClass(powerKey), argMap, settled, required);
    }

    private static Class<? extends PropertyHolder> holderClass(NamespacedKey key) {
        Class<? extends PropertyHolder> cls = PowerManager.getPower(key);
        if (cls == null) cls = PowerManager.getCondition(key);
        if (cls == null) cls = PowerManager.getMarker(key);
        return cls;
    }

    static List<String> resolvePropertiesSuggestions(CommandSender sender, RPGItem item, String last, NamespacedKey powerKey, Class<? extends PropertyHolder> holder, Map<String, Pair<Method, PropertyInstance>> argMap, Set<Field> settled, List<Field> required) {
        int colon = last.indexOf(':');
        if (colon >= 0) {
            // "name:value": complete the value. The name is matched like the keys are offered, ignoring case,
            // and is written back in its canonical spelling since the setter looks it up exactly.
            String typedName = last.substring(0, colon);
            String currentPropertyName = argMap.containsKey(typedName) ? typedName
                    : argMap.keySet().stream().filter(n -> n.equalsIgnoreCase(typedName)).findFirst().orElse(null);
            if (currentPropertyName != null) {
                actionBarTip(sender, powerKey, currentPropertyName);
                return resolvePropertyValueSuggestion(item, holder, currentPropertyName, currentPropertyName + last.substring(colon), true);
            }
        }
        List<String> suggestions;
        suggestions = required.stream().map(s -> s.getName() + ":").filter(s -> startsWithIgnoreCase(s, last)).collect(Collectors.toList());
        if (!suggestions.isEmpty()) return suggestions; //required property
        suggestions = argMap.values().stream().filter(s -> !settled.contains(s.getValue().field())).map(s -> s.getValue().name() + ":").filter(s -> startsWithIgnoreCase(s, last)).sorted().collect(Collectors.toList());
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
        I18n.sendMessage(sender, "message.propertyHolder.property", name, Strings.isNullOrEmpty(desc) ? I18n.getInstance(locale).getFormatted("message.propertyHolder.no_description") : desc);
        if (powerObj != null) {
            I18n.sendMessage(sender, "message.propertyHolder.property_value", Utils.getProperty(powerObj, name, prop.field()));
        }
    }

    @Override
    protected boolean showCompleteMessage() {
        return false;
    }
}