package think.rpgitems.power;

import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import think.rpgitems.RPGItems;
import think.rpgitems.TestProxies;
import think.rpgitems.api.firing.FiringLocation;
import think.rpgitems.api.firing.FiringLocations;
import think.rpgitems.power.trigger.Trigger;

import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property value completion as used by {@code /rpgitem power|condition|marker add|prop}. The holder mimics an
 * extension power: its properties are package-private, which the old completer (Class#getField) could not see.
 */
class PropertyCompletionTest {
    enum Mode {ALPHA, ALPHA_TWO, BETA}

    @Meta(marker = true)
    @SuppressWarnings("unused")
    abstract static class Holder implements PropertyHolder {
        @Property
        Mode mode = Mode.ALPHA;
        @Property
        boolean flag;
        @Property
        Set<Mode> modes = new HashSet<>();
        @Property
        List<Mode> order = new ArrayList<>();
        @Property
        Set<Trigger> triggers = new HashSet<>();
        @Property
        FiringLocation firingLocation = FiringLocation.SELF;
        @Property
        @BooleanChoice(trueChoice = "on", falseChoice = "off")
        boolean choice;
        @Property
        int amount;
    }

    private static final NamespacedKey KEY = new NamespacedKey("rgminion", "holder");
    private static final CommandSender CONSOLE = TestProxies.of(CommandSender.class, "console", Map.of());
    private static final Plugin EXTENSION = TestProxies.of(Plugin.class, "RGMinion", Map.of("getName", a -> "RGMinion", "isEnabled", a -> true));

    @BeforeAll
    static void register() {
        if (RPGItems.logger == null) RPGItems.logger = Logger.getLogger("RPGItems-test");
        PowerManager.registerMetas(Holder.class);
        // a trigger whose canonical spelling is mixed case, like minion_OnAttack
        if (Trigger.get("test_OnThing") == null) {
            Trigger.register(new Trigger<>("test_OnThing", Event.class, Pimpl.class, Void.class, Void.class) {
                @Override
                public PowerResult<Void> run(Pimpl power, Player player, ItemStack i, Event event) {
                    return PowerResult.noop();
                }
            });
        }
        if (Trigger.get("TEST_OTHER") == null) {
            Trigger.register(new Trigger<>("TEST_OTHER", Event.class, Pimpl.class, Void.class, Void.class) {
                @Override
                public PowerResult<Void> run(Pimpl power, Player player, ItemStack i, Event event) {
                    return PowerResult.noop();
                }
            });
        }
    }

    @AfterEach
    void cleanProviders() {
        FiringLocations.unregisterAll(EXTENSION);
    }

    private static List<String> value(String property, String last) {
        return RPGCommandReceiver.resolvePropertyValueSuggestion(null, Holder.class, property, last, true);
    }

    @Test
    void nonPublicEnumPropertyIsCompleted() {
        assertEquals(List.of("mode:ALPHA", "mode:ALPHA_TWO", "mode:BETA"), value("mode", "mode:"));
    }

    @Test
    void valuesMatchIgnoringCaseButKeepCanonicalSpelling() {
        assertEquals(List.of("mode:BETA"), value("mode", "mode:b"));
        assertEquals(List.of("flag:true"), value("flag", "flag:T"));
        assertEquals(List.of("choice:on", "choice:off"), value("choice", "choice:"));
        assertEquals(List.of("choice:off"), value("choice", "choice:OF"));
    }

    @Test
    void booleanOffersBothValues() {
        assertEquals(List.of("flag:true", "flag:false"), value("flag", "flag:"));
    }

    @Test
    void numbersHaveNoValueSuggestions() {
        assertEquals(List.of(), value("amount", "amount:"));
    }

    @Test
    void setListCompletesOnlyTheLastEntryAndSkipsChosenOnes() {
        assertEquals(List.of("modes:ALPHA_TWO,BETA"), value("modes", "modes:ALPHA_TWO,b"));
        List<String> afterComma = value("modes", "modes:ALPHA,");
        assertEquals(List.of("modes:ALPHA,ALPHA_TWO", "modes:ALPHA,BETA"), afterComma);
    }

    @Test
    void completeEntryAlsoOffersLongerNamesAndContinuations() {
        List<String> s = value("modes", "modes:ALPHA");
        assertTrue(s.contains("modes:ALPHA"), s.toString());
        assertTrue(s.contains("modes:ALPHA_TWO"), s.toString());
        assertTrue(s.contains("modes:ALPHA,BETA"), s.toString());
        assertFalse(s.contains("modes:ALPHA,ALPHA"), s.toString());
        assertEquals(new HashSet<>(s).size(), s.size(), "no duplicates: " + s);
    }

    @Test
    void plainListMayRepeatEntries() {
        assertTrue(value("order", "order:ALPHA,").contains("order:ALPHA,ALPHA"));
    }

    @Test
    void triggerNamesMatchIgnoringCase() {
        assertEquals(List.of("triggers:test_OnThing"), value("triggers", "triggers:test_on"));
        assertEquals(List.of("triggers:test_OnThing"), value("triggers", "triggers:TEST_ON"));
        List<String> afterComma = value("triggers", "triggers:test_OnThing,TEST_");
        assertEquals(List.of("triggers:test_OnThing,TEST_OTHER"), afterComma);
    }

    @Test
    void firingLocationsFollowTheProviderRegistry() {
        assertEquals(List.of("firingLocation:SELF", "firingLocation:TARGET"), value("firingLocation", "firingLocation:"));
        FiringLocations.register(EXTENSION, "TEST_EYE", ctx -> Optional.empty());
        assertEquals(List.of("firingLocation:SELF", "firingLocation:TARGET", "firingLocation:TEST_EYE"), value("firingLocation", "firingLocation:"));
        assertEquals(List.of("firingLocation:TEST_EYE"), value("firingLocation", "firingLocation:test"));
        FiringLocations.unregisterAll(EXTENSION);
        assertEquals(List.of(), value("firingLocation", "firingLocation:test"));
    }

    /** What the add completer hands over: the arguments after item and power, parsed like NyaaCore does for TAB. */
    private static List<String> keysAndValues(String... rest) {
        String[] raw = new String[rest.length + 2];
        raw[0] = "probe";
        raw[1] = "rgminion:holder";
        System.arraycopy(rest, 0, raw, 2, rest.length);
        Arguments args = Arguments.parsePreserveLastBlank(raw, CONSOLE);
        args.next();
        args.next();
        return RPGCommandReceiver.resolveProperties(CONSOLE, null, Holder.class, KEY, raw[raw.length - 1], args, true);
    }

    @Test
    void trailingBlankOffersUnsetPropertyKeys() {
        List<String> keys = keysAndValues("");
        assertTrue(keys.containsAll(List.of("mode:", "flag:", "modes:", "firingLocation:")), keys.toString());
        List<String> afterFlag = keysAndValues("flag:true", "");
        assertFalse(afterFlag.contains("flag:"), afterFlag.toString());
        assertTrue(afterFlag.contains("mode:"), afterFlag.toString());
    }

    @Test
    void keyNameMatchesIgnoringCase() {
        assertEquals(List.of("firingLocation:"), keysAndValues("FIRING"));
        // a value typed after a wrongly cased key is completed with the canonical key
        assertEquals(List.of("mode:BETA"), keysAndValues("MODE:be"));
    }

    @Test
    void valueThroughTheAddCompleterEntry() {
        assertEquals(List.of("flag:true", "flag:false"), keysAndValues("flag:"));
    }

    @Test
    void registryKeysMatchTheirBareName() {
        List<NamespacedKey> keys = List.of(new NamespacedKey("rpgitems", "projectile"), new NamespacedKey("rgminion", "sentry"), new NamespacedKey("rgminion", "ceasefire"));
        List<String> bySentry = RPGCommandReceiver.suggestKeys(keys, "sen");
        assertEquals(1, bySentry.size(), bySentry.toString());
        assertTrue(bySentry.get(0).endsWith("sentry"));
        assertEquals(2, RPGCommandReceiver.suggestKeys(keys, "RGMINION:").size());
        assertEquals(List.of(), RPGCommandReceiver.suggestKeys(keys, "nosuch"));
    }

    @Test
    void failingCompleterSuggestsNothingInsteadOfNull() {
        assertEquals(List.of(), RPGCommandReceiver.quietly(() -> {
            throw new IllegalStateException("unknown item");
        }));
        assertEquals(List.of(), RPGCommandReceiver.quietly(() -> null));
    }
}
