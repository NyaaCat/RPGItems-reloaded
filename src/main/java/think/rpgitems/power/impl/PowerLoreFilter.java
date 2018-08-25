package think.rpgitems.power.impl;

import think.rpgitems.commands.PowerMeta;
import think.rpgitems.commands.Property;

/**
 * Power lorefilter.
 * <p>
 * Not a triggerable power.
 * Preserve all lore lines match the {@link #regex}.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(marker = true)
public class PowerLoreFilter extends BasePower {
    /**
     * Regex to filter the lore
     */
    @Property(order = 0)
    public String regex = null;
    /**
     * Display text
     */
    @Property(order = 1, required = true)
    public String desc = "";

    @Override
    public String getName() {
        return "lorefilter";
    }

    @Override
    public String displayText() {
        return desc;
    }
}
