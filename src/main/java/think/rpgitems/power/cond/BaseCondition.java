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

    @Property
    public Set<String> tags = new HashSet<>();

    @Override
    public Set<String> getTags() {
        return tags;
    }

    @Override
    public void addTag(String tag) {
        tags.add(tag);
    }

    @Override
    public void removeTag(String tag) {
        tags.remove(tag);
    }

    @Override
    public Set<String> getConditions() {
        return Collections.unmodifiableSet(conditions);
    }

    @Override
    public String displayText() {
        return displayText;
    }

    @Override
    public final String getPropertyHolderType() {
        return "condition";
    }
}