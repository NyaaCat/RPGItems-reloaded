package think.rpgitems.power.trigger;

import cat.nyaa.nyaacore.Pair;
import com.google.common.base.Strings;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

public abstract class Trigger<
        TEvent extends Event, TPower extends Power, TPimpl extends Pimpl<TPower>, TResult, TReturn>
    extends BasePropertyHolder {

  private static boolean acceptingNew = true;

  private static final Map<String, Trigger> registry = new HashMap<>();

  public static Stream<Trigger> fromInterface(Class<? extends Pimpl> power) {
    return registry.values().stream().filter(t -> t.pimplClass.equals(power));
  }

  @Nullable
  public static Trigger get(String name) {
    return registry.get(name);
  }

  public static Set<Trigger> getValid(List<String> name, Set<String> ignored) {
    Set<Trigger> triggers = new HashSet<>();
    name.stream()
        .filter(((Predicate<String>) Strings::isNullOrEmpty).negate())
        .map(s -> Pair.of(s, Trigger.get(s)))
        .forEach(
            p -> {
              if (p.getValue() == null) {
                ignored.add(p.getKey());
              } else {
                triggers.add(p.getValue());
              }
            });
    return triggers;
  }

  public static Stream<Trigger> getValid(Stream<String> name) {
    return name.filter(((Predicate<String>) Strings::isNullOrEmpty).negate())
        .map(Trigger::get)
        .filter(Objects::nonNull);
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
      throw new IllegalStateException(
          "No longer accepting new triggers (can only be done when loading): " + trigger.name);
    }
    registry.put(name, trigger);
    PowerManager.registerMetas(trigger.getClass());
  }

  public static boolean isAcceptingRegistrations() {
    return acceptingNew;
  }

  public static void stopAcceptingRegistrations() {
    acceptingNew = false;
  }

  @Override
  public final String getPropertyHolderType() {
    return "trigger";
  }

  @Override
  public String getName() {
    return name();
  }

  private final Class<TEvent> eventClass;
  private final Class<TResult> resultClass;
  private final Class<TPimpl> pimplClass;
  private final Class<TReturn> returnClass;
  private final String name;
  private final String base;

  @Property public int priority;

  @SuppressWarnings({"unchecked", "rawtypes"})
  Trigger(
      Class<TEvent> eventClass,
      Class<? extends Pimpl> pimplClass,
      Class<TResult> resultClass,
      Class returnClass,
      String name) {
    this(name, eventClass, pimplClass, resultClass, returnClass);
    register(this);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Trigger(
      String name,
      Class<TEvent> eventClass,
      Class<? extends Pimpl> pimplClass,
      Class<TResult> resultClass,
      Class<TReturn> returnClass) {
    this.eventClass = eventClass;
    this.pimplClass = (Class<TPimpl>) pimplClass;
    this.resultClass = resultClass;
    this.returnClass = returnClass;
    this.name = name;
    this.base = null;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  Trigger(
      String name,
      String base,
      Class<TEvent> eventClass,
      Class<? extends Pimpl> pimplClass,
      Class<TResult> resultClass,
      Class returnClass) {
    this.eventClass = eventClass;
    this.pimplClass = (Class<TPimpl>) pimplClass;
    this.resultClass = resultClass;
    this.returnClass = returnClass;
    this.name = name;
    if (Trigger.get(base) == null) {
      throw new IllegalArgumentException();
    }
    this.base = base;
  }

  public int getPriority() {
    return priority;
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

  public abstract PowerResult<TResult> run(
      TPower power, TPimpl pimpl, Player player, ItemStack i, TEvent event);

  public PowerResult<TResult> run(
      TPower power, TPimpl pimpl, Player player, ItemStack i, TEvent event, Object data) {
    return run(power, pimpl, player, i, event);
  }

  public PowerResult<TResult> warpResult(
      PowerResult<Void> overrideResult,
      TPower power,
      TPimpl pimpl,
      Player player,
      ItemStack i,
      TEvent event) {
    return overrideResult.with(null);
  }

  public Class<? extends TPimpl> getPimplClass() {
    return pimplClass;
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
  public Trigger<TEvent, TPower, TPimpl, TResult, TReturn> copy(String name) {
    if (Trigger.get(name) != null) throw new IllegalArgumentException("name is used");
    try {
      return getClass().getConstructor(String.class).newInstance(name);
    } catch (NoSuchMethodException
        | InstantiationException
        | IllegalAccessException
        | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
