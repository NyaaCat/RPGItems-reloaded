package think.rpgitems.power.marker;

import think.rpgitems.I18n;
import think.rpgitems.power.Meta;

/**
 * Power unbreakable.
 *
 * <p>Not a triggerable power. Mark this item as unbreakable.
 */
@Meta(marker = true)
public class Unbreakable extends BaseMarker {

    @Override
    public String getName() {
        return "unbreakable";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.unbreakable");
    }
}
