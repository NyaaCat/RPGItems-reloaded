package think.rpgitems.power.impl;

import think.rpgitems.I18n;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.Property;


/**
 * Power ranged.
 * <p>
 * Not a triggerable power.
 * Mark this item as ranged.
 * </p>
 */
@PowerMeta(marker = true)
public class PowerRanged extends BasePower {
    /**
     * Maximum radius
     */
    @Property(order = 1)
    public int r = Integer.MAX_VALUE;

    /**
     * Minimum radius
     */
    @Property(order = 0)
    public int rm = 0;

    @Override
    public String getName() {
        return "ranged";
    }

    @Override
    public String displayText() {
        return I18n.format("power.ranged");
    }
}
