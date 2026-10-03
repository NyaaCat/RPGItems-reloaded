package think.rpgitems;

import cat.nyaa.nyaacore.cmdreceiver.BadCommandException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Predicate;

/**
 * Resolves the target argument of {@code /rpgitem give}.
 * <p>
 * A target specification is a comma separated list whose elements are online player names, player UUIDs or
 * Minecraft target selectors ({@code @a}, {@code @p[distance=..5]}, ...). Resolution is all-or-nothing: the
 * whole specification is resolved and validated before anything is handed out, so a bad element never leaves
 * some players with the item and others without.
 */
public final class GiveTargets {
    /**
     * Needed to use a target selector ({@code @...}).
     */
    public static final String PERMISSION_SELECTOR = "rpgitem.givetarget.selector";
    /**
     * Needed to give to more than one player with a single command.
     */
    public static final String PERMISSION_MULTIPLE = "rpgitem.givetarget.multiple";
    /**
     * Largest amount accepted per player: a full inventory of 64-stacks.
     */
    public static final int MAX_COUNT = 36 * 64;

    private GiveTargets() {
    }

    /**
     * The server operations target resolution needs, separated out so the rules can be tested without one.
     */
    public interface Lookup {
        /**
         * @return the online player for a name or UUID, or null
         */
        Player player(String nameOrUuid);

        /**
         * @return entities matched by {@code selector} evaluated from {@code sender}'s position
         * @throws IllegalArgumentException if the selector cannot be parsed
         */
        List<Entity> select(CommandSender sender, String selector) throws IllegalArgumentException;
    }

    public static final Lookup BUKKIT = new Lookup() {
        @Override
        public Player player(String nameOrUuid) {
            try {
                return Bukkit.getPlayer(UUID.fromString(nameOrUuid));
            } catch (IllegalArgumentException ignored) {
            }
            Player exact = Bukkit.getPlayerExact(nameOrUuid);
            // Same lookup the single-player syntax has always used.
            return exact != null ? exact : Bukkit.getPlayer(nameOrUuid);
        }

        @Override
        public List<Entity> select(CommandSender sender, String selector) {
            return Bukkit.selectEntities(sender, selector);
        }
    };

    /**
     * Splits a specification on the commas that are not inside a selector's {@code [...]}, an NBT
     * {@code {...}} or a quoted string.
     *
     * @throws BadCommandException if an element is empty or brackets do not balance
     */
    public static List<String> split(String spec) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Deque<Character> open = new ArrayDeque<>();
        char quote = 0;
        for (int i = 0; i < spec.length(); i++) {
            char c = spec.charAt(i);
            if (quote != 0) {
                current.append(c);
                if (c == '\\' && i + 1 < spec.length()) {
                    current.append(spec.charAt(++i));
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            switch (c) {
                case '"', '\'' -> {
                    quote = c;
                    current.append(c);
                }
                case '[', '{' -> {
                    open.push(c);
                    current.append(c);
                }
                case ']', '}' -> {
                    char expected = c == ']' ? '[' : '{';
                    if (open.isEmpty() || open.pop() != expected) {
                        throw new BadCommandException("message.give.error.malformed", spec);
                    }
                    current.append(c);
                }
                case ',' -> {
                    if (open.isEmpty()) {
                        parts.add(current.toString());
                        current.setLength(0);
                    } else {
                        current.append(c);
                    }
                }
                default -> current.append(c);
            }
        }
        if (quote != 0 || !open.isEmpty()) {
            throw new BadCommandException("message.give.error.malformed", spec);
        }
        parts.add(current.toString());
        for (String part : parts) {
            if (part.isBlank()) {
                throw new BadCommandException("message.give.error.malformed", spec);
            }
        }
        return parts;
    }

    /**
     * @param sender     who runs the command; selectors are evaluated from this sender's position
     * @param spec       the raw target argument
     * @param lookup     server access
     * @param permission permission check for {@code sender}
     * @return the distinct players to give to, in the order they were first matched, never empty
     * @throws BadCommandException if anything about the specification is wrong; nothing may be given then
     */
    public static List<Player> resolve(CommandSender sender, String spec, Lookup lookup, Predicate<String> permission) {
        List<String> parts = split(spec);
        Map<UUID, Player> players = new LinkedHashMap<>();
        for (String raw : parts) {
            String part = raw.trim();
            if (part.startsWith("@")) {
                if (!permission.test(PERMISSION_SELECTOR)) {
                    throw new BadCommandException("message.give.error.permission_selector", PERMISSION_SELECTOR);
                }
                List<Entity> entities;
                try {
                    entities = lookup.select(sender, part);
                } catch (IllegalArgumentException e) {
                    throw new BadCommandException("message.give.error.selector", part);
                }
                if (entities == null || entities.isEmpty()) {
                    throw new BadCommandException("message.give.error.no_match", part);
                }
                for (Entity entity : entities) {
                    if (!(entity instanceof Player player)) {
                        throw new BadCommandException("message.give.error.not_player", part);
                    }
                    players.putIfAbsent(player.getUniqueId(), player);
                }
            } else {
                Player player = lookup.player(part);
                if (player == null) {
                    throw new BadCommandException("message.give.error.player_not_found", part);
                }
                players.putIfAbsent(player.getUniqueId(), player);
            }
        }
        if (players.size() > 1 && !permission.test(PERMISSION_MULTIPLE)) {
            throw new BadCommandException("message.give.error.permission_multiple", PERMISSION_MULTIPLE);
        }
        return new ArrayList<>(players.values());
    }

    /**
     * @param raw the amount argument, or null if it was omitted
     * @return the amount each player receives; 1 if omitted
     * @throws BadCommandException if present but not a whole number from 1 to {@link #MAX_COUNT}
     */
    public static int parseCount(String raw) {
        if (raw == null) return 1;
        int count;
        try {
            count = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new BadCommandException("message.give.error.count", raw, MAX_COUNT);
        }
        if (count < 1 || count > MAX_COUNT) {
            throw new BadCommandException("message.give.error.count", raw, MAX_COUNT);
        }
        return count;
    }

    /**
     * Completions for the target argument: continues a comma separated list, never offering a name twice.
     *
     * @param typed       what has been typed for the argument so far
     * @param playerNames online player names
     * @param selectors   whether the sender may use selectors
     */
    public static List<String> complete(String typed, Collection<String> playerNames, boolean selectors) {
        int depth = 0;
        int cut = -1;
        for (int i = 0; i < typed.length(); i++) {
            char c = typed.charAt(i);
            if (c == '[' || c == '{') depth++;
            else if (c == ']' || c == '}') depth--;
            else if (c == ',' && depth == 0) cut = i;
        }
        if (depth != 0) return Collections.emptyList(); // inside a selector's arguments
        String prefix = typed.substring(0, cut + 1);
        String last = typed.substring(cut + 1).toLowerCase(Locale.ROOT);
        Set<String> used = new HashSet<>();
        for (String s : prefix.split(",")) used.add(s.toLowerCase(Locale.ROOT));
        List<String> candidates = new ArrayList<>(new TreeSet<>(playerNames));
        if (selectors) candidates.addAll(List.of("@a", "@p", "@r", "@s"));
        List<String> result = new ArrayList<>();
        for (String candidate : candidates) {
            String lower = candidate.toLowerCase(Locale.ROOT);
            if (lower.startsWith(last) && !used.contains(lower)) result.add(prefix + candidate);
        }
        return result;
    }
}
