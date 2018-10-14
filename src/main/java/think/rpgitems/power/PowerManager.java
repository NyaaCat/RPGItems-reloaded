package think.rpgitems.power;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.ClassPathUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import think.rpgitems.Handler;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.utils.MaterialUtils;

import javax.annotation.CheckForNull;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Power Manager for registering and inspecting powers.
 */
@SuppressWarnings("unchecked")
public class PowerManager {
    private static final Map<Class<? extends Power>, SortedMap<PowerProperty, Field>> properties = new HashMap<>();

    private static final Map<String, Plugin> extensions = new HashMap<>();

    /**
     * Power by name, and name by power
     */
    static BiMap<NamespacedKey, Class<? extends Power>> powers = HashBiMap.create();

    private static final HashMap<Plugin, BiFunction<NamespacedKey, String, String>> descriptionResolvers = new HashMap<>();

    private static void registerPower(Class<? extends Power> clazz) {
        NamespacedKey key;
        try {
            Power p = clazz.getConstructor().newInstance();
            key = p.getNamespacedKey();
            if (key != null) {
                powers.put(key, clazz);
            }
        } catch (Exception e) {
            RPGItems.plugin.getLogger().log(Level.WARNING, "Failed to add power", e);
            RPGItems.plugin.getLogger().log(Level.WARNING, "With {0}", clazz);
            return;
        }
        SortedMap<PowerProperty, Field> argumentPriorityMap = getPowerProperties(clazz);
        properties.put(clazz, argumentPriorityMap);
    }

    private static SortedMap<PowerProperty, Field> getPowerProperties(Class<? extends Power> cls) {
        SortedMap<PowerProperty, Field> argumentPriorityMap = new TreeMap<>(Comparator.comparing(PowerProperty::order).thenComparing(PowerProperty::hashCode));
        Arrays.stream(cls.getFields())
              .filter(field -> field.getAnnotation(Property.class) != null)
              .forEach(field -> argumentPriorityMap.put(new PowerProperty(field.getName(), field.getAnnotation(Property.class).required(), field.getAnnotation(Property.class).order()), field));
        return argumentPriorityMap;
    }

    public static void registerPowers(Plugin plugin, String basePackage) {
        Class<? extends Power>[] classes = ClassPathUtils.scanSubclasses(plugin, basePackage, Power.class);
        registerPowers(plugin, classes);
    }

    @SuppressWarnings({"WeakerAccess"})
    public static void registerPowers(Plugin plugin, Class<? extends Power>... powers) {
        extensions.put(plugin.getName().toLowerCase(Locale.ROOT), plugin);
        Stream.of(powers).filter(c -> !Modifier.isAbstract(c.getModifiers()) && !c.isInterface()).sorted(Comparator.comparing(Class::getCanonicalName)).forEach(PowerManager::registerPower);
    }

    public static void addDescriptionResolver(Plugin plugin, BiFunction<NamespacedKey, String, String> descriptionResolver) {
        descriptionResolvers.put(plugin, descriptionResolver);
    }

    public static NamespacedKey parseKey(String powerStr) throws UnknownExtensionException {
        if (!powerStr.contains(":")) return new NamespacedKey(RPGItems.plugin, powerStr);
        String[] split = powerStr.split(":");
        if (split.length != 2) {
            throw new IllegalArgumentException();
        }

        Plugin namespace = extensions.get(split[0].toLowerCase(Locale.ROOT));
        if (namespace == null) {
            throw new UnknownExtensionException(split[0]);
        }
        return new NamespacedKey(namespace, split[1]);
    }

    public static NamespacedKey parseLegacyKey(String powerStr) {
        if (powerStr.contains(":")) {
            throw new IllegalArgumentException();
        }
        return new NamespacedKey(RPGItems.plugin, powerStr);
    }

    public static void setPowerProperty(CommandSender sender, Power power, String field, String value) throws
            IllegalAccessException {
        Field f;
        Class<? extends Power> cls = power.getClass();
        try {
            f = cls.getField(field);
        } catch (NoSuchFieldException e) {
            throw new Handler.CommandException("internal.error.invalid_command_arg", e);//TODO
        }
        setPowerProperty(sender, power, f, value);
    }

    @SuppressWarnings("unchecked")
    public static void setPowerProperty(CommandSender sender, Power power, Field field, String value) throws
            IllegalAccessException {
        Class<? extends Power> cls = power.getClass();
        BooleanChoice bc = field.getAnnotation(BooleanChoice.class);
        if (bc != null) {
            String trueChoice = bc.trueChoice();
            String falseChoice = bc.falseChoice();
            if (value.equalsIgnoreCase(trueChoice) || value.equalsIgnoreCase(falseChoice)) {
                field.set(power, value.equalsIgnoreCase(trueChoice));
            } else {
                throw new Handler.CommandException("message.error.invalid_option", value, field.getName(), falseChoice + ", " + trueChoice);//TODO
            }
            return;
        }
        AcceptedValue as = field.getAnnotation(AcceptedValue.class);
        if (as != null) {
            List<String> acc = getAcceptedValue(cls, as);
            if (!acc.contains(value) && !Collection.class.isAssignableFrom(field.getType())) {
                throw new Handler.CommandException("message.error.invalid_option", value, field.getName(), String.join(", ", acc));
            }
        }
        setPowerPropertyInternal(sender, power, field, value);
    }

    @SuppressWarnings("unchecked")
    private static void setPowerPropertyInternal(CommandSender sender, Power power, Field field, String value) {
        try {
            Deserializer st = field.getAnnotation(Deserializer.class);
            field.setAccessible(true);
            Class<? extends Power> cls = power.getClass();
            if (st != null) {
                try {
                    Object v = Setter.from(power, st.value()).set(value);
                    field.set(power, v);
                } catch (IllegalArgumentException e) {
                    new Message(I18n.format(st.message())).send(sender);
                }
            } else {
                if (value.equals("null")) {
                    field.set(power, null);
                    return;
                }
                if (field.getType().equals(int.class) || field.getType().equals(Integer.class)) {
                    try {
                        field.set(power, Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        throw new Handler.CommandException("internal.error.bad_int", value);
                    }
                } else if (field.getType().equals(long.class) || field.getType().equals(Long.class)) {
                    try {
                        field.set(power, Long.parseLong(value));
                    } catch (NumberFormatException e) {
                        throw new Handler.CommandException("internal.error.bad_int", value);
                    }
                } else if (field.getType().equals(double.class) || field.getType().equals(Double.class)) {
                    try {
                        field.set(power, Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        throw new Handler.CommandException("internal.error.bad_double", value);
                    }
                } else if (field.getType().equals(String.class)) {
                    field.set(power, value);
                } else if (field.getType().equals(boolean.class) || field.getType().equals(Boolean.class)) {
                    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                        field.set(power, Boolean.valueOf(value));
                    } else {
                        throw new Handler.CommandException("message.error.invalid_option", value, field.getName(), "true, false");
                    }
                } else if (field.getType().isEnum()) {
                    try {
                        field.set(power, Enum.valueOf((Class<Enum>) field.getType(), value));
                    } catch (IllegalArgumentException e) {
                        throw new Handler.CommandException("internal.error.bad_enum", field.getName(), Stream.of(field.getType().getEnumConstants()).map(Object::toString).collect(Collectors.joining(", ")));
                    }
                } else if (Collection.class.isAssignableFrom(field.getType())) {
                    ParameterizedType listType = (ParameterizedType) field.getGenericType();
                    Class<?> listArg = (Class<?>) listType.getActualTypeArguments()[0];
                    String[] valueStrs = value.split(",");
                    AcceptedValue as = field.getAnnotation(AcceptedValue.class);
                    if (as != null) {
                        List<String> acc = getAcceptedValue(cls, as);
                        if (Arrays.stream(valueStrs).filter(s -> !s.isEmpty()).anyMatch(v -> !acc.contains(v))) {
                            throw new Handler.CommandException("message.error.invalid_option", value, field.getName(), String.join(", ", acc));
                        }
                    }
                    Stream<String> values = Arrays.stream(valueStrs).filter(s -> !s.isEmpty());
                    if (field.getType().equals(List.class)) {
                        if (listArg.isEnum()) {
                            Class<? extends Enum> enumClass = (Class<? extends Enum>) listArg;
                            List<Enum> list = (List<Enum>) values.map(v -> Enum.valueOf(enumClass, v)).collect(Collectors.toList());
                            field.set(power, list);
                        } else if (listArg.equals(String.class)) {
                            List<String> list = values.collect(Collectors.toList());
                            field.set(power, list);
                        } else {
                            throw new Handler.CommandException("internal.error.command_exception");
                        }
                    } else {
                        if (listArg.isEnum()) {
                            Class<? extends Enum> enumClass = (Class<? extends Enum>) listArg;
                            Set<Enum> set = (Set<Enum>) values.map(v -> Enum.valueOf(enumClass, v)).collect(Collectors.toSet());
                            field.set(power, set);
                        } else if (listArg.equals(String.class)) {
                            Set<String> set = values.collect(Collectors.toSet());
                            field.set(power, set);
                        } else {
                            throw new Handler.CommandException("internal.error.command_exception");
                        }
                    }
                } else if (field.getType() == ItemStack.class) {
                    Material m = MaterialUtils.getMaterial(value, sender);
                    ItemStack item;
                    if (sender instanceof Player && value.equalsIgnoreCase("HAND")) {
                        ItemStack hand = ((Player) sender).getInventory().getItemInMainHand();
                        if (hand == null || hand.getType() == Material.AIR) {
                            throw new Handler.CommandException("message.error.iteminhand");
                        }
                        item = hand.clone();
                        item.setAmount(1);
                    } else if (m == null || m == Material.AIR || !m.isItem()) {
                        throw new Handler.CommandException("message.error.material", value);
                    } else {
                        item = new ItemStack(m);
                    }
                    field.set(power, item.clone());
                } else {
                    throw new Handler.CommandException("internal.error.invalid_command_arg");
                }
            }
        } catch (IllegalAccessException e) {
            throw new Handler.CommandException("internal.error.command_exception", e);
        }
    }

    public static List<String> getAcceptedValue(Class<? extends Power> cls, AcceptedValue anno) {
        if (anno.preset() != Preset.NONE) {
            return Stream.concat(Arrays.stream(anno.value()), anno.preset().get(cls).stream())
                         .collect(Collectors.toList());
        } else {
            return Arrays.asList(anno.value());
        }
    }

    /**
     * @return All registered extensions mapped by theirs name
     */
    @SuppressWarnings("unused")
    public static Map<String, Plugin> getExtensions() {
        return Collections.unmodifiableMap(extensions);
    }

    /**
     * @return All registered powers' properties mapped by theirs class
     */
    @SuppressWarnings("unused")
    public static Map<Class<? extends Power>, SortedMap<PowerProperty, Field>> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * @return All registered powers mapped by theirs key
     */
    @SuppressWarnings("unused")
    public static Map<NamespacedKey, Class<? extends Power>> getPowers() {
        return Collections.unmodifiableMap(powers);
    }

    public static SortedMap<PowerProperty, Field> getProperties(Class<? extends Power> cls) {
        return Collections.unmodifiableSortedMap(properties.get(cls));
    }

    @CheckForNull
    public static Class<? extends Power> getPower(NamespacedKey key) {
        return powers.get(key);
    }

    @CheckForNull
    public static Class<? extends Power> getPower(String key) throws UnknownExtensionException {
        return getPower(parseKey(key));
    }

    public static boolean hasPower(NamespacedKey key) {
        return powers.containsKey(key);
    }

    public static String getDescription(NamespacedKey power, String property) {
        Plugin plugin = extensions.get(power.getNamespace());
        return PowerManager.descriptionResolvers.get(plugin).apply(power, property);
    }
}
