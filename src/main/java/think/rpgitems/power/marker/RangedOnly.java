package think.rpgitems.power.marker;

import think.rpgitems.I18n;
import think.rpgitems.power.Meta;


/**
 * Power ranged.
 * <p>
 * Not a triggerable power.
 * Mark this item as ranged only.
 * </p>
 */
@Meta(marker = true)
public class RangedOnly extends Ranged {
    @Override
    public String getName() {
        return "rangedonly";
    }

    @Override
    public String displayText() {
        return I18n.format("power.rangedonly");
    }
}
