package think.rpgitems.utils.cast;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import think.rpgitems.power.Getter;
import think.rpgitems.power.Setter;
import think.rpgitems.utils.WeightedPair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static think.rpgitems.power.Utils.weightedRandomPick;

public class CastUtils {
    /**
     * legacy
     * @see #rayTrace(Location, Vector, double, LivingEntity).
     */
    @Deprecated
    public static CastLocation rayTrace(LivingEntity from, Location fromLocation, Vector towards, double range) {
        return rayTrace(fromLocation, towards, range, from);
    }

    public static CastLocation rayTrace(Location fromLocation, Vector towards, double range, LivingEntity from) {
        Vector hitPosition;
        CastLocation castLocation = new CastLocation();

        RayTraceResult rayTraceResult = fromLocation.getWorld().rayTrace(fromLocation, towards, range, FluidCollisionMode.NEVER, true, 0.1,
                e -> e != null &&
                        (e instanceof LivingEntity || e.getType() == EntityType.ITEM_FRAME) &&
                        !(e instanceof LivingEntity && !((LivingEntity) e).isCollidable()) &&
                        (from == null || e.getUniqueId() != from.getUniqueId()) &&
                        !(e instanceof Player && ((Player) e).getGameMode() == GameMode.SPECTATOR));
        Vector normal = new Vector(0, 1, 0);
        if (rayTraceResult != null) {
            castLocation.hitEntity = rayTraceResult.getHitEntity();
            BlockFace hitBlockFace = rayTraceResult.getHitBlockFace();
            if (hitBlockFace != null) {
                normal = hitBlockFace.getDirection();
            }
            hitPosition = rayTraceResult.getHitPosition();
        } else {
            Location clone = fromLocation.clone();
            hitPosition = clone.add(clone.getDirection().normalize().multiply(range)).toVector();
        }

        Location targetLocation = new Location(fromLocation.getWorld(), hitPosition.getX(), hitPosition.getY(), hitPosition.getZ(), fromLocation.getYaw(), fromLocation.getPitch());
        castLocation.targetLocation = targetLocation;
        castLocation.normalDirection = normal;

        return castLocation;
    }

    public static CastLocation rayTrace(Location fromLocation, Vector towards, double range) {
        return rayTrace(fromLocation, towards, range, null);
    }

    private static final Vector yAxis = new Vector(0, 1, 0);

    public static Location parseFiringLocation(Location targetingLocation, Vector normalDir, Location direction, RoundedConeInfo coneInfo) {
        double r = coneInfo.getR();
        double phi = coneInfo.getRPhi();
        double theta = coneInfo.getRTheta();

        Vector base = normalDir.clone();

        Vector cross1;
        Vector dv = direction.getDirection();
        if (dv.getZ() == 0 && dv.getX()==0){
            Location clone = direction.clone();
            clone.setPitch(0);
            cross1 = clone.getDirection().getCrossProduct(yAxis).normalize();
        }else {
            cross1= dv.getCrossProduct(yAxis).normalize();
        }

        Vector clone = base.clone();
        clone.rotateAroundAxis(cross1, Math.toRadians(theta));
        clone.rotateAroundAxis(base, Math.toRadians(phi));
        clone.normalize().multiply(r);
        return targetingLocation.clone().add(clone);
    }

    private static final Vector crosser = new Vector(0, 1, 0);

    public static Vector makeCone(Location fromLocation, Vector towards, RoundedConeInfo coneInfo){
        Vector clone = towards.clone();
        Vector vertical;
        if (clone.getX() == 0 && clone.getZ() == 0){
            Location loclone = fromLocation.clone();
            loclone.setPitch(0);
            vertical = loclone.toVector();
        }else {
            vertical = clone.getCrossProduct(crosser);
        }
        Vector rotated = clone.clone();
        rotated.rotateAroundAxis(vertical, Math.toRadians(coneInfo.getTheta()));
        rotated.rotateAroundAxis(clone, Math.toRadians(coneInfo.getPhi() + coneInfo.getInitalRotation()));
        return rotated;
    }

    public static CastLocation of(Location location, LivingEntity target, Vector normal) {
        CastLocation castLocation = new CastLocation();
        castLocation.normalDirection = normal;
        castLocation.hitEntity = target;
        castLocation.targetLocation = location;
        return castLocation;
    }

    public static class CastLocation {
        Location targetLocation;
        Entity hitEntity;
        Vector normalDirection;

        public Location getTargetLocation() {
            return targetLocation;
        }

        public Entity getHitEntity() {
            return hitEntity;
        }

        public Vector getNormalDirection() {
            return normalDirection;
        }
    }

}
