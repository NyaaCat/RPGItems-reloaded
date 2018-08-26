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

import cat.nyaa.nyaacore.utils.ClassPathUtils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.PowerProperty;
import think.rpgitems.commands.Property;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Power Manager for registering and inspecting powers.
 */
@SuppressWarnings("unchecked")
public class PowerManager {
    public static final Map<Class<? extends Power>, SortedMap<PowerProperty, Field>> propertyOrders = new HashMap<>();

    public static final LoadingCache<String, Plugin> keyCache =
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
    public static BiMap<NamespacedKey, Class<? extends Power>> powers = HashBiMap.create();

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
        propertyOrders.put(clazz, argumentPriorityMap);
    }

    private static SortedMap<PowerProperty, Field> getPowerProperties(Class<? extends Power> cls) {
        SortedMap<PowerProperty, Field> argumentPriorityMap = new TreeMap<>(Comparator.comparing(PowerProperty::order).thenComparing(PowerProperty::hashCode));
        Arrays.stream(cls.getFields())
              .filter(field -> field.getAnnotation(Property.class) != null)
              .forEach(field -> argumentPriorityMap.put(new PowerProperty(field.getName(), field.getAnnotation(Property.class).required(), field.getAnnotation(Property.class).order()), field));
        return argumentPriorityMap;
    }

    public static void load(Plugin plugin, String basePackage) {
        keyCache.put(plugin.getName(), plugin);
        Class<? extends Power>[] classes = ClassPathUtils.scanSubclasses(plugin, basePackage, Power.class);
        Stream.of(classes).filter(c -> !Modifier.isAbstract(c.getModifiers()) && !c.isInterface()).sorted(Comparator.comparing(Class::getCanonicalName)).forEach(PowerManager::registerPower);
    }

    public static NamespacedKey parseKey(String powerStr) {
        if (!powerStr.contains(":")) return new NamespacedKey(RPGItems.plugin, powerStr);
        String[] split = powerStr.split(":");
        if (split.length != 2) {
            throw new IllegalArgumentException();
        }
        try {
            Plugin key = keyCache.get(split[0]);
            return new NamespacedKey(key, split[1]);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
