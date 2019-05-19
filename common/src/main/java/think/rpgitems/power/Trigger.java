package think.rpgitems.power;

import cat.nyaa.nyaacore.Pair;
import com.google.common.base.Strings;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.ItemManager;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static think.rpgitems.power.Utils.maxWithCancel;
import static think.rpgitems.power.Utils.minWithCancel;

public abstract class Trigger<TEvent extends Event, TPower extends Power, TResult, TReturn> {

    private static final Map<String, Trigger> registry = new HashMap<>();

    public static Stream<Trigger> fromInterface(Class<? extends Power> power) {
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
        }
        registry.put(name, trigger);
    }

    private final Class<TEvent> eventClass;
    private final Class<TResult> resultClass;
    private final Class<TPower> powerClass;
    private final Class<TReturn> returnClass;
    private final String name;

    @SuppressWarnings("unchecked")
    protected Trigger(Class<TEvent> eventClass, Class<TPower> powerClass, Class<TResult> resultClass, Class returnClass, String name) {
        this(name, eventClass, powerClass, resultClass, returnClass);
        register(this);
    }

    public Trigger(String name, Class<TEvent> eventClass, Class<TPower> powerClass, Class<TResult> resultClass, Class<TReturn> returnClass) {
        this.eventClass = eventClass;
        this.resultClass = resultClass;
        this.powerClass = powerClass;
        this.returnClass = returnClass;
        this.name = name;
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

    @Override
    public String toString() {
        return name();
    }

}