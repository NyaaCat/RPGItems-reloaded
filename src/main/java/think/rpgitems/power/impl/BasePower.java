package think.rpgitems.power.impl;

import think.rpgitems.power.*;
import think.rpgitems.power.trigger.Trigger;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class containing common methods and fields.
 */
public abstract class BasePower extends BasePropertyHolder implements Serializable, Power {
    @Property
    public String displayName;
    @Property
    @AcceptedValue(preset = Preset.TRIGGERS)
    public Set<Trigger> triggers = Power.getDefaultTriggers(this.getClass());
    @Property
    public Set<String> selectors = new HashSet<>();
    @Property
    public Set<String> conditions = new HashSet<>();
    @Property
    public String requiredContext;
    @Property
    public String powerId = "";
    @Property
    public Set<String> powerTag = new HashSet<>();

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public Set<Trigger> getTriggers() {
        return Collections.unmodifiableSet(triggers);
    }

    @Override
    public Set<String> getSelectors() {
        return Collections.unmodifiableSet(selectors);
    }

    @Override
    public Set<String> getConditions() {
        return Collections.unmodifiableSet(conditions);
    }

    public String getPowerId() {
        return powerId;
    }

    public void setPowerId(String powerId) {
        this.powerId = powerId;
    }

    public Set<String> getPowerTag() {
        return powerTag;
    }

    public void addPowerTag(String tag){
        powerTag.add(tag);
    }

    public void removePowerTag(String tag){
        powerTag.remove(tag);
    }

    @Override
    public String requiredContext() {
        return requiredContext;
    }

    @Override
    public final String getPropertyHolderType() {
        return "power";
    }
}