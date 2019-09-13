package think.rpgitems.power.impl;

import think.rpgitems.I18n;
import think.rpgitems.power.Pimpl;
import think.rpgitems.power.Meta;


/**
 * Power ranged.
 * <p>
 * Not a triggerable power.
 * Mark this item as ranged only.
 * </p>
 */
@Meta(implClass = Pimpl.class, marker = true)
public class PowerRangedOnly extends PowerRanged {
    @Override
    public String getName() {
        return "rangedonly";
    }

    @Override
    public String displayText() {
        return I18n.format("power.rangedonly");
    }
}
