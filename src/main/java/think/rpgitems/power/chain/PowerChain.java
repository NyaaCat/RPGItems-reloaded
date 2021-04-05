package think.rpgitems.power.chain;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Condition;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Utils;
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
    private RPGItem ITEM;

    private String chainId;
    private String displayName;
    private List<String> chainTags = new ArrayList<>();
    private List<Power> powers = new ArrayList<>();
    @SuppressWarnings("rawtypes")
    private Map<String, Trigger> triggers = new HashMap<>();
    private Set<Condition<?>>conditions = new HashSet<>();

    private int cooldown = 0;
    private boolean showCDWarning = false;

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


    //todo update old version cost & cooldown
    public PowerResult<?> doCost(RPGItem item, ItemStack itemStack){
        //todo adapt this in lower level framework
        if (!item.consumeDurability(itemStack, getCost())){
            return PowerResult.cost();
        }
        return PowerResult.noop();
    }

    public PowerResult<?> checkCooldown(Player player){
        //todo adapt this in lower level framework
        if (!Utils.checkCooldown(this, player, getCooldown(), true, true))
            return PowerResult.cd();
        return PowerResult.noop();
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

    public boolean isShowCDWarning() {
        return showCDWarning;
    }

    public void setShowCDWarning(boolean showCDWarning) {
        this.showCDWarning = showCDWarning;
    }

    public RPGItem getItem() {
        return ITEM;
    }

    public void setItem(RPGItem item) {
        this.ITEM = item;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    //</editor-fold>
}
