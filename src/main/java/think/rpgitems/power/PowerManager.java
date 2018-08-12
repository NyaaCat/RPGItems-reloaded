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
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.*;
import think.rpgitems.power.impl.BasePower;

import java.lang.annotation.Annotation;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for all powers
 */
@SuppressWarnings("unchecked")
public class PowerManager {
    public static final HashBasedTable<Class<? extends Power>, String, BiFunction<Object, String, String>> transformers = HashBasedTable.create();
    public static final HashBasedTable<Class<? extends Power>, String, BiFunction<Object, String, Boolean>> validators = HashBasedTable.create();
    public static final HashBasedTable<Class<? extends Power>, String, BiConsumer<Object, String>> setters = HashBasedTable.create();
    public static final HashBasedTable<Class<? extends Power>, String, Function<Object, String>> getters = HashBasedTable.create();
    public static final Map<Class<? extends Power>, SortedMap<PowerProperty, Field>> propertyOrders = new HashMap<>();
    /**
     * Power by name, and name by power
     */
    public static BiMap<String, Class<? extends Power>> powers = HashBiMap.create();

    private static void addPower(Class<? extends Power> clazz) {
        String name = null;
        try {
            Power p = clazz.getConstructor().newInstance();
            name = p.getName();
            if (!Strings.isNullOrEmpty(name)) {
                powers.put(name, clazz);
            }
        } catch (Exception e) {
            RPGItems.plugin.getLogger().log(Level.WARNING, "Failed to add power:" + name, e);
            RPGItems.plugin.getLogger().log(Level.WARNING, "With {0}", clazz);
            return;
        }
        inspectPower(clazz);
    }

    private static void inspectPower(Class<? extends Power> cls) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Map<Class<? extends Annotation>, List<Annotation>> annos =
                Arrays.stream(cls.getFields())
                      .flatMap(field -> Arrays.stream(field.getAnnotations()))
                      .distinct()
                      .collect(Collectors.groupingBy(Annotation::annotationType, Collectors.toList()));

        MethodType transformerType = MethodType.methodType(String.class, cls, String.class);
        List<Transformer> transformerList;
        if (annos.get(Transformer.class) == null) {
            transformerList = new ArrayList<>();
        } else {
            transformerList = annos.get(Transformer.class)
                                   .stream().map(i -> (Transformer) i)
                                   .collect(Collectors.toList());
        }
        transformerList.forEach(
                exactTransformers(cls, lookup, transformerType)
        );

        MethodType validatorType = MethodType.methodType(boolean.class, cls, String.class);
        List<Validator> validatorList;
        if (annos.get(Validator.class) == null) {
            validatorList = new ArrayList<>();
        } else {
            validatorList = annos.get(Validator.class)
                                 .stream().map(i -> (Validator) i)
                                 .collect(Collectors.toList());
        }
        validatorList.forEach(
                exactValidators(cls, lookup, validatorType)
        );

        MethodType setterType = MethodType.methodType(void.class, cls, String.class);
        List<Setter> setterList;
        if (annos.get(Setter.class) == null) {
            setterList = new ArrayList<>();
        } else {
            setterList = annos.get(Setter.class)
                              .stream().map(i -> (Setter) i)
                              .collect(Collectors.toList());
        }
        setterList.forEach(
                exactSetters(cls, lookup, setterType)
        );

        MethodType getterType = MethodType.methodType(String.class, cls);
        List<Getter> getterList;
        if (annos.get(Getter.class) == null) {
            getterList = new ArrayList<>();
        } else {
            getterList = annos.get(Getter.class)
                              .stream().map(i -> (Getter) i)
                              .collect(Collectors.toList());
        }
        getterList.forEach(
                exactGetters(cls, lookup, getterType)
        );

        SortedMap<PowerProperty, Field> argumentPriorityMap = getPowerProperties(cls);
        propertyOrders.put(cls, argumentPriorityMap);
    }

    private static SortedMap<PowerProperty, Field> getPowerProperties(Class<? extends Power> cls) {
        SortedMap<PowerProperty, Field> argumentPriorityMap = new TreeMap<>(Comparator.comparing(PowerProperty::order).thenComparing(PowerProperty::hashCode));
        Arrays.stream(cls.getFields())
              .filter(field -> field.getAnnotation(Property.class) != null)
              .forEach(field -> argumentPriorityMap.put(new PowerProperty(field.getName(), field.getAnnotation(Property.class).required(), field.getAnnotation(Property.class).order()), field));
        return argumentPriorityMap;
    }

    private static Consumer<Setter> exactSetters(Class<? extends Power> cls, MethodHandles.Lookup lookup, MethodType setterType) {
        return setterAnno -> {
            String fname = setterAnno.value();
            try {
                Method m = cls.getMethod(fname, String.class);
                MethodHandle mh = lookup.unreflect(m);
                if (!mh.type().equals(setterType)) {
                    return;
                }
                setters.put(cls, fname, (BiConsumer<Object, String>) LambdaMetafactory.metafactory(
                        lookup,
                        "accept",
                        MethodType.methodType(BiConsumer.class),
                        setterType.generic().changeReturnType(void.class),
                        mh,
                        setterType).getTarget().invokeExact());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };
    }

    private static Consumer<Getter> exactGetters(Class<? extends Power> cls, MethodHandles.Lookup lookup, MethodType getterType) {
        return getterAnno -> {
            String fname = getterAnno.value();
            try {
                Method m = cls.getMethod(fname);
                MethodHandle mh = lookup.unreflect(m);
                if (!mh.type().equals(getterType)) {
                    return;
                }
                getters.put(cls, fname, (Function<Object, String>) LambdaMetafactory.metafactory(
                        lookup,
                        "accept",
                        MethodType.methodType(Function.class),
                        getterType.generic(),
                        mh,
                        getterType).getTarget().invokeExact());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };
    }

    private static Consumer<Validator> exactValidators(Class<? extends Power> cls, MethodHandles.Lookup lookup, MethodType validatorType) {
        return valiAnno -> {
            String fname = valiAnno.value();
            try {
                Method m = cls.getMethod(fname, String.class);
                MethodHandle mh = lookup.unreflect(m);
                if (!mh.type().equals(validatorType)) {
                    return;
                }
                validators.put(cls, fname, (BiFunction<Object, String, Boolean>) LambdaMetafactory.metafactory(
                        lookup,
                        "apply",
                        MethodType.methodType(BiFunction.class),
                        validatorType.generic(),
                        mh,
                        validatorType).getTarget().invokeExact());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };
    }

    private static Consumer<Transformer> exactTransformers(Class<? extends Power> cls, MethodHandles.Lookup lookup, MethodType transformerType) {
        return tranAnno -> {
            String fname = tranAnno.value();
            try {
                Method m = cls.getMethod(fname, String.class);
                MethodHandle mh = lookup.unreflect(m);
                if (!mh.type().equals(transformerType)) {
                    return;
                }
                transformers.put(cls, fname, (BiFunction<Object, String, String>) LambdaMetafactory.metafactory(
                        lookup,
                        "apply",
                        MethodType.methodType(BiFunction.class),
                        transformerType.generic(),
                        mh,
                        transformerType).getTarget().invokeExact());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };
    }

    public static void load() {
        Class<? extends Power>[] classes = ClassPathUtils.scanSubclasses(RPGItems.plugin, BasePower.class.getPackage().getName(), Power.class);
        Stream.of(classes).filter(c -> !Modifier.isAbstract(c.getModifiers()) && !c.isInterface()).sorted(Comparator.comparing(Class::getCanonicalName)).forEach(PowerManager::addPower);
    }
}
