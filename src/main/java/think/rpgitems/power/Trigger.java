package think.rpgitems.power;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Trigger<TEvent extends Event, TPower extends Power, TResult, TReturn> {

    private static boolean acceptingNew = true;

    private static final Map<String, Trigger> registry = new HashMap<>();

    public static Stream<Trigger> fromInterface(Class<? extends Power> power) {
        return registry.values().stream().filter(t -> t.powerClass.equals(power));
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
        return name.map(Trigger::valueOf);
    }

    public static void register(String name, Trigger trigger) {
        if (registry.containsKey(name)) {
            throw new IllegalArgumentException("Cannot set already-set trigger");
        } else if (!isAcceptingRegistrations()) {
            throw new IllegalStateException("No longer accepting new triggers (can only be done when loading)");
        }
        registry.put(name, trigger);
    }

    public static boolean isAcceptingRegistrations() {
        return acceptingNew;
    }

    public static void stopAcceptingRegistrations() {
        acceptingNew = false;
    }

    public static final Trigger<EntityDamageByEntityEvent, PowerHit, Double, Double> HIT = new Trigger<EntityDamageByEntityEvent, PowerHit, Double, Double>("HIT", EntityDamageByEntityEvent.class, PowerHit.class, Double.class, Double.class) {
        @Override
        public Double def(Player player, ItemStack i, EntityDamageByEntityEvent event) {
            return event.getDamage();
        }

        @Override
        public Double next(Double a, PowerResult<Double> b) {
            return b.isOK() ? Math.max(a, b.getData()) : a;
        }

        @Override
        public PowerResult<Double> run(PowerHit power, Player player, ItemStack i, EntityDamageByEntityEvent event) {
            return power.hit(player, i, (LivingEntity) event.getEntity(), event.getDamage(), event);
        }
    };

    public static final Trigger<ProjectileHitEvent, PowerProjectileHit, Void, Void> PROJECTILE_HIT = new Trigger<ProjectileHitEvent, PowerProjectileHit, Void, Void>("PROJECTILE_HIT", ProjectileHitEvent.class, PowerProjectileHit.class, Void.class, Void.class) {
        @Override
        public Void def(Player player, ItemStack i, ProjectileHitEvent event) {
            return null;
        }

        @Override
        public Void next(Void a, PowerResult<Void> b) {
            return null;
        }

        @Override
        public PowerResult<Void> run(PowerProjectileHit power, Player player, ItemStack i, ProjectileHitEvent event) {
            return power.projectileHit(player, i, event);
        }
    };

    public static final Trigger<EntityDamageEvent, PowerHitTaken, Double, Double> HIT_TAKEN = new Trigger<EntityDamageEvent, PowerHitTaken, Double, Double>("HIT_TAKEN", EntityDamageEvent.class, PowerHitTaken.class, Double.class, Double.class) {
        @Override
        public Double def(Player player, ItemStack i, EntityDamageEvent event) {
            return event.getDamage();
        }

        @Override
        public Double next(Double a, PowerResult<Double> b) {
            return b.isOK() ? Math.min(a, b.getData()) : a;
        }

        @Override
        public PowerResult<Double> run(PowerHitTaken power, Player player, ItemStack i, EntityDamageEvent event) {
            return power.takeHit(player, i, event.getDamage(), event);
        }
    };

    public static final Trigger<EntityDamageEvent, PowerHurt, Void, Void> HURT = new Trigger<EntityDamageEvent, PowerHurt, Void, Void>("HURT", EntityDamageEvent.class, PowerHurt.class, Void.class, Void.class) {
        @Override
        public Void def(Player player, ItemStack i, EntityDamageEvent event) {
            return null;
        }

        @Override
        public Void next(Void a, PowerResult<Void> b) {
            return null;
        }

        @Override
        public PowerResult<Void> run(PowerHurt power, Player player, ItemStack i, EntityDamageEvent event) {
            return power.hurt(player, i, event);
        }
    };

    public static final Trigger<PlayerInteractEvent, PowerLeftClick, Void, Void> LEFT_CLICK = new Trigger<PlayerInteractEvent, PowerLeftClick, Void, Void>("LEFT_CLICK", PlayerInteractEvent.class, PowerLeftClick.class, Void.class, Void.class) {
        @Override
        public Void def(Player player, ItemStack i, PlayerInteractEvent event) {
            return null;
        }

        @Override
        public Void next(Void a, PowerResult<Void> b) {
            return null;
        }

        @Override
        public PowerResult<Void> run(PowerLeftClick power, Player player, ItemStack i, PlayerInteractEvent event) {
            return power.leftClick(player, i, event);
        }
    };

    public static final Trigger<PlayerInteractEvent, PowerRightClick, Void, Void> RIGHT_CLICK = new Trigger<PlayerInteractEvent, PowerRightClick, Void, Void>("RIGHT_CLICK", PlayerInteractEvent.class, PowerRightClick.class, Void.class, Void.class) {
        @Override
        public Void def(Player player, ItemStack i, PlayerInteractEvent event) {
            return null;
        }

        @Override
        public Void next(Void a, PowerResult<Void> b) {
            return null;
        }

        @Override
        public PowerResult<Void> run(PowerRightClick power, Player player, ItemStack i, PlayerInteractEvent event) {
            return power.rightClick(player, i, event);
        }
    };

    public static final Trigger<PlayerInteractEvent, PowerOffhandClick, Void, Void> OFFHAND_CLICK = new Trigger<PlayerInteractEvent, PowerOffhandClick, Void, Void>("OFFHAND_CLICK", PlayerInteractEvent.class, PowerOffhandClick.class, Void.class, Void.class) {
        @Override
        public Void def(Player player, ItemStack i, PlayerInteractEvent event) {
            return null;
        }

        @Override
        public Void next(Void a, PowerResult<Void> b) {
            return null;
        }

        @Override
        public PowerResult<Void> run(PowerOffhandClick power, Player player, ItemStack i, PlayerInteractEvent event) {
            return power.offhandClick(player, i, event);
        }
    };

    public static final Trigger<PlayerToggleSneakEvent, PowerSneak, Void, Void> SNEAK = new Trigger<PlayerToggleSneakEvent, PowerSneak, Void, Void>("SNEAK", PlayerToggleSneakEvent.class, PowerSneak.class, Void.class, Void.class) {
        @Override
        public Void def(Player player, ItemStack i, PlayerToggleSneakEvent event) {
            return null;
        }

        @Override
        public Void next(Void a, PowerResult<Void> b) {
            return null;
        }

        @Override
        public PowerResult<Void> run(PowerSneak power, Player player, ItemStack i, PlayerToggleSneakEvent event) {
            return power.sneak(player, i, event);
        }
    };

    public static final Trigger<PlayerToggleSprintEvent, PowerSprint, Void, Void> SPRINT = new Trigger<PlayerToggleSprintEvent, PowerSprint, Void, Void>("SPRINT", PlayerToggleSprintEvent.class, PowerSprint.class, Void.class, Void.class) {
        @Override
        public Void def(Player player, ItemStack i, PlayerToggleSprintEvent event) {
            return null;
        }

        @Override
        public Void next(Void a, PowerResult<Void> b) {
            return null;
        }

        @Override
        public PowerResult<Void> run(PowerSprint power, Player player, ItemStack i, PlayerToggleSprintEvent event) {
            return power.sprint(player, i, event);
        }
    };

    public static final Trigger<PlayerSwapHandItemsEvent, PowerMainhandItem, Boolean, Boolean> SWAP_TO_OFFHAND = new Trigger<PlayerSwapHandItemsEvent, PowerMainhandItem, Boolean, Boolean>("SWAP_TO_OFFHAND", PlayerSwapHandItemsEvent.class, PowerMainhandItem.class, Boolean.class, Boolean.class) {
        @Override
        public Boolean def(Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
            return true;
        }

        @Override
        public Boolean next(Boolean a, PowerResult<Boolean> b) {
            return b.isOK() ? b.getData() && a : a;
        }

        @Override
        public PowerResult<Boolean> run(PowerMainhandItem power, Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
            return power.swapToOffhand(player, i, event);
        }
    };

    public static final Trigger<PlayerSwapHandItemsEvent, PowerOffhandItem, Boolean, Boolean> SWAP_TO_MAINHAND = new Trigger<PlayerSwapHandItemsEvent, PowerOffhandItem, Boolean, Boolean>("SWAP_TO_MAINHAND", PlayerSwapHandItemsEvent.class, PowerOffhandItem.class, Boolean.class, Boolean.class) {
        @Override
        public Boolean def(Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
            return true;
        }

        @Override
        public Boolean next(Boolean a, PowerResult<Boolean> b) {
            return b.isOK() ? b.getData() && a : a;
        }

        @Override
        public PowerResult<Boolean> run(PowerOffhandItem power, Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
            return power.swapToMainhand(player, i, event);
        }
    };

    public static final Trigger<InventoryClickEvent, PowerMainhandItem, Boolean, Boolean> PLACE_OFF_HAND = new Trigger<InventoryClickEvent, PowerMainhandItem, Boolean, Boolean>("PLACE_OFF_HAND", InventoryClickEvent.class, PowerMainhandItem.class, Boolean.class, Boolean.class) {
        @Override
        public Boolean def(Player player, ItemStack i, InventoryClickEvent event) {
            return true;
        }

        @Override
        public Boolean next(Boolean a, PowerResult<Boolean> b) {
            return b.isOK() ? b.getData() && a : a;
        }

        @Override
        public PowerResult<Boolean> run(PowerMainhandItem power, Player player, ItemStack i, InventoryClickEvent event) {
            return power.placeOffhand(player, i, event);
        }
    };

    public static final Trigger<InventoryClickEvent, PowerOffhandItem, Boolean, Boolean> PICKUP_OFF_HAND = new Trigger<InventoryClickEvent, PowerOffhandItem, Boolean, Boolean>("PICKUP_OFF_HAND", InventoryClickEvent.class, PowerOffhandItem.class, Boolean.class, Boolean.class) {
        @Override
        public Boolean def(Player player, ItemStack i, InventoryClickEvent event) {
            return true;
        }

        @Override
        public Boolean next(Boolean a, PowerResult<Boolean> b) {
            return b.isOK() ? b.getData() && a : a;
        }

        @Override
        public PowerResult<Boolean> run(PowerOffhandItem power, Player player, ItemStack i, InventoryClickEvent event) {
            return power.pickupOffhand(player, i, event);
        }
    };

    public static final Trigger<Event, PowerTick, Void, Void> TICK = new Trigger<Event, PowerTick, Void, Void>("TICK", Event.class, PowerTick.class, Void.class, Void.class) {
        @Override
        public Void def(Player player, ItemStack i, Event event) {
            return null;
        }

        @Override
        public Void next(Void a, PowerResult<Void> b) {
            return null;
        }

        @Override
        public PowerResult<Void> run(PowerTick power, Player player, ItemStack i, Event event) {
            return power.tick(player, i);
        }
    };

    private final Class<TEvent> eventClass;
    private final Class<TResult> resultClass;
    private final Class<TPower> powerClass;
    private final Class<TReturn> returnClass;
    private final String name;

    private Trigger(String name, Class<TEvent> eventClass, Class<TPower> powerClass, Class<TResult> resultClass, Class<TReturn> returnClass) {
        this(eventClass, powerClass, resultClass, returnClass, name);
        register(name, this);
    }

    public Trigger(Class<TEvent> eventClass, Class<TPower> powerClass, Class<TResult> resultClass, Class<TReturn> returnClass, String name) {
        this.eventClass = eventClass;
        this.resultClass = resultClass;
        this.powerClass = powerClass;
        this.returnClass = returnClass;
        this.name = name;
    }

    public abstract TReturn def(Player player, ItemStack i, TEvent event);

    public abstract TReturn next(TReturn a, PowerResult<TResult> b);

    public abstract PowerResult<TResult> run(TPower power, Player player, ItemStack i, TEvent event);

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