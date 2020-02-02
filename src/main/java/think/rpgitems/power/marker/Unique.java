package think.rpgitems.power.marker;

import think.rpgitems.power.Meta;
import think.rpgitems.power.Property;

@Meta(marker = true)
public class Unique extends BaseMarker {
    @Property
    public boolean enabled = true;

    @Override
    public String displayText() {
        return null;
    }

    @Override
    public String getName() {
        return "unique";
    }
}
