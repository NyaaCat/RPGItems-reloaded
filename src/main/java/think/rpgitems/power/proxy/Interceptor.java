package think.rpgitems.power.proxy;

import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.utils.ItemTagUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static think.rpgitems.item.RPGItem.getModifiers;

public class Interceptor {
    private static final Cache<String, Pair<origPowerHolder, Power>> POWER_CACHE = CacheBuilder.newBuilder().weakValues().build();

    // Phase 5: Cache proxy CLASSES by (powerClass, triggerClass) - eliminates ByteBuddy regeneration (was 84% of CPU)
    private static final Map<String, Class<? extends Power>> PROXY_CLASS_CACHE = new ConcurrentHashMap<>();
    // Cache constructor MethodHandles for fast proxy instantiation
    private static final Map<Class<?>, MethodHandle> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();
    // Cache the _interceptor field for fast injection
    private static final Map<Class<?>, Field> INTERCEPTOR_FIELD_CACHE = new ConcurrentHashMap<>();

    private final Power orig;
    private final Player player;
    private final Map<Method, PropertyInstance> getters;
    private final ItemStack stack;
    private final MethodHandles.Lookup lookup;
    // Performance optimization: cache modifiers at construction time instead of fetching on every intercept
    private final List<Modifier> allModifiers;
    // Performance optimization: cache MethodHandles to avoid repeated unreflect() calls
    private final Map<Method, MethodHandle> methodHandleCache = new ConcurrentHashMap<>();

    protected Interceptor(Power orig, Player player, ItemStack stack, MethodHandles.Lookup lookup) {
        this.lookup = lookup;
        this.orig = orig;
        this.player = player;
        this.getters = PowerManager.getProperties(orig.getClass())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getValue().getKey(), e -> e.getValue().getValue()));
        this.stack = stack;
        // Cache modifiers at construction time - eliminates 2x getModifiers() calls per intercept
        List<Modifier> playerModifiers = getModifiers(player);
        List<Modifier> stackModifiers = getModifiers(stack);
        this.allModifiers = Stream.concat(playerModifiers.stream(), stackModifiers.stream())
                .sorted(Comparator.comparing(Modifier::priority))
                .toList();
    }

    /**
     * Gets the original (non-proxied) Power that this Interceptor wraps.
     */
    public Power getOrig() {
        return orig;
    }

    public static Power create(Power orig, Player player, ItemStack stack, Trigger trigger) {
        // Phase 5 optimization: The expensive ByteBuddy class generation is now cached in getOrCreateProxyClass().
        // The POWER_CACHE provides secondary caching of proxy instances to avoid Interceptor constructor overhead.
        // However, with class caching, the primary bottleneck (84% CPU) is eliminated.

        String cacheKey = getCacheKey(player, stack, orig);
        Pair<origPowerHolder, Power> result = POWER_CACHE.getIfPresent(cacheKey);
        if (result != null) {
            origPowerHolder holder = result.getKey();
            // Validate cache entry matches current context
            if (holder.itemStack().equals(stack) &&
                holder.playerId().equals(player.getUniqueId()) &&
                holder.orig().equals(orig)) {
                return result.getValue();
            }
        }

        // Create new proxy (fast now - class is cached, only Interceptor instantiation needed)
        Power proxyPower = makeProxy(orig, player, stack, trigger);
        POWER_CACHE.put(cacheKey, Pair.of(new origPowerHolder(player.getUniqueId(), stack, orig), proxyPower));
        return proxyPower;
    }

    /**
     * Gets the original (non-proxied) Power from a proxy Power.
     * Returns the input if it's not a proxy or not found.
     * This allows bypassing proxy overhead when direct field access is needed.
     *
     * @param proxy The potentially proxied Power
     * @return The original Power if found, otherwise the input
     */
    @SuppressWarnings("unchecked")
    public static <T extends Power> T getOriginal(T proxy) {
        if (proxy == null) return null;

        // Check if it's a ByteBuddy proxy by class name
        if (!proxy.getClass().getName().contains("ByteBuddy")) {
            return proxy; // Not a proxy, return as-is
        }

        // Phase 5 fix: Access the _interceptor field directly from the proxy
        // This is reliable because every proxy has this field injected at creation
        try {
            Field interceptorField = INTERCEPTOR_FIELD_CACHE.get(proxy.getClass());
            if (interceptorField == null) {
                interceptorField = proxy.getClass().getField("_interceptor");
                INTERCEPTOR_FIELD_CACHE.put(proxy.getClass(), interceptorField);
            }
            Interceptor interceptor = (Interceptor) interceptorField.get(proxy);
            if (interceptor != null) {
                return (T) interceptor.getOrig();
            }
        } catch (Exception e) {
            RPGItems.logger.warning("Failed to get original power from proxy: " + e.getMessage());
        }

        // Fallback: Search cache (shouldn't normally be reached)
        for (Pair<origPowerHolder, Power> entry : POWER_CACHE.asMap().values()) {
            if (entry.getValue() == proxy) {
                return (T) entry.getKey().orig();
            }
        }

        return proxy; // Not found, return as-is (will use default values - this is a bug state)
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

        try {
            // Get or create the proxy CLASS (cached - no ByteBuddy generation after first call)
            Class<? extends Power> proxyClass = getOrCreateProxyClass(
                    orig.getClass(),
                    trigger.getPowerClass(),
                    lookup
            );

            // Get or create cached constructor MethodHandle
            final MethodHandles.Lookup finalLookup = lookup;
            MethodHandle constructorMH = CONSTRUCTOR_CACHE.computeIfAbsent(proxyClass, c -> {
                try {
                    return finalLookup.findConstructor(c, MethodType.methodType(void.class));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to find constructor for proxy class", e);
                }
            });

            // Instantiate proxy (fast - just constructor call)
            Power proxy = (Power) constructorMH.invoke();

            // Get or create cached field reference for injection
            Field interceptorField = INTERCEPTOR_FIELD_CACHE.computeIfAbsent(proxyClass, c -> {
                try {
                    return c.getField("_interceptor");
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Failed to find _interceptor field", e);
                }
            });

            // Inject the Interceptor instance with player/stack-specific data
            interceptorField.set(proxy, new Interceptor(orig, player, stack, finalLookup));

            return proxy;
        } catch (Throwable e) {
            RPGItems.logger.severe("make proxy error: " + orig.getClass() + " - " + e.getMessage());
            e.printStackTrace();
            return orig;
        }
    }

    private static String getCacheKey(Player player, ItemStack itemStack, Power orig) {
        String playerHash = player.getUniqueId().toString();
        String itemHash = ItemTagUtils.getString(itemStack, RPGItem.NBT_ITEM_UUID).orElseGet(() -> String.valueOf(itemStack.hashCode()));
        String origHash = orig.getName() + ":" + orig.getPlaceholderId() + ":" + orig.getClass().getName();
        return playerHash + "-#-" + itemHash + "-#-" + origHash;

    }

    /**
     * Cache key for proxy class - only depends on power class and trigger class.
     * This is stable across all players/items, allowing massive reuse.
     */
    private static String getClassCacheKey(Class<? extends Power> origClass, Class<?> triggerClass) {
        return origClass.getName() + "##" + triggerClass.getName();
    }

    /**
     * Gets or creates a cached proxy class for the given power and trigger combination.
     * Uses field-based delegation so the Interceptor instance can be injected per-use.
     * This eliminates the 84% CPU overhead from ByteBuddy class generation.
     */
    private static Class<? extends Power> getOrCreateProxyClass(
            Class<? extends Power> origClass,
            Class<?> triggerClass,
            MethodHandles.Lookup lookup) {

        String classKey = getClassCacheKey(origClass, triggerClass);

        return PROXY_CLASS_CACHE.computeIfAbsent(classKey, k -> {
            try {
                return new ByteBuddy()
                        .subclass(origClass)
                        .implement(triggerClass)
                        .implement(NotUser.class)
                        .defineField("_interceptor", Interceptor.class, Visibility.PUBLIC)
                        .method(ElementMatchers.any())
                        .intercept(MethodDelegation.toField("_interceptor"))
                        .make()
                        .load(origClass.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(lookup))
                        .getLoaded();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create proxy class for " + origClass.getName(), e);
            }
        });
    }

    @RuntimeType
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object intercept(@AllArguments Object[] args, @Origin Method method) {
        try {
            PropertyInstance propertyInstance = getters.get(method);

            // FAST PATH: Not a property getter - invoke directly
            if (propertyInstance == null) {
                return invokeMethodCached(method, orig, args);
            }

            Class<?> type = propertyInstance.field().getType();

            // FAST PATH: Non-numeric type - no modifiers can apply
            if (!(type == int.class || type == Integer.class ||
                  type == float.class || type == Float.class ||
                  type == double.class || type == Double.class)) {
                return invokeMethodCached(method, orig, args);
            }

            // Filter modifiers for this property (use cached allModifiers)
            List<Modifier<Double>> numberModifiers = allModifiers.stream()
                    .filter(m -> m.getModifierTargetType() == Double.class && m.match(orig, propertyInstance))
                    .map(m -> (Modifier<Double>) m)
                    .toList();

            // FAST PATH: No modifiers match this property
            if (numberModifiers.isEmpty()) {
                return invokeMethodCached(method, orig, args);
            }

            // Apply modifiers (only for numeric properties with matching modifiers)
            Number value = (Number) invokeMethodCached(method, orig, args);
            double origValue = value.doubleValue();
            for (Modifier<Double> numberModifier : numberModifiers) {
                RgiParameter param = new RgiParameter<>(orig.getItem(), orig, stack, origValue);
                origValue = numberModifier.apply(param);
            }

            // Return appropriately typed result
            if (int.class.equals(type) || Integer.class.equals(type)) {
                return (int) Math.round(origValue);
            } else if (float.class.equals(type) || Float.class.equals(type)) {
                return (float) origValue;
            } else {
                return origValue;
            }
        } catch (Throwable e) {
            RPGItems.logger.severe("invoke method error:" + method);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Cached MethodHandle invocation - avoids repeated unreflect() calls.
     */
    private Object invokeMethodCached(Method method, Object obj, Object... args) throws Throwable {
        MethodHandle mh = methodHandleCache.computeIfAbsent(method, m -> {
            try {
                return lookup.unreflect(m);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        return mh.bindTo(obj).invokeWithArguments(args);
    }
}

record origPowerHolder(UUID playerId, ItemStack itemStack, Power orig) {
}

