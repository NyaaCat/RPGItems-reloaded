package think.rpgitems.power;

import cat.nyaa.nyaacore.Pair;
import com.google.common.base.Strings;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
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

public abstract class Trigger<TEvent extends Event, TPower extends Power, TResult, TReturn> {

    private static boolean acceptingNew = true;

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

    public static final Trigger<EntityDamageByEntityEvent, PowerHit, Double, Double> HIT = new Trigger<EntityDamageByEntityEvent, PowerHit, Double, Double>(EntityDamageByEntityEvent.class, PowerHit.class, Double.class, Double.class, "HIT") {
        @Override
        public Double def(Player player, ItemStack i, EntityDamageByEntityEvent event) {
            return event.getDamage();
        }

        @Override
        public Double next(Double a, PowerResult<Double> b) {
            return b.isOK() ? Math.max(a, b.data()) : a;
        }

        @Override
        public PowerResult<Double> warpResult(PowerResult<Void> overrideResult, PowerHit power, Player player, ItemStack i, EntityDamageByEntityEvent event) {
            return overrideResult.with(event.getDamage());
        }

        @Override
        public PowerResult<Double> run(PowerHit power, Player player, ItemStack i, EntityDamageByEntityEvent event) {
            return power.hit(player, i, (LivingEntity) event.getEntity(), event.getDamage(), event);
        }
    };

    public static final Trigger<ProjectileHitEvent, PowerProjectileHit, Void, Void> PROJECTILE_HIT = new Trigger<ProjectileHitEvent, PowerProjectileHit, Void, Void>(ProjectileHitEvent.class, PowerProjectileHit.class, Void.class, Void.class, "PROJECTILE_HIT") {
        @Override
        public PowerResult<Void> run(PowerProjectileHit power, Player player, ItemStack i, ProjectileHitEvent event) {
            return power.projectileHit(player, i, event);
        }
    };

    public static final Trigger<EntityDamageEvent, PowerHitTaken, Double, Double> HIT_TAKEN = new Trigger<EntityDamageEvent, PowerHitTaken, Double, Double>(EntityDamageEvent.class, PowerHitTaken.class, Double.class, Double.class, "HIT_TAKEN") {
        @Override
        public Double def(Player player, ItemStack i, EntityDamageEvent event) {
            return event.getDamage();
        }

        @Override
        public Double next(Double a, PowerResult<Double> b) {
            return b.isOK() ? Math.min(a, b.data()) : a;
        }

        @Override
        public PowerResult<Double> warpResult(PowerResult<Void> overrideResult, PowerHitTaken power, Player player, ItemStack i, EntityDamageEvent event) {
            return overrideResult.with(event.getDamage());
        }

        @Override
        public PowerResult<Double> run(PowerHitTaken power, Player player, ItemStack i, EntityDamageEvent event) {
            return power.takeHit(player, i, event.getDamage(), event);
        }
    };

    public static final Trigger<EntityDamageEvent, PowerHurt, Double, Double> HURT = new Trigger<EntityDamageEvent, PowerHurt, Double, Double>(EntityDamageEvent.class, PowerHurt.class, Double.class, Double.class, "HURT") {
        @Override
        public Double def(Player player, ItemStack i, EntityDamageEvent event) {
            return event.getDamage();
        }

        @Override
        public Double next(Double a, PowerResult<Double> b) {
            return b.isOK() ? Math.min(a, b.data()) : a;
        }

        @Override
        public PowerResult<Double> warpResult(PowerResult<Void> overrideResult, PowerHurt power, Player player, ItemStack i, EntityDamageEvent event) {
            return overrideResult.with(event.getDamage());
        }

        @Override
        public PowerResult<Double> run(PowerHurt power, Player player, ItemStack i, EntityDamageEvent event) {
            return power.hurt(player, i, event.getDamage(), event);
        }
    };

    public static final Trigger<PlayerInteractEvent, PowerLeftClick, Void, Void> LEFT_CLICK = new Trigger<PlayerInteractEvent, PowerLeftClick, Void, Void>(PlayerInteractEvent.class, PowerLeftClick.class, Void.class, Void.class, "LEFT_CLICK") {
        @Override
        public PowerResult<Void> run(PowerLeftClick power, Player player, ItemStack i, PlayerInteractEvent event) {
            return power.leftClick(player, i, event);
        }
    };

    public static final Trigger<PlayerInteractEvent, PowerRightClick, Void, Void> RIGHT_CLICK = new Trigger<PlayerInteractEvent, PowerRightClick, Void, Void>(PlayerInteractEvent.class, PowerRightClick.class, Void.class, Void.class, "RIGHT_CLICK") {
        @Override
        public PowerResult<Void> run(PowerRightClick power, Player player, ItemStack i, PlayerInteractEvent event) {
            return power.rightClick(player, i, event);
        }
    };

    public static final Trigger<PlayerInteractEvent, PowerOffhandClick, Void, Void> OFFHAND_CLICK = new Trigger<PlayerInteractEvent, PowerOffhandClick, Void, Void>(PlayerInteractEvent.class, PowerOffhandClick.class, Void.class, Void.class, "OFFHAND_CLICK") {
        @Override
        public PowerResult<Void> run(PowerOffhandClick power, Player player, ItemStack i, PlayerInteractEvent event) {
            return power.offhandClick(player, i, event);
        }
    };

    public static final Trigger<PlayerToggleSneakEvent, PowerSneak, Void, Void> SNEAK = new Trigger<PlayerToggleSneakEvent, PowerSneak, Void, Void>(PlayerToggleSneakEvent.class, PowerSneak.class, Void.class, Void.class, "SNEAK") {
        @Override
        public PowerResult<Void> run(PowerSneak power, Player player, ItemStack i, PlayerToggleSneakEvent event) {
            return power.sneak(player, i, event);
        }
    };

    public static final Trigger<PlayerToggleSprintEvent, PowerSprint, Void, Void> SPRINT = new Trigger<PlayerToggleSprintEvent, PowerSprint, Void, Void>(PlayerToggleSprintEvent.class, PowerSprint.class, Void.class, Void.class, "SPRINT") {
        @Override
        public PowerResult<Void> run(PowerSprint power, Player player, ItemStack i, PlayerToggleSprintEvent event) {
            return power.sprint(player, i, event);
        }
    };

    public static final Trigger<PlayerSwapHandItemsEvent, PowerMainhandItem, Boolean, Boolean> SWAP_TO_OFFHAND = new Trigger<PlayerSwapHandItemsEvent, PowerMainhandItem, Boolean, Boolean>(PlayerSwapHandItemsEvent.class, PowerMainhandItem.class, Boolean.class, Boolean.class, "SWAP_TO_OFFHAND") {
        @Override
        public Boolean def(Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
            return true;
        }

        @Override
        public Boolean next(Boolean a, PowerResult<Boolean> b) {
            return b.isOK() ? b.data() && a : a;
        }

        @Override
        public PowerResult<Boolean> warpResult(PowerResult<Void> overrideResult, PowerMainhandItem power, Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
            return overrideResult.with(true);
        }

        @Override
        public PowerResult<Boolean> run(PowerMainhandItem power, Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
            return power.swapToOffhand(player, i, event);
        }
    };

    public static final Trigger<PlayerSwapHandItemsEvent, PowerOffhandItem, Boolean, Boolean> SWAP_TO_MAINHAND = new Trigger<PlayerSwapHandItemsEvent, PowerOffhandItem, Boolean, Boolean>(PlayerSwapHandItemsEvent.class, PowerOffhandItem.class, Boolean.class, Boolean.class, "SWAP_TO_MAINHAND") {
        @Override
        public Boolean def(Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
            return true;
        }

        @Override
        public Boolean next(Boolean a, PowerResult<Boolean> b) {
            return b.isOK() ? b.data() && a : a;
        }

        @Override
        public PowerResult<Boolean> warpResult(PowerResult<Void> overrideResult, PowerOffhandItem power, Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
            return overrideResult.with(true);
        }

        @Override
        public PowerResult<Boolean> run(PowerOffhandItem power, Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
            return power.swapToMainhand(player, i, event);
        }
    };

    public static final Trigger<InventoryClickEvent, PowerMainhandItem, Boolean, Boolean> PLACE_OFF_HAND = new Trigger<InventoryClickEvent, PowerMainhandItem, Boolean, Boolean>(InventoryClickEvent.class, PowerMainhandItem.class, Boolean.class, Boolean.class, "PLACE_OFF_HAND") {
        @Override
        public Boolean def(Player player, ItemStack i, InventoryClickEvent event) {
            return true;
        }

        @Override
        public Boolean next(Boolean a, PowerResult<Boolean> b) {
            return b.isOK() ? b.data() && a : a;
        }

        @Override
        public PowerResult<Boolean> warpResult(PowerResult<Void> overrideResult, PowerMainhandItem power, Player player, ItemStack i, InventoryClickEvent event) {
            return overrideResult.with(true);
        }

        @Override
        public PowerResult<Boolean> run(PowerMainhandItem power, Player player, ItemStack i, InventoryClickEvent event) {
            return power.placeOffhand(player, i, event);
        }
    };

    public static final Trigger<InventoryClickEvent, PowerOffhandItem, Boolean, Boolean> PICKUP_OFF_HAND = new Trigger<InventoryClickEvent, PowerOffhandItem, Boolean, Boolean>(InventoryClickEvent.class, PowerOffhandItem.class, Boolean.class, Boolean.class, "PICKUP_OFF_HAND") {
        @Override
        public Boolean def(Player player, ItemStack i, InventoryClickEvent event) {
            return true;
        }

        @Override
        public Boolean next(Boolean a, PowerResult<Boolean> b) {
            return b.isOK() ? b.data() && a : a;
        }

        @Override
        public PowerResult<Boolean> warpResult(PowerResult<Void> overrideResult, PowerOffhandItem power, Player player, ItemStack i, InventoryClickEvent event) {
            return overrideResult.with(true);
        }

        @Override
        public PowerResult<Boolean> run(PowerOffhandItem power, Player player, ItemStack i, InventoryClickEvent event) {
            return power.pickupOffhand(player, i, event);
        }
    };

    public static final Trigger<Event, PowerSneaking, Void, Void> SNEAKING = new Trigger<Event, PowerSneaking, Void, Void>(Event.class, PowerSneaking.class, Void.class, Void.class, "SNEAKING") {
        @Override
        public PowerResult<Void> run(PowerSneaking power, Player player, ItemStack i, Event event) {
            return power.sneaking(player, i);
        }
    };

    public static final Trigger<ProjectileLaunchEvent, PowerProjectileLaunch, Void, Void> LAUNCH_PROJECTILE = new Trigger<ProjectileLaunchEvent, PowerProjectileLaunch, Void, Void>(ProjectileLaunchEvent.class, PowerProjectileLaunch.class, Void.class, Void.class, "PROJECTILE_LAUNCH") {
        @Override
        public PowerResult<Void> run(PowerProjectileLaunch power, Player player, ItemStack i, ProjectileLaunchEvent event) {
            return power.projectileLaunch(player, i, event);
        }
    };

    public static final Trigger<Event, PowerTick, Void, Void> TICK = new Trigger<Event, PowerTick, Void, Void>(Event.class, PowerTick.class, Void.class, Void.class, "TICK") {
        @Override
        public PowerResult<Void> run(PowerTick power, Player player, ItemStack i, Event event) {
            return power.tick(player, i);
        }
    };

    public static final Trigger<Event, PowerAttachment, Void, Void> ATTACHMENT = new Trigger<Event, PowerAttachment, Void, Void>(Event.class, PowerAttachment.class, Void.class, Void.class, "ATTACHMENT") {
        @Override
        public PowerResult<Void> run(PowerAttachment power, Player player, ItemStack i, Event event) {
            throw new IllegalStateException();
        }

        @Override
        public PowerResult<Void> run(PowerAttachment power, Player player, ItemStack i, Event event, Object data) {
            ItemStack originalItemstack = (ItemStack) ((Pair) data).getKey();
            Event originalEvent = (Event) ((Pair) data).getValue();
            return power.attachment(player, i, ItemManager.toRPGItem(originalItemstack).orElseThrow(IllegalArgumentException::new), originalEvent, originalItemstack);
        }
    };

    public static final Trigger<Event, PowerLocation, Void, Void> LOCATION = new Trigger<Event, PowerLocation, Void, Void>(Event.class, PowerLocation.class, Void.class, Void.class, "LOCATION") {
        @Override
        public PowerResult<Void> run(PowerLocation power, Player player, ItemStack i, Event event) {
            throw new IllegalStateException();
        }

        @Override
        public PowerResult<Void> run(PowerLocation power, Player player, ItemStack i, Event event, Object data) {
            return power.fire(player, i, (Location) data);
        }
    };

    public static final Trigger<Event, PowerLivingEntity, Void, Void> LIVINGENTITY = new Trigger<Event, PowerLivingEntity, Void, Void>(Event.class, PowerLivingEntity.class, Void.class, Void.class, "LIVINGENTITY") {
        @Override
        public PowerResult<Void> run(PowerLivingEntity power, Player player, ItemStack i, Event event) {
            throw new IllegalStateException();
        }

        @Override
        public PowerResult<Void> run(PowerLivingEntity power, Player player, ItemStack i, Event event, Object data) {
            return power.fire(player, i, (LivingEntity) ((Pair) data).getKey(), (Double) ((Pair) data).getValue());
        }
    };

    private final Class<TEvent> eventClass;
    private final Class<TResult> resultClass;
    private final Class<TPower> powerClass;
    private final Class<TReturn> returnClass;
    private final String name;

    private Trigger(Class<TEvent> eventClass, Class<TPower> powerClass, Class<TResult> resultClass, Class<TReturn> returnClass, String name) {
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