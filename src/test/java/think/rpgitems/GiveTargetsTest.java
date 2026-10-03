package think.rpgitems;

import cat.nyaa.nyaacore.cmdreceiver.BadCommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class GiveTargetsTest {
    private static final Predicate<String> ALL = p -> true;
    private static final Predicate<String> NONE = p -> false;

    private final Player alice = player("Alice");
    private final Player bob = player("Bob");
    private final Player carol = player("Carol");
    private final Zombie zombie = TestProxies.of(Zombie.class, "zombie", Map.of());
    private final CommandSender console = TestProxies.of(CommandSender.class, "console", Map.of());

    /** Records every lookup so tests can assert what was (not) asked of the server. */
    private final List<String> selectorCalls = new ArrayList<>();
    private final Map<String, List<Entity>> selectors = new HashMap<>();
    private final GiveTargets.Lookup lookup = new GiveTargets.Lookup() {
        @Override
        public Player player(String name) {
            for (Player p : List.of(alice, bob, carol)) {
                if (p.getName().equalsIgnoreCase(name) || p.getUniqueId().toString().equals(name)) return p;
            }
            return null;
        }

        @Override
        public List<Entity> select(CommandSender sender, String selector) {
            selectorCalls.add(sender + ":" + selector);
            if (!selectors.containsKey(selector)) throw new IllegalArgumentException("bad selector");
            return selectors.get(selector);
        }
    };

    private static Player player(String name) {
        UUID id = UUID.nameUUIDFromBytes(name.getBytes());
        return TestProxies.of(Player.class, name, Map.of("getName", a -> name, "getUniqueId", a -> id));
    }

    private static String key(Runnable r) {
        return assertThrows(BadCommandException.class, r::run).getMessage();
    }

    // ---- splitting

    @Test
    void splitsOnTopLevelCommasOnly() {
        assertEquals(List.of("Alice", "Bob"), GiveTargets.split("Alice,Bob"));
        assertEquals(List.of("@a[tag=x,limit=2]", "Bob"), GiveTargets.split("@a[tag=x,limit=2],Bob"));
        assertEquals(List.of("@a[nbt={a:1b,b:[1,2]}]", "@p"), GiveTargets.split("@a[nbt={a:1b,b:[1,2]}],@p"));
        assertEquals(List.of("@a[name=\"a,b]\"]", "Bob"), GiveTargets.split("@a[name=\"a,b]\"],Bob"));
        assertEquals(List.of("Alice"), GiveTargets.split("Alice"));
    }

    @Test
    void rejectsEmptyElementsAndUnbalancedBrackets() {
        for (String bad : List.of("", ",", "Alice,", ",Alice", "Alice,,Bob", "@a[tag=x", "@a]", "@a[nbt={a:1]}", "@a[name=\"x]")) {
            assertEquals("message.give.error.malformed", key(() -> GiveTargets.split(bad)), bad);
        }
    }

    // ---- resolution

    @Test
    void singleNameNeedsNoExtraPermission() {
        assertEquals(List.of(alice), GiveTargets.resolve(console, "Alice", lookup, NONE));
        assertEquals(List.of(bob), GiveTargets.resolve(console, bob.getUniqueId().toString(), lookup, NONE));
        assertTrue(selectorCalls.isEmpty());
    }

    @Test
    void nameListKeepsOrderAndDeduplicates() {
        assertEquals(List.of(bob, alice, carol), GiveTargets.resolve(console, "Bob,Alice,bob,Carol,ALICE", lookup, ALL));
    }

    @Test
    void selectorsAndNamesCombineWithoutDuplicates() {
        selectors.put("@a", List.of(alice, bob, carol));
        selectors.put("@a[tag=x,limit=2]", List.of(carol, alice));
        assertEquals(List.of(bob, alice, carol), GiveTargets.resolve(console, "Bob,@a", lookup, ALL));
        assertEquals(List.of(carol, alice, bob), GiveTargets.resolve(console, "@a[tag=x,limit=2],Bob,@a", lookup, ALL));
    }

    @Test
    void selectorIsEvaluatedWithTheSendersContext() {
        selectors.put("@p", List.of(alice));
        GiveTargets.resolve(alice, "@p", lookup, ALL);
        GiveTargets.resolve(console, "@p", lookup, ALL);
        assertEquals(List.of("Alice:@p", "console:@p"), selectorCalls);
    }

    @Test
    void unknownPlayerFailsTheWholeList() {
        assertEquals("message.give.error.player_not_found", key(() -> GiveTargets.resolve(console, "Alice,Nobody,Bob", lookup, ALL)));
    }

    @Test
    void selectorNeedsSelectorPermissionAndIsNotEvaluatedWithoutIt() {
        selectors.put("@a", List.of(alice));
        assertEquals("message.give.error.permission_selector", key(() -> GiveTargets.resolve(alice, "@a", lookup, NONE)));
        assertEquals("message.give.error.permission_selector",
                key(() -> GiveTargets.resolve(alice, "@a", lookup, p -> p.equals(GiveTargets.PERMISSION_MULTIPLE))));
        assertTrue(selectorCalls.isEmpty(), "selector must not run before the permission check");
    }

    @Test
    void multipleRecipientsNeedMultiplePermission() {
        selectors.put("@a", List.of(alice, bob));
        selectors.put("@s", List.of(alice));
        Predicate<String> selectorOnly = p -> p.equals(GiveTargets.PERMISSION_SELECTOR);
        assertEquals("message.give.error.permission_multiple", key(() -> GiveTargets.resolve(alice, "@a", lookup, selectorOnly)));
        assertEquals("message.give.error.permission_multiple", key(() -> GiveTargets.resolve(alice, "Alice,Bob", lookup, NONE)));
        // one recipient is fine, however it was written
        assertEquals(List.of(alice), GiveTargets.resolve(alice, "@s", lookup, selectorOnly));
        assertEquals(List.of(alice), GiveTargets.resolve(alice, "Alice,alice", lookup, NONE));
    }

    @Test
    void emptySelectorMatchIsAnErrorNotASilentNoOp() {
        selectors.put("@a[tag=nobody]", List.of());
        selectors.put("@a", List.of(alice));
        assertEquals("message.give.error.no_match", key(() -> GiveTargets.resolve(console, "@a[tag=nobody]", lookup, ALL)));
        assertEquals("message.give.error.no_match", key(() -> GiveTargets.resolve(console, "@a,@a[tag=nobody]", lookup, ALL)));
    }

    @Test
    void selectorMatchingNonPlayersIsRejectedInsteadOfFiltered() {
        selectors.put("@e", List.of(alice, zombie, bob));
        selectors.put("@e[type=player]", List.of(alice, bob));
        assertEquals("message.give.error.not_player", key(() -> GiveTargets.resolve(console, "@e", lookup, ALL)));
        assertEquals(List.of(alice, bob), GiveTargets.resolve(console, "@e[type=player]", lookup, ALL));
    }

    @Test
    void unparsableSelectorIsReportedAsSuch() {
        assertEquals("message.give.error.malformed", key(() -> GiveTargets.resolve(console, "@x[", lookup, ALL)));
        assertEquals("message.give.error.selector", key(() -> GiveTargets.resolve(console, "Alice,@x[]", lookup, ALL)));
        assertEquals("message.give.error.selector", key(() -> GiveTargets.resolve(console, "@zzz", lookup, ALL)));
    }

    // ---- amount

    @Test
    void amountDefaultsToOneOnlyWhenOmitted() {
        assertEquals(1, GiveTargets.parseCount(null));
        assertEquals(5, GiveTargets.parseCount("5"));
        assertEquals(GiveTargets.MAX_COUNT, GiveTargets.parseCount(String.valueOf(GiveTargets.MAX_COUNT)));
        for (String bad : List.of("0", "-1", "abc", "1.5", "", "5k", String.valueOf(GiveTargets.MAX_COUNT + 1), "99999999999")) {
            assertEquals("message.give.error.count", key(() -> GiveTargets.parseCount(bad)), bad);
        }
    }

    // ---- tab completion

    @Test
    void completionOffersPlayersAndSelectorsAndContinuesLists() {
        List<String> online = List.of("Bob", "Alice", "Carol");
        assertEquals(List.of("Alice", "Bob", "Carol", "@a", "@p", "@r", "@s"), GiveTargets.complete("", online, true));
        assertEquals(List.of("Alice", "Bob", "Carol"), GiveTargets.complete("", online, false));
        assertEquals(List.of("Carol"), GiveTargets.complete("ca", online, true));
        assertEquals(List.of("Alice,Bob", "Alice,Carol", "Alice,@a", "Alice,@p", "Alice,@r", "Alice,@s"), GiveTargets.complete("Alice,", online, true));
        assertEquals(List.of("Alice,@a[tag=x,limit=1],Bob"), GiveTargets.complete("Alice,@a[tag=x,limit=1],b", online, true));
        assertEquals(List.of(), GiveTargets.complete("@a[tag=", online, true));
    }
}
