package think.rpgitems.power.proxy;

import cat.nyaa.nyaacore.utils.ItemTagUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerManager;
import think.rpgitems.power.PropertyInstance;
import think.rpgitems.power.propertymodifier.Modifier;
import think.rpgitems.power.propertymodifier.RgiParameter;
import think.rpgitems.power.trigger.Trigger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static think.rpgitems.item.RPGItem.getModifiers;

public class Interceptor {
    public static final String HANDLER_FIELD_NAME = "power_handler";

    private static final Cache<String, Power> cache = CacheBuilder.newBuilder().weakValues().build();

    public static Power create(Power orig, Player player, Class<? extends Power> cls, ItemStack stack, Trigger trigger) {
        Power result = cache.getIfPresent(getCacheKey(player, stack, orig));
        if (result != null) return result;
        result = makeProxy(orig, player, cls, stack, trigger);
        cache.put(getCacheKey(player, stack, orig), result);
        return result;

    }

    private static Power makeProxy(Power orig, Player player, Class<? extends Power> cls, ItemStack stack, Trigger trigger) {
        try {
            return makeProxyClass(orig, player, cls, stack, trigger).getDeclaredConstructor(cls).newInstance(orig);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return orig;
    }

    private static String getCacheKey(Player player, ItemStack itemStack, Power orig) {
        String playerHash = player.getUniqueId().toString();
        String itemHash = ItemTagUtils.getString(itemStack, RPGItem.NBT_ITEM_UUID).orElseGet(() -> String.valueOf(itemStack.hashCode()));
        String origHash = orig.getClass().getName();
        return playerHash + "-" + itemHash + "-" + origHash;

    }

    private static Class<? extends Power> makeProxyClass(Power orig, Player player, Class<? extends Power> cls, ItemStack stack, Trigger trigger) {
        return new ByteBuddy()
                .subclass(cls)
                .implement(new Class[]{trigger.getPowerClass()})
                .implement(NotUser.class)

                .defineField(HANDLER_FIELD_NAME, orig.getClass(), Visibility.PUBLIC)

                .defineConstructor(Visibility.PUBLIC)
                .withParameters(orig.getClass())
                .intercept(FieldAccessor.ofField(HANDLER_FIELD_NAME).setsArgumentAt(0))

                .method(ElementMatchers.any())
                .intercept(MethodDelegation.to(new Interceptor(orig, player, stack)))
                .make()
                .load(cls.getClassLoader())
                .getLoaded();
    }

    private final Power orig;
    private final Player player;
    private final Map<Method, PropertyInstance> getters;
    private ItemStack stack;

    protected Interceptor(Power orig, Player player, ItemStack stack) {
        this.orig = orig;
        this.player = player;
        this.getters = PowerManager.getProperties(orig.getClass())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getValue().getKey(), e -> e.getValue().getValue()));
        this.stack = stack;
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
                    Number value = (Number) method.invoke(orig, args);
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

            return method.invoke(orig, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
