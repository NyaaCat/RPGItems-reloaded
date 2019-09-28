package think.rpgitems.power.trigger;

import cat.nyaa.nyaacore.Pair;
import com.google.common.base.Strings;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.BasePropertyHolder;
import think.rpgitems.power.Pimpl;
import think.rpgitems.power.PowerResult;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Trigger<TEvent extends Event, TPower extends Pimpl, TResult, TReturn> extends BasePropertyHolder {

    private static boolean acceptingNew = true;

    private static final Map<String, Trigger> registry = new HashMap<>();

    public static Stream<Trigger> fromInterface(Class<? extends Pimpl> power) {
        return registry.values().stream().filter(t -> t.powerClass.equals(power));
    }

    @Nullable
    public static Trigger get(String name) {
        return registry.get(name);
    }

    public static Set<Trigger> getValid(List<String> name, Set<String> ignored) {
        Set<Trigger> triggers = new HashSet<>();
        name.stream().filter(((Predicate<String>) Strings::isNullOrEmpty).negate()).map(s -> Pair.of(s, Trigger.get(s))).forEach(
                p -> {
                    if (p.getValue() == null) {
                        ignored.add(p.getKey());
                    } else {
                        triggers.add(p.getValue());
                    }
                }
        );
        return triggers;
    }

    public static Stream<Trigger> getValid(Stream<String> name) {
        return name.filter(((Predicate<String>) Strings::isNullOrEmpty).negate()).map(Trigger::get).filter(Objects::nonNull);
    }

    public static Trigger valueOf(String name) {
        Trigger trigger = registry.get(name);
        if (trigger == null) throw new IllegalArgumentException();
        return trigger;
    }

    public static Set<Trigger> valueOf(String[] name) {
        return valueOf(Arrays.stream(name)).collect(Collectors.toSet());
    }

    public static Stream<Trigger> valueOf(Stream<String> name) {
        return name.filter(((Predicate<String>) Strings::isNullOrEmpty).negate()).map(Trigger::valueOf);
    }

    public static void register(Trigger trigger) {
        String name = trigger.name();
        if (registry.containsKey(name)) {
            throw new IllegalArgumentException("Cannot set already-set trigger: " + trigger.name);
        } else if (!isAcceptingRegistrations()) {
            throw new IllegalStateException("No longer accepting new triggers (can only be done when loading): " + trigger.name);
        }
        registry.put(name, trigger);
    }

    public static boolean isAcceptingRegistrations() {
        return acceptingNew;
    }

    public static void stopAcceptingRegistrations() {
        acceptingNew = false;
    }

    @Override
    public final String getType() {
        return "trigger";
    }

    @Override
    public String getName() {
        return name();
    }

    private final Class<TEvent> eventClass;
    private final Class<TResult> resultClass;
    private final Class<TPower> powerClass;
    private final Class<TReturn> returnClass;
    private final String name;
    private final String base;

    @SuppressWarnings("unchecked")
    Trigger(Class<TEvent> eventClass, Class<TPower> powerClass, Class<TResult> resultClass, Class returnClass, String name) {
        this(name, eventClass, powerClass, resultClass, returnClass);
        register(this);
    }

    public Trigger(String name, Class<TEvent> eventClass, Class<TPower> powerClass, Class<TResult> resultClass, Class<TReturn> returnClass) {
        this.eventClass = eventClass;
        this.powerClass = powerClass;
        this.resultClass = resultClass;
        this.returnClass = returnClass;
        this.name = name;
        this.base = null;
    }

    public Trigger(String name, String base, Class<TEvent> eventClass, Class<TPower> powerClass, Class<TResult> resultClass, Class<TReturn> returnClass) {
        this.eventClass = eventClass;
        this.powerClass = powerClass;
        this.resultClass = resultClass;
        this.returnClass = returnClass;
        this.name = name;
        if(Trigger.get(base) == null) {
            throw new IllegalArgumentException();
        }
        this.base = base;
    }


    public static Set<String> keySet() {
        return registry.keySet();
    }

    public static Collection<Trigger> values() {
        return registry.values();
    }

    public TReturn def(Player player, ItemStack i, TEvent event) {
        return null;
    }

    public TReturn next(TReturn a, PowerResult<TResult> b) {
        return null;
    }

    public boolean check(Player player, ItemStack i, TEvent event) {
        return true;
    }

    public abstract PowerResult<TResult> run(TPower power, Player player, ItemStack i, TEvent event);

    public PowerResult<TResult> run(TPower power, Player player, ItemStack i, TEvent event, Object data) {
        return run(power, player, i, event);
    }

    public PowerResult<TResult> warpResult(PowerResult<Void> overrideResult, TPower power, Player player, ItemStack i, TEvent event) {
        return overrideResult.with(null);
    }

    public Class<TPower> getPowerClass() {
        return powerClass;
    }

    public Class<TResult> getResultClass() {
        return resultClass;
    }

    public Class<TEvent> getEventClass() {
        return eventClass;
    }

    public Class<TReturn> getReturnClass() {
        return returnClass;
    }

    public String name() {
        return name;
    }

    public String getBase() {
        return base;
    }

    @Override
    public String toString() {
        return name();
    }

    @SuppressWarnings("unchecked")
    public Trigger<TEvent, TPower, TResult, TReturn> copy(String name) {
        if (Trigger.get(name) != null) throw new IllegalArgumentException("name is used");
        try {
            return getClass()
                           .getConstructor(String.class, String.class, Class.class, Class.class, Class.class, Class.class)
                           .newInstance(name, this.name(), this.getEventClass(), this.getPowerClass(), this.getReturnClass(), this.getResultClass());
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}