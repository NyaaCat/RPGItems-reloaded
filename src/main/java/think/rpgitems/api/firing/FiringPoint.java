package think.rpgitems.api.firing;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * One resolved firing position.
 *
 * @param kind     how powers use {@code location}, see {@link Kind}
 * @param location for {@link Kind#ORIGIN} the position shots start from, its yaw/pitch being the firing
 *                 direction; for {@link Kind#CAST} the position being fired at. Never null, world never null
 * @param entity   for {@link Kind#CAST} the entity standing at {@code location}, if any
 * @param normal   for {@link Kind#CAST} the surface normal at {@code location}; straight up when unknown
 */
public record FiringPoint(Kind kind, Location location, @Nullable Entity entity, Vector normal) {
    public enum Kind {
        /**
         * Replaces the firing entity: the power fires <em>from</em> the location, along its direction.
         * This is what {@code SELF} does with the player's own position.
         */
        ORIGIN,
        /**
         * Replaces the ray-traced cast point: the power fires <em>at / around</em> the location.
         * This is what {@code TARGET} does with the block or entity the player is looking at.
         */
        CAST
    }

    public FiringPoint {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(location.getWorld(), "location world");
        location = location.clone();
        normal = normal == null ? new Vector(0, 1, 0) : normal.clone();
    }

    public static FiringPoint origin(Location location) {
        return new FiringPoint(Kind.ORIGIN, location, null, null);
    }

    public static FiringPoint cast(Location location, @Nullable Entity entity) {
        return new FiringPoint(Kind.CAST, location, entity, null);
    }

    @Override
    public Location location() {
        return location.clone();
    }

    @Override
    public Vector normal() {
        return normal.clone();
    }
}
