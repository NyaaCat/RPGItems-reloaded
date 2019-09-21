package think.rpgitems.power.cond;

import think.rpgitems.power.BasePropertyHolder;
import think.rpgitems.power.Condition;
import think.rpgitems.power.Property;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class containing common methods and fields.
 */
public abstract class BaseCondition<T> extends BasePropertyHolder implements Serializable, Condition<T> {
    @Property
    public String displayText;
    @Property
    public Set<String> conditions = new HashSet<>();

    @Override
    public Set<String> getConditions() {
        return Collections.unmodifiableSet(conditions);
    }

    @Override
    public String displayText() {
        return displayText;
    }
}