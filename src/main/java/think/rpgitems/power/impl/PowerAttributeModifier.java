package think.rpgitems.power.impl;

import think.rpgitems.commands.Property;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class PowerAttributeModifier extends BasePower {

    @Property(order = 0)
    public int amount = 2;

    @Property(order = 1)
    public String attributeName = "generic.maxHealth";

    @Property(order = 2)
    public String name = "generic.maxHealth";

    @Property(order = 3, required = true)
    public int operation = 0;

    @Property
    public String slot = null;

    @Property
    public int uuidLeast = (int) UUID.randomUUID().getLeastSignificantBits();

    @Property
    public int uuidMost = (int) UUID.randomUUID().getMostSignificantBits();

    @Override
    public String getName() {
        return "attributemodifier";
    }

    @Override
    public String displayText() {
        return null;
    }
}
