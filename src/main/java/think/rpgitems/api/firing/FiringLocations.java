package think.rpgitems.api.firing;

import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry of extension {@code firingLocation} names.
 * <p>
 * Registration and lookup are thread-safe. {@link #bind} and the anchors it returns touch live entities and
 * must only be used on the server thread.
 * <p>
 * Every registration belongs to a plugin. RPGItems drops all registrations of a plugin when that plugin is
 * disabled, so a provider can never outlive the classes that back it; extensions should still call
 * {@link #unregisterAll(Plugin)} from their own {@code onDisable}.
 */
public final class FiringLocations {
    private static final Map<String, Registration> PROVIDERS = new ConcurrentHashMap<>();
    private static final Logger LOGGER = Logger.getLogger("RPGItems");

    private record Registration(Plugin owner, FiringLocationProvider provider) {
    }

    private FiringLocations() {
    }

    /**
     * @param owner    plugin providing the location, used for cleanup
     * @param name     case-insensitive name as it will be written in {@code firingLocation:<name>}
     * @param provider the resolver
     * @throws IllegalArgumentException if the name is invalid, is {@code SELF}/{@code TARGET}, or is already
     *                                  registered by a different plugin
     */
    public static void register(Plugin owner, String name, FiringLocationProvider provider) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(provider, "provider");
        FiringLocation location = FiringLocation.of(name);
        if (!location.isExtension()) {
            throw new IllegalArgumentException("built-in firing location cannot be replaced: " + location);
        }
        Registration registration = new Registration(owner, provider);
        PROVIDERS.compute(location.name(), (key, existing) -> {
            // The same plugin may register again (RPGItemsReloadEvent); anybody else may not take the name.
            if (existing != null && !existing.owner().getName().equals(owner.getName())) {
                throw new IllegalArgumentException("firing location " + key + " is already registered by " + existing.owner().getName());
            }
            return registration;
        });
    }

    /**
     * @return true if {@code name} was registered by {@code owner} and has been removed
     */
    public static boolean unregister(Plugin owner, String name) {
        String key = FiringLocation.normalize(name);
        Registration existing = PROVIDERS.get(key);
        return existing != null && existing.owner().getName().equals(owner.getName()) && PROVIDERS.remove(key, existing);
    }

    /**
     * Removes every firing location registered by {@code owner}.
     *
     * @return number of removed registrations
     */
    public static int unregisterAll(Plugin owner) {
        int before = PROVIDERS.size();
        PROVIDERS.values().removeIf(r -> r.owner().getName().equals(owner.getName()));
        return before - PROVIDERS.size();
    }

    /**
     * Removes every registration. Called by RPGItems when it is disabled; extensions use
     * {@link #unregisterAll(Plugin)}.
     */
    public static void clear() {
        PROVIDERS.clear();
    }

    public static boolean isRegistered(String name) {
        return PROVIDERS.containsKey(FiringLocation.normalize(name));
    }

    /**
     * @return true for {@code SELF}, {@code TARGET} and every currently registered extension name
     */
    public static boolean isKnown(FiringLocation location) {
        return !location.isExtension() || PROVIDERS.containsKey(location.name());
    }

    /**
     * @return {@code SELF}, {@code TARGET}, then the registered extension names in alphabetical order
     */
    public static List<String> names() {
        List<String> names = new ArrayList<>();
        names.add(FiringLocation.SELF.name());
        names.add(FiringLocation.TARGET.name());
        names.addAll(new TreeSet<>(PROVIDERS.keySet()));
        return names;
    }

    /**
     * Binds an extension firing location for one power activation. Server thread only.
     *
     * @return the anchor, or empty if the name has no provider (extension not installed or disabled), the
     * provider declined the context, or the provider failed
     */
    public static Optional<FiringAnchor> bind(FiringLocation location, FiringContext context) {
        if (!location.isExtension()) {
            throw new IllegalArgumentException(location + " is built in and is not resolved through providers");
        }
        Registration registration = PROVIDERS.get(location.name());
        if (registration == null || !registration.owner().isEnabled()) {
            return Optional.empty();
        }
        try {
            Optional<FiringAnchor> anchor = registration.provider().bind(context);
            return anchor == null ? Optional.empty() : anchor.map(FiringLocations::guarded);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "firing location " + location + " of " + registration.owner().getName() + " failed to bind", e);
            return Optional.empty();
        }
    }

    /**
     * Binds and resolves the first shot in one go.
     *
     * @return a bound firing whose first point is already known, or empty if the power must not fire
     */
    public static Optional<BoundFiring> begin(FiringLocation location, FiringContext context) {
        Optional<FiringAnchor> anchor = bind(location, context);
        if (anchor.isEmpty()) return Optional.empty();
        return anchor.get().locate().map(first -> new BoundFiring(anchor.get(), first));
    }

    private static FiringAnchor guarded(FiringAnchor anchor) {
        return () -> {
            try {
                Optional<FiringPoint> point = anchor.locate();
                return point == null ? Optional.empty() : point;
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "firing anchor failed to locate", e);
                return Optional.empty();
            }
        };
    }

    /**
     * An anchor together with the point it resolved to when the power was activated.
     */
    public record BoundFiring(FiringAnchor anchor, FiringPoint first) {
        /**
         * @param frozen true to keep using the activation-time point (the powers' {@code castOff}), false to
         *               ask the anchor again
         * @return the point for the next shot, or empty if the shot must be skipped
         */
        public Optional<FiringPoint> next(boolean frozen) {
            return frozen ? Optional.of(first) : anchor.locate();
        }
    }
}
