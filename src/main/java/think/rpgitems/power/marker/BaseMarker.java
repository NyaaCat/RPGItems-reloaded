package think.rpgitems.power.marker;

import think.rpgitems.power.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class containing common methods and fields.
 */
public abstract class BaseMarker extends BasePropertyHolder implements Serializable, Marker {
    @Property
    public String markerId = "";
    @Property
    public Set<String> tags = new HashSet<>();

    @Override
    public String getPlaceholderId() {
        return markerId;
    }

    @Override
    public void setPlaceholderId(String id) {
        this.markerId = id;
    }

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

    @Property
    public String displayName;

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public final String getPropertyHolderType() {
        return "marker";
    }
}