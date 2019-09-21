package think.rpgitems.power.marker;

import think.rpgitems.power.BasePropertyHolder;
import think.rpgitems.power.Marker;
import think.rpgitems.power.Property;

import java.io.Serializable;

/**
 * Base class containing common methods and fields.
 */
public abstract class BaseMarker extends BasePropertyHolder implements Serializable, Marker {
    @Property
    public String displayName;

    @Override
    public String displayName() {
        return displayName;
    }
}