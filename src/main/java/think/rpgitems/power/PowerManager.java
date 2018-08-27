/*
 *  This file is part of RPG Items.
 *
 *  RPG Items is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RPG Items is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RPG Items.  If not, see <http://www.gnu.org/licenses/>.
 */
package think.rpgitems.power;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.ClassPathUtils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.Bukkit;
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ExecutionException;
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

    static final LoadingCache<String, Plugin> nameSpaceCache =
            CacheBuilder.newBuilder().concurrencyLevel(2).build(
                    CacheLoader.from(
                            k -> Arrays.stream(Bukkit.getServer().getPluginManager().getPlugins()).filter(p -> p.getName().toLowerCase(Locale.ROOT).equals(k)).reduce((a, b) -> {
                                throw new IllegalStateException("Multiple elements: " + a + ", " + b);
                            }).orElse(null)
                    )
            );

    /**
     * Power by name, and name by power
     */
    static BiMap<NamespacedKey, Class<? extends Power>> powers = HashBiMap.create();

    static final HashMap<Plugin, BiFunction<NamespacedKey, String, String>> descriptionResolvers = new HashMap<>();

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

    public static void registerPowers(Plugin plugin, Class<? extends Power>... powers) {
        nameSpaceCache.put(plugin.getName().toLowerCase(Locale.ROOT), plugin);
        Stream.of(powers).filter(c -> !Modifier.isAbstract(c.getModifiers()) && !c.isInterface()).sorted(Comparator.comparing(Class::getCanonicalName)).forEach(PowerManager::registerPower);
    }

    public static void addDescriptionResolver(Plugin plugin, BiFunction<NamespacedKey, String, String> descriptionResolver) {
        descriptionResolvers.put(plugin, descriptionResolver);
    }

    public static NamespacedKey parseKey(String powerStr) {
        if (!powerStr.contains(":")) return new NamespacedKey(RPGItems.plugin, powerStr);
        String[] split = powerStr.split(":");
        if (split.length != 2) {
            throw new IllegalArgumentException();
        }
        try {
            Plugin namespace = nameSpaceCache.get(split[0]);
            return new NamespacedKey(namespace, split[1]);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPowerProperty(CommandSender sender, Power power, String field, String value) throws IllegalAccessException {
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
    public static void setPowerProperty(CommandSender sender, Power power, Field field, String value) throws IllegalAccessException {
        Class<? extends Power> cls = power.getClass();
        BooleanChoice bc = field.getAnnotation(BooleanChoice.class);
        if (bc != null) {
            String trueChoice = bc.trueChoice();
            String falseChoice = bc.falseChoice();
            if (value.equalsIgnoreCase(trueChoice) || value.equalsIgnoreCase(falseChoice)) {
                field.set(power, value.equalsIgnoreCase(trueChoice));
            } else {
                throw new Handler.CommandException("message.error.invalid_option", field.getName(), falseChoice + ", " + trueChoice);//TODO
            }
            return;
        }
        AcceptedValue as = field.getAnnotation(AcceptedValue.class);
        if (as != null) {
            List<String> acc = getAcceptedValue(cls, as);
            if (!acc.contains(value) && !Collection.class.isAssignableFrom(field.getType())) {
                throw new Handler.CommandException("message.error.invalid_option", field.getName(), acc.stream().reduce(" ", (a, b) -> a + ", " + b));
            }
        }
        setPowerPropertyInternal(sender, power, field, value);
    }

    @SuppressWarnings("unchecked")
    private static void setPowerPropertyInternal(CommandSender sender, Power power, Field field, String value) {
        try {
            Deserializer st = field.getAnnotation(Deserializer.class);
            Class<? extends Power> cls = power.getClass();
            if (st != null) {
                try {
                    Object v = Setter.from(power, st.value()).set(value);
                    field.set(power, v);
                } catch (IllegalArgumentException e) {
                    new Message(I18n.format(st.message())).send(sender);
                }
            } else {
                if (field.getType().equals(int.class)) {
                    try {
                        field.set(power, Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        throw new Handler.CommandException("internal.error.bad_int", value);
                    }
                } else if (field.getType().equals(long.class)) {
                    try {
                        field.set(power, Long.parseLong(value));
                    } catch (NumberFormatException e) {
                        throw new Handler.CommandException("internal.error.bad_int", value);
                    }
                } else if (field.getType().equals(double.class)) {
                    try {
                        field.set(power, Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        throw new Handler.CommandException("internal.error.bad_double", value);
                    }
                } else if (field.getType().equals(String.class)) {
                    field.set(power, value);
                } else if (field.getType().equals(boolean.class)) {
                    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                        field.set(power, Boolean.valueOf(value));
                    } else {
                        throw new Handler.CommandException("message.error.invalid_option", field.getName(), "true, false");
                    }
                } else if (field.getType().isEnum()) {
                    try {
                        field.set(power, Enum.valueOf((Class<Enum>) field.getType(), value));
                    } catch (IllegalArgumentException e) {
                        throw new Handler.CommandException("internal.error.bad_enum", field.getName(), Stream.of(field.getType().getEnumConstants()).map(Object::toString).reduce(" ", (a, b) -> a + ", " + b));
                    }
                } else if (Collection.class.isAssignableFrom(field.getType())) {
                    ParameterizedType listType = (ParameterizedType) field.getGenericType();
                    Class<?> listArg = (Class<?>) listType.getActualTypeArguments()[0];

                    AcceptedValue as = field.getAnnotation(AcceptedValue.class);
                    if (as != null) {
                        List<String> acc = getAcceptedValue(cls, as);
                        if (Arrays.stream(value.split(",")).anyMatch(v -> !acc.contains(v))) {
                            throw new Handler.CommandException("message.error.invalid_option", field.getName(), String.join(", ", acc));
                        }
                    }
                    Stream<String> values = Arrays.stream(value.split(","));
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
            throw new Handler.CommandException("internal.error.command_exception");
        }
    }

    public static List<String> getAcceptedValue(Class<? extends Power> cls, AcceptedValue anno) {
        if (anno.preset() != Preset.NONE) {
            return anno.preset().get(cls);
        } else {
            return Arrays.asList(anno.value());
        }
    }

    public static SortedMap<PowerProperty, Field> getProperties(Class<? extends Power> cls) {
        return properties.get(cls);
    }

    public static Class<? extends Power> getPower(NamespacedKey key) {
        return powers.get(key);
    }

    public static Class<? extends Power> getPower(String key) {
        return getPower(parseKey(key));
    }

    public static boolean hasPower(NamespacedKey key) {
        return powers.containsKey(key);
    }
}
