package think.rpgitems.power;

import cat.nyaa.nyaacore.utils.ClassPathUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import think.rpgitems.Handler;
import think.rpgitems.RPGItems;

import javax.annotation.CheckForNull;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Power Manager for registering and inspecting powers.
 */
@SuppressWarnings("unchecked")
public class PowerManager {
    private static final Map<Class<? extends Power>, SortedMap<PowerProperty, Field>> properties = new HashMap<>();

    private static final Map<Class<? extends Power>, PowerMeta> metas = new HashMap<>();

    private static final Map<String, Plugin> extensions = new HashMap<>();

    /**
     * Power by name, and name by power
     */
    static BiMap<NamespacedKey, Class<? extends Power>> powers = HashBiMap.create();

    private static final HashMap<Plugin, BiFunction<NamespacedKey, String, String>> descriptionResolvers = new HashMap<>();

    static final HashBasedTable<Class<? extends Power>, Class<? extends Power>, Function> adapters = HashBasedTable.create();

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
        metas.put(clazz, clazz.getAnnotation(PowerMeta.class));
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
        powerStr = powerStr.trim();
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
        Utils.setPowerProperty(sender, power, f, value);
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

    public static SortedMap<PowerProperty, Field> getProperties(NamespacedKey key) {
        return Collections.unmodifiableSortedMap(properties.get(getPower(key)));
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

    static boolean hasExtension() {
        return extensions.size() > 1;
    }

    public static PowerMeta getMeta(NamespacedKey key) {
        return getMeta(getPower(key));
    }

    public static PowerMeta getMeta(Class<? extends Power> cls) {
        return metas.get(cls);
    }

    public static <G extends Power, S extends Power> void registAdapter(Class<G> general, Class<S> specified, Function<G, S> adapter) {
        adapters.put(general, specified, adapter);
    }

    public static <T extends Power> T adaptPower(Power power, Class<T> specified) {
        List<Class<? extends Power>> generals = Arrays.asList(power.getClass().getAnnotation(PowerMeta.class).generalInterface());
        Set<Class<? extends Power>> statics = Power.getStaticInterfaces(power.getClass());
        List<Class<? extends Power>> preferences = generals.stream().filter(statics::contains).collect(Collectors.toList());

        for (Class<? extends Power> general : preferences) {
            if (adapters.contains(general, specified)) {
                return (T) adapters.get(general, specified).apply(power);
            }
        }
        throw new ClassCastException();
    }
}
