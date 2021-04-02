package think.rpgitems.power.chain.utils;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;


/**
 * information needed to trigger a power.
 * include a center point, a vector, a target point (raytracing) and a target entity (nullable)
 */
public class LocationVector {
    Location center;

    /**
     * a vector starting from center point.
     *
     * both its length & direction are meaningful.
     * length indicates the range of a specific power.
     * e.g. effective range, targeting range, or initial velocity.
     */
    Vector vector;

    /**
     * nullable
     */
    LivingEntity target;

    /**
     * nonnull
     * best to be lazy loaded.
     */
    Location targetPoint;
}
