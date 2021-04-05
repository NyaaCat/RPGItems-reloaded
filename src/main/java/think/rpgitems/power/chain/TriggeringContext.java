package think.rpgitems.power.chain;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import think.rpgitems.power.chain.utils.LocationVector;
import think.rpgitems.utils.cast.CastUtils;
import think.rpgitems.utils.cast.RoundedConeInfo;

import java.util.List;

public class TriggeringContext {
    private LocationVector initialLocationVector;

    public TriggeringContext(LivingEntity entity, double length){
        final Location eyeLocation = entity.getEyeLocation();
        final LocationVector locationVector = selectLocation(eyeLocation, eyeLocation.getDirection(), length);
    }

    public TriggeringContext(LocationVector locationVector){
        this.initialLocationVector = locationVector;
    }

    /**
     * @return
     * @see LocationVector
     */
    public LocationVector getTriggeringLocationVector(){
        return initialLocationVector;
    }

    /**
     * @see think.rpgitems.power.impl.Beam
     */
    public LocationVector selectLocation(Location start, Vector direction, double length) {
        final CastUtils.CastLocation castLocation = CastUtils.rayTrace(start, direction, length);
        final LocationVector locationVector = new LocationVector(castLocation.getTargetLocation(), castLocation.getNormalDirection());

        Entity hitEntity = castLocation.getHitEntity();
        if (hitEntity != null){
            locationVector.offerTarget(hitEntity);
        }

        return locationVector;
    }

    /**
     * calculate a fire location for a triggered chain.
     *
     * @param fromLocation
     * @param towards
     * @param range
     * @param roundedConeInfo
     * @return
     */
    public Location doCast(Location fromLocation, Vector towards, double range, RoundedConeInfo roundedConeInfo){
        //todo cache this
        final CastUtils.CastLocation castLocation = CastUtils.rayTrace(fromLocation, towards, range);
        final Location direction = new Location(fromLocation.getWorld(), towards.getX(), towards.getY(), towards.getZ());
        return CastUtils.parseFiringLocation(castLocation.getTargetLocation(), castLocation.getNormalDirection(), direction, roundedConeInfo);
    }

    public List<Entity> getNearbyEntities(LocationVector locationVector){
        //todo implement this
        throw new UnsupportedOperationException();
    }
}
