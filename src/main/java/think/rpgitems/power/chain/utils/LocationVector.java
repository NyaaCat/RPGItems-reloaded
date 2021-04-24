package think.rpgitems.power.chain.utils;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/**
 * information needed to trigger a power. include a center point, a vector, a target point
 * (raytracing) and a target entity list (nullable)
 */
public class LocationVector {
    public LocationVector(Location center, Vector vector) {
        this.center = center;
        this.vector = vector;
    }

    Location center;

    /**
     * a vector starting from center point.
     *
     * <p>both its length & direction are meaningful. length indicates the range of a specific
     * power. e.g. effective range, targeting range, or initial velocity.
     */
    Vector vector;

    /** nullable */
    List<Entity> targets = new ArrayList<>();

    // <editor-fold desc="getters&setters" defaultstate="collapsed">
    public Location getCenter() {
        return center;
    }

    public void setCenter(Location center) {
        this.center = center;
    }

    public Vector getVector() {
        return vector;
    }

    public void setVector(Vector vector) {
        this.vector = vector;
    }

    public List<Entity> getTargets(
            Location startLocation, Vector towards, double length, double angle) {
        if (targets.isEmpty()) {
            // todo implement this
        }
        return targets;
    }

    public void offerTarget(Entity hitEntity) {
        targets.add(0, hitEntity);
    }
    // </editor-fold>
}
