package think.rpgitems.power.proxy;

import cat.nyaa.nyaacore.utils.ItemTagUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.RPGItems;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerManager;
import think.rpgitems.power.PropertyInstance;
import think.rpgitems.power.propertymodifier.Modifier;
import think.rpgitems.power.propertymodifier.RgiParameter;
import think.rpgitems.power.trigger.Trigger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static think.rpgitems.item.RPGItem.getModifiers;

public class Interceptor {
    private static final Cache<String, Power> POWER_CACHE = CacheBuilder.newBuilder().weakValues().build();
    private final Power orig;
    private final Player player;
    private final Map<Method, PropertyInstance> getters;
    private final ItemStack stack;
    private final MethodHandles.Lookup lookup;

    protected Interceptor(Power orig, Player player, ItemStack stack, MethodHandles.Lookup lookup) {
        this.lookup = lookup;
        this.orig = orig;
        this.player = player;
        this.getters = PowerManager.getProperties(orig.getClass())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getValue().getKey(), e -> e.getValue().getValue()));
        this.stack = stack;
    }

    public static Power create(Power orig, Player player, ItemStack stack, Trigger trigger) {
        Power result = POWER_CACHE.getIfPresent(getCacheKey(player, stack, orig));
        if (result != null) return result;
        result = makeProxy(orig, player, stack, trigger);
        POWER_CACHE.put(getCacheKey(player, stack, orig), result);
        return result;

    }

    private static Power makeProxy(Power orig, Player player, ItemStack stack, Trigger trigger) {
        MethodHandles.Lookup lookup = orig.getLookup();
        if (lookup == null) lookup = MethodHandles.lookup();
        if (lookup.lookupClass() != orig.getClass()) {
            try {
                lookup = MethodHandles.privateLookupIn(orig.getClass(), lookup);
            } catch (IllegalAccessException e) {
                RPGItems.logger.severe("make proxy error: can not get lookup (is it outdated?): " + orig.getClass());
                e.printStackTrace();
                return orig;
            }
        }

        MethodHandle constructorMH;

        try {
            Class<? extends Power> proxyClass = makeProxyClass(orig, player, stack, trigger, lookup);
            constructorMH = lookup.findConstructor(proxyClass, MethodType.methodType(void.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            RPGItems.logger.severe("make proxy error: not instantiatable: " + orig.getClass());
            e.printStackTrace();
            return orig;
        }

        try {
            return (Power) constructorMH.invoke();
        } catch (Throwable e) {
            RPGItems.logger.severe("make proxy error: not instantiatable (invoke error): " + orig.getClass());
            e.printStackTrace();
            return orig;
        }
    }

    private static String getCacheKey(Player player, ItemStack itemStack, Power orig) {
        String playerHash = player.getUniqueId().toString();
        String itemHash = ItemTagUtils.getString(itemStack, RPGItem.NBT_ITEM_UUID).orElseGet(() -> String.valueOf(itemStack.hashCode()));
        String origHash = orig.getClass().getName();
        return playerHash + "-#-" + itemHash + "-#-" + origHash;

    }

    private static Class<? extends Power> makeProxyClass(Power orig, Player player, ItemStack stack, Trigger trigger, MethodHandles.Lookup lookup) throws NoSuchMethodException {
        Class<? extends Power> origClass = orig.getClass();


        return new ByteBuddy()
                .subclass(origClass)
                .implement(new Class[]{trigger.getPowerClass()})
                .implement(NotUser.class)
                .method(ElementMatchers.any())
                .intercept(MethodDelegation.to(new Interceptor(orig, player, stack, lookup)))
                .make()
                .load(origClass.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(lookup))
                .getLoaded();
    }

    @RuntimeType
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object intercept(@AllArguments Object[] args, @Origin Method method) {
        try {
            if (getters.containsKey(method)) {
                PropertyInstance propertyInstance = getters.get(method);
                Class<?> type = propertyInstance.field().getType();
                List<Modifier> playerModifiers = getModifiers(player);
                List<Modifier> stackModifiers = getModifiers(stack);
                List<Modifier> modifiers = Stream.concat(playerModifiers.stream(), stackModifiers.stream()).sorted(Comparator.comparing(Modifier::priority)).collect(Collectors.toList());
                // Numeric modifiers
                if (type == int.class || type == Integer.class || type == float.class || type == Float.class || type == double.class || type == Double.class) {

                    List<Modifier<Double>> numberModifiers = modifiers.stream().filter(m -> (m.getModifierTargetType() == Double.class) && m.match(orig, propertyInstance)).map(m -> (Modifier<Double>) m).collect(Collectors.toList());
                    Number value = (Number) invokeMethod(method, orig, args);
                    double origValue = value.doubleValue();
                    for (Modifier<Double> numberModifier : numberModifiers) {
                        RgiParameter param = new RgiParameter<>(orig.getItem(), orig, stack, origValue);

                        origValue = numberModifier.apply(param);
                    }
                    if (int.class.equals(type) || Integer.class.equals(type)) {
                        return (int) Math.round(origValue);
                    } else if (float.class.equals(type) || Float.class.equals(type)) {
                        return (float) (origValue);
                    } else {
                        return origValue;
                    }
                }
            }

            return invokeMethod(method, orig, args);
        } catch (Throwable e) {
            RPGItems.logger.severe("invoke method error:" + method);
            e.printStackTrace();
        }
        return null;
    }

    private Object invokeMethod(Method method, Object obj, Object... args) throws Throwable {
        //method.trySetAccessible();
        MethodHandle MH;
        MH = lookup.unreflect(method);
        MH = MH.bindTo(obj);
        return MH.invokeWithArguments(args);
    }
}
