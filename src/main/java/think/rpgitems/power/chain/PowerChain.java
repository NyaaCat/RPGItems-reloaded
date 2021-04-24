package think.rpgitems.power.chain;

import java.util.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.Trigger;
import think.rpgitems.utils.cast.CastParameter;

/**
 * represents a set of Powers that triggers in the same time. It's very useful to create organized,
 * maintainable power list. also can be used to create effects.
 *
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

    private Set<Condition<?>> conditions = new HashSet<>();

    private int cooldown = 0;
    private boolean showCDWarning = false;

    private int cost = 0;
    private boolean castoff = false;
    private CastParameter castParameter = new CastParameter();

    /**
     * check whether a trigger can trigger a power.
     *
     * @param power
     * @param trigger
     * @return whether a trigger can trigger a power.
     */
    @SuppressWarnings("rawtypes")
    public static boolean isCompatibleTrigger(Power power, String trigger) {
        final Trigger trigger1 = Trigger.get(trigger);
        if (trigger1 == null) {
            return false;
        }
        final Class<? extends Power> pClass = power.getClass();
        final Meta annotation = pClass.getAnnotation(Meta.class);
        if (annotation != null) {
            final Class powerClass = trigger1.getPimplClass();
            return annotation.implClass().isAssignableFrom(powerClass);
        }
        return false;
    }

    // todo update old version cost & cooldown
    public PowerResult<?> doCost(RPGItem item, ItemStack itemStack) {
        // todo adapt this in lower level framework
        if (!item.consumeDurability(itemStack, getCost())) {
            return PowerResult.cost();
        }
        return PowerResult.noop();
    }

    public PowerResult<?> checkCooldown(Player player) {
        // todo adapt this in lower level framework
        if (!Utils.checkCooldown(this, player, getCooldown(), true, true)) return PowerResult.cd();
        return PowerResult.noop();
    }

    // <editor-fold desc="getters&setters">
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

    // </editor-fold>
}
