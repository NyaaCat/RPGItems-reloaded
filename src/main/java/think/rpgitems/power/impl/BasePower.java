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

    @Override
    public String requiredContext() {
        return requiredContext;
    }

    @Override
    public final String getType() {
        return "power";
    }
}