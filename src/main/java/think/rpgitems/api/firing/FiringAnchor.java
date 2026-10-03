package think.rpgitems.api.firing;

import java.util.Optional;

/**
 * A firing location bound to one activation of one power.
 * <p>
 * The anchor is created once, synchronously, by {@link FiringLocationProvider#bind(FiringContext)} and must
 * capture everything it needs (which entity, which owner) at that moment. RPGItems then calls
 * {@link #locate()} once for every shot, which for burst and delayed powers can be many ticks later. An
 * anchor must therefore never read "the current" attacker from shared state inside {@code locate()}; it
 * re-validates what it captured and answers empty once that is gone.
 */
@FunctionalInterface
public interface FiringAnchor {
    /**
     * Called on the server thread immediately before a shot is fired.
     *
     * @return where to fire, or empty if the captured context is no longer valid (entity dead or removed,
     * owner offline, world unloaded...). An empty answer skips the shot and ends any remaining burst.
     */
    Optional<FiringPoint> locate();
}
