package think.rpgitems.power.chain;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Condition;
import think.rpgitems.power.Power;
import think.rpgitems.power.chain.utils.LocationVector;
import think.rpgitems.power.trigger.Trigger;
import think.rpgitems.utils.cast.CastParameter;
import think.rpgitems.utils.cast.CastUtils;

import java.util.*;

/**
 * represents a set of Powers that triggers in the same time.
 * It's very useful to create organized, maintainable power list.
 * also can be used to create
 * @author ReinWD
 * @since v4.0
 */
public class PowerChain {
    private String chainId;
    private List<String> chainTags = new ArrayList<>();
    private List<Power> powers = new ArrayList<>();
    @SuppressWarnings("rawtypes")
    private Map<String, Trigger> triggers = new HashMap<>();
    private Set<Condition<?>>conditions = new HashSet<>();
    private int cooldown = 0;
    private int cost = 0;
    private boolean castoff = false;
    private CastParameter castParameter = new CastParameter();

    /**
     * check whether a trigger can trigger a power.
     * @param power
     * @param trigger
     * @return whether a trigger can trigger a power.
     */
    public boolean supportTrigger(Power power, String trigger){
        //todo implement this
        throw new UnsupportedOperationException();
    }


    public void doCost(RPGItem item, int amount, boolean force, boolean breakItem){
        //todo extract cost related code here
        //params can be modified freely

    }

    public boolean checkCooldown(){
        //todo extract cd related code here
        return false;
    }

    /**
     * @return
     * @see LocationVector
     */
    public LocationVector getTriggeringLocationVector(){
        //todo add function parameter
        LocationVector locationVector = new LocationVector();
        //todo build location vector
        throw new UnsupportedOperationException();
    }

    /**
     * @see think.rpgitems.power.impl.Beam
     */
    //todo ↓ fold this, assigned to ReinWD,
    //<editor-fold desc="beam related">
    public LocationVector rayTrace(Location start, Vector direction, double length){
        return rayTrace(start, direction, length, FluidCollisionMode.NEVER);
    }

    public LocationVector rayTrace(Location start, Vector direction, double length, FluidCollisionMode fluidCollisionMode) {
        //todo implement this
        throw new UnsupportedOperationException();
    }

    public CastUtils.CastLocation doCast(Location fromLocation, Vector towards, double range, LivingEntity from){
        //todo implement this
        if (from == null) {
            //todo implement this
        }
        throw new UnsupportedOperationException();
    }

    public CastUtils.CastLocation doCast(Location fromLocation, Vector towards, double range){
        //todo implement this
        return doCast(fromLocation, towards, range, null);
    }

    public List<Entity> getNearbyEntities(LocationVector locationVector){
        //todo implement this
        throw new UnsupportedOperationException();
    }

    //</editor-fold>

    //<editor-fold desc="getters&setters">
    public List<String> getChainTags() {
        return chainTags;
    }

    public List<Power> getPowers() {
        return powers;
    }

    public Map<String, Trigger> getTriggers() {
        return triggers;
    }

    public Set<Condition<?>> getConditions() {
        return conditions;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public boolean isCastoff() {
        return castoff;
    }

    public void setCastoff(boolean castoff) {
        this.castoff = castoff;
    }

    public CastParameter getCastParameter() {
        return castParameter;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    //</editor-fold>
}
