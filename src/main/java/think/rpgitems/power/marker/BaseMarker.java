package think.rpgitems.power.marker;

import think.rpgitems.power.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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