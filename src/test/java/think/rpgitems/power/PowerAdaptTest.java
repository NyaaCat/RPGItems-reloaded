package think.rpgitems.power;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import think.rpgitems.RPGItems;
import think.rpgitems.TestProxies;
import think.rpgitems.power.trigger.Trigger;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Casting the powers of an item to the power interface of the trigger being dispatched (RPGItem#getPower).
 * The trigger here stands in for an extension trigger such as MINION_ATTACK: its power interface is only
 * reachable from PowerLivingEntity through an adapter the extension registers.
 */
class PowerAdaptTest {
    /** The extension trigger's power interface, e.g. PowerMinionAttack. */
    public interface PowerTestAttack extends Pimpl {
        PowerResult<Void> onTestAttack(Player player);
    }

    /** A power with a living-entity form, like rpgitems:beam. */
    @Meta(generalInterface = PowerLivingEntity.class, implClass = Pimpl.class)
    abstract static class LivingPower implements Power {
    }

    /** An extension power that implements the trigger's interface itself. */
    @Meta(generalInterface = PowerLivingEntity.class, implClass = Pimpl.class)
    abstract static class NativePower implements Power {
    }

    /** A power with no living-entity form, like rpgitems:potiontick. */
    @Meta(generalInterface = PowerTick.class, implClass = Pimpl.class)
    abstract static class TickPower implements Power {
    }

    record LivingImpl(Power getPower) implements PowerLivingEntity {
        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, Double value) {
            return PowerResult.ok();
        }
    }

    record TickImpl(Power getPower) implements PowerTick {
        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            return PowerResult.ok();
        }
    }

    record NativeImpl(Power getPower) implements PowerTestAttack {
        @Override
        public PowerResult<Void> onTestAttack(Player player) {
            return PowerResult.ok();
        }
    }

    private static final NamespacedKey LIVING = new NamespacedKey("rpgitems", "test_living");
    private static final NamespacedKey TICK = new NamespacedKey("rpgitems", "test_tick");
    private static final NamespacedKey NATIVE = new NamespacedKey("rgminion", "test_native");
    private static Trigger<Event, PowerTestAttack, Void, Void> attack;

    private final List<LogRecord> logged = new ArrayList<>();
    private final Handler capture = new Handler() {
        @Override
        public void publish(LogRecord record) {
            logged.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeAll
    static void trigger() {
        if (RPGItems.logger == null) RPGItems.logger = Logger.getLogger("RPGItems-test");
        if (Trigger.get("PROBE_ATTACK") == null) {
            Trigger.register(new Trigger<>("PROBE_ATTACK", Event.class, PowerTestAttack.class, Void.class, Void.class) {
                @Override
                public PowerResult<Void> run(PowerTestAttack power, Player player, ItemStack i, Event event) {
                    return power.onTestAttack(player);
                }
            });
        }
        @SuppressWarnings("unchecked")
        Trigger<Event, PowerTestAttack, Void, Void> t = Trigger.get("PROBE_ATTACK");
        attack = t;
    }

    @BeforeEach
    void registry() throws ReflectiveOperationException {
        // What RPGItems#loadPowers does first (cold start and /rpgitem reload): forget every registration.
        PowerManager.clear();
        registerPower(LIVING, LivingPower.class);
        registerPower(TICK, TickPower.class);
        registerPower(NATIVE, NativePower.class);
        RPGItems.logger.addHandler(capture);
    }

    @AfterEach
    void detach() {
        RPGItems.logger.removeHandler(capture);
        PowerManager.clear();
    }

    /** PowerManager#registerPower without instantiating the power (BasePropertyHolder needs a live plugin). */
    @SuppressWarnings("unchecked")
    private static void registerPower(NamespacedKey key, Class<? extends Power> cls) throws ReflectiveOperationException {
        PowerManager.registerMetas(cls);
        Field all = PowerManager.class.getDeclaredField("all");
        all.setAccessible(true);
        ((Map<NamespacedKey, Class<?>>) all.get(null)).put(key, cls);
    }

    private static Power power(NamespacedKey key, String id) {
        return TestProxies.of(Power.class, id, Map.of(
                "getNamespacedKey", a -> key,
                "getPlaceholderId", a -> id));
    }

    private static void registerAdapter() {
        PowerManager.registerAdapter(PowerLivingEntity.class, PowerTestAttack.class, living -> new PowerTestAttack() {
            @Override
            public PowerResult<Void> onTestAttack(Player player) {
                return living.fire(player, null, null, 0d);
            }

            @Override
            public Power getPower() {
                return living.getPower();
            }
        });
    }

    private List<LogRecord> warnings() {
        return logged.stream().filter(r -> r.getLevel() == Level.WARNING).toList();
    }

    @Test
    void adapterTurnsLivingEntityPowerIntoTriggerPower() {
        registerAdapter();
        Power beam = power(LIVING, "beam-0");
        List<PowerTestAttack> cast = PowerManager.castForTrigger("d21", List.of(new LivingImpl(beam)), attack);
        assertEquals(1, cast.size());
        assertSame(beam, cast.get(0).getPower());
        assertTrue(cast.get(0).onTestAttack(null).isOK());
        assertTrue(warnings().isEmpty(), "nothing to warn about: " + warnings());
    }

    @Test
    void missingAdapterIsReportedWithPowerAndTarget() {
        // the state after /rpgitem reload when the extension did not register its adapter again
        Pimpl beam = new LivingImpl(power(LIVING, "beam-0"));
        ClassCastException e = assertThrows(ClassCastException.class, () -> beam.cast(PowerTestAttack.class));
        assertNotNull(e.getMessage(), "a bare ClassCastException does not say which power or interface");
        assertTrue(e.getMessage().contains("rpgitems:test_living") && e.getMessage().contains("PowerTestAttack") && e.getMessage().contains("PowerLivingEntity"),
                e.getMessage());
    }

    @Test
    void unadaptablePowerDoesNotStopTheOtherPowers() {
        registerAdapter();
        Power tick = power(TICK, "bad-0");
        Power beam = power(LIVING, "beam-0");
        Power nativePower = power(NATIVE, "native-0");
        List<Pimpl> impls = List.of(new TickImpl(tick), new LivingImpl(beam), new NativeImpl(nativePower));

        List<PowerTestAttack> cast = PowerManager.castForTrigger("hmix", impls, attack);

        assertEquals(List.of(beam, nativePower), cast.stream().map(Pimpl::getPower).toList(), "the powers that can run still run, in order");
    }

    @Test
    void unadaptablePowerIsWarnedOnceNamingItemPowerAndTrigger() {
        Power beam = power(LIVING, "beam-0");
        Power nativePower = power(NATIVE, "native-0");
        for (int attackNo = 0; attackNo < 50; attackNo++) {
            List<PowerTestAttack> cast = PowerManager.castForTrigger("vrs2_weapon_d21", List.of(new LivingImpl(beam), new NativeImpl(nativePower)), attack);
            assertEquals(List.of(nativePower), cast.stream().map(Pimpl::getPower).toList());
        }
        List<LogRecord> w = warnings();
        assertEquals(1, w.size(), "one warning for 50 attacks, not one per attack: " + w.stream().map(LogRecord::getMessage).toList());
        String msg = w.get(0).getMessage();
        assertTrue(msg.contains("vrs2_weapon_d21") && msg.contains("rpgitems:test_living") && msg.contains("beam-0") && msg.contains("PROBE_ATTACK"), msg);
    }

    @Test
    void eachItemPowerAndTriggerIsWarnedSeparately() {
        Power beam = power(LIVING, "beam-0");
        Power beam2 = power(LIVING, "beam-1");
        PowerManager.castForTrigger("a", List.of(new LivingImpl(beam)), attack);
        PowerManager.castForTrigger("a", List.of(new LivingImpl(beam2)), attack);
        PowerManager.castForTrigger("b", List.of(new LivingImpl(beam)), attack);
        PowerManager.castForTrigger("a", List.of(new LivingImpl(beam)), attack);
        assertEquals(3, warnings().size(), warnings().stream().map(LogRecord::getMessage).toList().toString());
    }

    @Test
    void reloadWarnsAgain() {
        Power beam = power(LIVING, "beam-0");
        PowerManager.castForTrigger("d21", List.of(new LivingImpl(beam)), attack);
        PowerManager.clear();
        try {
            registerPower(LIVING, LivingPower.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        PowerManager.castForTrigger("d21", List.of(new LivingImpl(beam)), attack);
        assertEquals(2, warnings().size(), "a reload starts over, so a still-broken setup is reported once more");
    }

    @Test
    void failuresInsideAnAdapterAreNotSwallowed() {
        PowerManager.registerAdapter(PowerLivingEntity.class, PowerTestAttack.class, living -> {
            throw new IllegalStateException("adapter bug");
        });
        Pimpl beam = new LivingImpl(power(LIVING, "beam-0"));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> PowerManager.castForTrigger("d21", List.of(beam), attack));
        assertEquals("adapter bug", e.getMessage());
    }
}
