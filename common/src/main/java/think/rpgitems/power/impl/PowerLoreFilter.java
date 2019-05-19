package think.rpgitems.power.impl;

import think.rpgitems.power.BasePower;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.Property;

import java.util.regex.Pattern;

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

    /**
     * Whether use .find() instead of .match()
     */
    @Property
    public boolean find = false;

    private Pattern pattern;

    public PowerLoreFilter compile(){
        pattern = Pattern.compile(regex);
        return this;
    }

    public Pattern pattern(){
        return pattern;
    }

    @Override
    public String getName() {
        return "lorefilter";
    }

    @Override
    public String displayText() {
        return desc;
    }
}
