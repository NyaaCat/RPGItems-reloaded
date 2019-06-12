package think.rpgitems.power.impl;

import think.rpgitems.I18n;
import think.rpgitems.power.Pimpl;
import think.rpgitems.power.PowerMeta;


/**
 * Power unbreakable.
 * <p>
 * Not a triggerable power.
 * Mark this item as unbreakable.
 * </p>
 */
@PowerMeta(implClass = Pimpl.class, marker = true)
public class PowerUnbreakable extends BasePower {

    @Override
    public String getName() {
        return "unbreakable";
    }

    @Override
    public String displayText() {
        return I18n.format("power.unbreakable");
    }
}
