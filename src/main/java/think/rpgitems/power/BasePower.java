package think.rpgitems.power;

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
    public Set<String> powerTags = new HashSet<>();

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

    public Set<String> getPowerTags() {
        return powerTags;
    }

    public void addPowerTag(String tag) {
        powerTags.add(tag);
    }

    public void removePowerTag(String tag) {
        powerTags.remove(tag);
    }

    @Override
    public String getPlaceholderId() {
        return getPowerId();
    }

    @Override
    public void setPlaceholderId(String id) {
        this.setPowerId(id);
    }

    @Override
    public Set<String> getTags() {
        return getPowerTags();
    }

    @Override
    public void addTag(String tag) {
        addPowerTag(tag);
    }

    @Override
    public void removeTag(String tag) {
        removePowerTag(tag);
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