package think.rpgitems.api.firing;

import java.util.Optional;

/**
 * Resolves an extension {@code firingLocation} name. Register with
 * {@link FiringLocations#register(org.bukkit.plugin.Plugin, String, FiringLocationProvider)}.
 */
@FunctionalInterface
public interface FiringLocationProvider {
    /**
     * Called on the server thread when a power using this firing location is activated, before its cooldown
     * and durability cost are applied.
     *
     * @param context who is firing what; do not retain beyond what the returned anchor needs
     * @return an anchor for this activation, or empty if the location has no meaning in this context, in
     * which case the power does not fire and nothing is consumed
     */
    Optional<FiringAnchor> bind(FiringContext context);
}
