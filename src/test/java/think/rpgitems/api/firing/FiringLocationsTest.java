package think.rpgitems.api.firing;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import think.rpgitems.TestProxies;
import think.rpgitems.power.impl.AOEDamage;
import think.rpgitems.power.impl.Attract;
import think.rpgitems.power.impl.Beam;
import think.rpgitems.power.impl.ProjectilePower;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FiringLocationsTest {
    private final World world = TestProxies.of(World.class, "world", Map.of());
    private final Player player = TestProxies.of(Player.class, "player", Map.of());
    private final FiringContext context = new FiringContext(player, player, null, null);

    private static Plugin plugin(String name, AtomicBoolean enabled) {
        return TestProxies.of(Plugin.class, name, Map.of("getName", a -> name, "isEnabled", a -> enabled.get()));
    }

    private static Plugin plugin(String name) {
        return plugin(name, new AtomicBoolean(true));
    }

    @AfterEach
    void cleanup() {
        FiringLocations.clear();
    }

    // ---- names and backward compatibility

    @Test
    void builtInNamesKeepTheirTextAndIdentity() {
        assertSame(FiringLocation.SELF, FiringLocation.of("SELF"));
        assertSame(FiringLocation.TARGET, FiringLocation.of("target"));
        assertEquals("SELF", FiringLocation.SELF.toString());
        assertEquals("TARGET", FiringLocation.TARGET.name());
        assertFalse(FiringLocation.SELF.isExtension());
        assertFalse(FiringLocation.TARGET.isExtension());
        assertTrue(FiringLocation.of("minion_eye").isExtension());
        assertEquals(FiringLocation.of("MINION_EYE"), FiringLocation.of(" minion_eye "));
    }

    @Test
    void invalidNamesAreRejected() {
        for (String bad : new String[]{"", " ", "1ABC", "A-B", "A B", "a:b", "X".repeat(33)}) {
            assertThrows(IllegalArgumentException.class, () -> FiringLocation.of(bad), bad);
        }
        assertThrows(IllegalArgumentException.class, () -> FiringLocation.of(null));
    }

    @Test
    void serializerRoundTripsOldValuesAndKeepsUnregisteredNames() {
        FiringLocationSerializer serializer = new FiringLocationSerializer();
        for (String stored : List.of("SELF", "TARGET")) {
            assertEquals(stored, serializer.get(serializer.set(stored).orElseThrow()));
        }
        // An item saved while an extension was installed must survive a load without it.
        FiringLocation unknown = serializer.set("MINION_TARGET").orElseThrow();
        assertFalse(FiringLocations.isKnown(unknown));
        assertEquals("MINION_TARGET", serializer.get(unknown));
        assertThrows(IllegalArgumentException.class, () -> serializer.set("not a name"));
    }

    @Test
    void theFourPowersShareTheNewPropertyType() throws Exception {
        for (Class<?> power : List.of(Beam.class, ProjectilePower.class, AOEDamage.class, Attract.class)) {
            var field = power.getField("firingLocation");
            assertEquals(FiringLocation.class, field.getType(), power.getSimpleName());
            assertEquals(FiringLocationSerializer.class, field.getAnnotation(think.rpgitems.power.Serializer.class).value());
            assertEquals(FiringLocationSerializer.class, field.getAnnotation(think.rpgitems.power.Deserializer.class).value());
            assertNotNull(field.getAnnotation(think.rpgitems.power.Property.class), power.getSimpleName());
        }
    }

    // ---- registration

    @Test
    void registrationIsCaseInsensitiveAndListed() {
        Plugin ext = plugin("Ext");
        assertEquals(List.of("SELF", "TARGET"), FiringLocations.names());
        FiringLocations.register(ext, "zeta", c -> Optional.empty());
        FiringLocations.register(ext, "Alpha", c -> Optional.empty());
        assertEquals(List.of("SELF", "TARGET", "ALPHA", "ZETA"), FiringLocations.names());
        assertTrue(FiringLocations.isRegistered("alpha"));
        assertTrue(FiringLocations.isKnown(FiringLocation.of("ZETA")));
        assertTrue(FiringLocations.isKnown(FiringLocation.SELF));
        assertFalse(FiringLocations.isKnown(FiringLocation.of("OTHER")));
    }

    @Test
    void builtInsCannotBeReplacedAndNamesCannotBeStolen() {
        Plugin ext = plugin("Ext");
        Plugin other = plugin("Other");
        assertThrows(IllegalArgumentException.class, () -> FiringLocations.register(ext, "SELF", c -> Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> FiringLocations.register(ext, "target", c -> Optional.empty()));
        FiringLocations.register(ext, "MINION", c -> Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> FiringLocations.register(other, "minion", c -> Optional.empty()));
        assertFalse(FiringLocations.unregister(other, "MINION"), "only the owner can unregister");
        assertTrue(FiringLocations.isRegistered("MINION"));
    }

    @Test
    void samePluginMayRegisterAgainAfterAReload() {
        // /rpgitem reload re-fires registration in a still-enabled extension; a fresh plugin instance with the
        // same name (ext/ layout) must also be able to take its own names back.
        FiringPoint first = FiringPoint.origin(new Location(world, 1, 0, 0));
        FiringPoint second = FiringPoint.origin(new Location(world, 2, 0, 0));
        FiringLocations.register(plugin("Ext"), "MINION", c -> Optional.of(() -> Optional.of(first)));
        FiringLocations.register(plugin("Ext"), "MINION", c -> Optional.of(() -> Optional.of(second)));
        assertEquals(2, FiringLocations.begin(FiringLocation.of("MINION"), context).orElseThrow().first().location().getX());
    }

    @Test
    void unregisterAllOnlyRemovesThatPluginsNames() {
        Plugin ext = plugin("Ext");
        Plugin other = plugin("Other");
        FiringLocations.register(ext, "A", c -> Optional.empty());
        FiringLocations.register(ext, "B", c -> Optional.empty());
        FiringLocations.register(other, "C", c -> Optional.empty());
        assertEquals(2, FiringLocations.unregisterAll(ext));
        assertEquals(List.of("SELF", "TARGET", "C"), FiringLocations.names());
        assertEquals(0, FiringLocations.unregisterAll(ext));
    }

    // ---- resolution

    @Test
    void builtInsAreNeverSentToProviders() {
        assertThrows(IllegalArgumentException.class, () -> FiringLocations.bind(FiringLocation.SELF, context));
        assertThrows(IllegalArgumentException.class, () -> FiringLocations.bind(FiringLocation.TARGET, context));
    }

    @Test
    void unregisteredNameDoesNotFire() {
        assertTrue(FiringLocations.bind(FiringLocation.of("MINION"), context).isEmpty());
        assertTrue(FiringLocations.begin(FiringLocation.of("MINION"), context).isEmpty());
    }

    @Test
    void providerOfADisabledPluginIsNotCalled() {
        AtomicBoolean enabled = new AtomicBoolean(true);
        AtomicInteger calls = new AtomicInteger();
        FiringLocations.register(plugin("Ext", enabled), "MINION", c -> {
            calls.incrementAndGet();
            return Optional.of(() -> Optional.of(FiringPoint.origin(new Location(world, 0, 0, 0))));
        });
        assertTrue(FiringLocations.begin(FiringLocation.of("MINION"), context).isPresent());
        enabled.set(false);
        assertTrue(FiringLocations.begin(FiringLocation.of("MINION"), context).isEmpty());
        assertEquals(1, calls.get());
    }

    @Test
    void providerReceivesTheExplicitContext() {
        FiringContext[] seen = new FiringContext[1];
        FiringLocations.register(plugin("Ext"), "MINION", c -> {
            seen[0] = c;
            return Optional.empty();
        });
        assertTrue(FiringLocations.begin(FiringLocation.of("minion"), context).isEmpty());
        assertSame(context, seen[0]);
    }

    @Test
    void failingOrNullReturningProvidersNeverPropagate() {
        Plugin ext = plugin("Ext");
        FiringLocations.register(ext, "THROWS", c -> {
            throw new IllegalStateException("boom");
        });
        FiringLocations.register(ext, "NULL", c -> null);
        FiringLocations.register(ext, "NULL_POINT", c -> Optional.of(() -> null));
        FiringLocations.register(ext, "THROWS_LATER", c -> Optional.of(() -> {
            throw new IllegalStateException("boom");
        }));
        for (String name : List.of("THROWS", "NULL", "NULL_POINT", "THROWS_LATER")) {
            assertTrue(FiringLocations.begin(FiringLocation.of(name), context).isEmpty(), name);
        }
    }

    @Test
    void anchorIsAskedPerShotUnlessFrozenAndCanExpire() {
        AtomicInteger shots = new AtomicInteger();
        AtomicBoolean alive = new AtomicBoolean(true);
        FiringLocations.register(plugin("Ext"), "MINION", c -> Optional.of(() -> alive.get()
                ? Optional.of(FiringPoint.origin(new Location(world, shots.incrementAndGet(), 64, 0)))
                : Optional.empty()));
        FiringLocations.BoundFiring bound = FiringLocations.begin(FiringLocation.of("MINION"), context).orElseThrow();
        assertEquals(1, bound.first().location().getX());
        // castOff: later bursts reuse the activation-time point and do not consult the anchor
        assertEquals(1, bound.next(true).orElseThrow().location().getX());
        assertEquals(1, shots.get());
        // otherwise every burst re-locates
        assertEquals(2, bound.next(false).orElseThrow().location().getX());
        assertEquals(3, bound.next(false).orElseThrow().location().getX());
        // the minion died between bursts: the remaining shots are skipped
        alive.set(false);
        assertTrue(bound.next(false).isEmpty());
    }

    @Test
    void twoActivationsNeverShareAnAnchor() {
        // Each bind captures its own state; a second player's activation must not change the first one's.
        AtomicInteger binds = new AtomicInteger();
        FiringLocations.register(plugin("Ext"), "MINION", c -> {
            int id = binds.incrementAndGet();
            return Optional.of(() -> Optional.of(FiringPoint.origin(new Location(world, id * 100, 0, 0))));
        });
        FiringLocations.BoundFiring a = FiringLocations.begin(FiringLocation.of("MINION"), context).orElseThrow();
        FiringLocations.BoundFiring b = FiringLocations.begin(FiringLocation.of("MINION"), context).orElseThrow();
        assertEquals(100, a.next(false).orElseThrow().location().getX());
        assertEquals(200, b.next(false).orElseThrow().location().getX());
        assertEquals(100, a.next(false).orElseThrow().location().getX());
    }

    @Test
    void firingPointIsDefensivelyCopied() {
        Location location = new Location(world, 1, 2, 3, 90, 10);
        Vector normal = new Vector(1, 0, 0);
        FiringPoint point = new FiringPoint(FiringPoint.Kind.CAST, location, null, normal);
        location.setX(50);
        normal.setX(9);
        point.location().setY(99);
        assertEquals(1, point.location().getX());
        assertEquals(2, point.location().getY());
        assertEquals(1, point.normal().getX());
        assertEquals(new Vector(0, 1, 0), FiringPoint.cast(new Location(world, 0, 0, 0), null).normal());
        assertEquals(FiringPoint.Kind.ORIGIN, FiringPoint.origin(new Location(world, 0, 0, 0)).kind());
        assertThrows(NullPointerException.class, () -> FiringPoint.origin(new Location(null, 0, 0, 0)));
    }

    @Test
    void registrationIsSafeFromManyThreads() throws Exception {
        Plugin ext = plugin("Ext");
        Thread[] threads = new Thread[8];
        for (int t = 0; t < threads.length; t++) {
            int id = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 200; i++) {
                    FiringLocations.register(ext, "T" + id + "_" + (i % 20), c -> Optional.empty());
                    FiringLocations.names();
                    FiringLocations.isRegistered("T" + id + "_" + (i % 20));
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) thread.join();
        assertEquals(2 + 8 * 20, FiringLocations.names().size());
        assertEquals(160, FiringLocations.unregisterAll(ext));
    }
}
