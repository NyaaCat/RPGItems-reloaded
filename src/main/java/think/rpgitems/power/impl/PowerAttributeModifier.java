package think.rpgitems.power.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import think.rpgitems.power.Pimpl;
import think.rpgitems.power.Meta;
import think.rpgitems.power.Property;

import java.util.UUID;

/**
 * Power attributemodifier.
 * <p>
 * Add attribute modifier to item
 * </p>
 */
@Meta(implClass = Pimpl.class, marker = true)
public class PowerAttributeModifier extends BasePower {

    @Property(order = 0)
    public double amount = 2;

    @Property(order = 1)
    public Attribute attribute = Attribute.GENERIC_MAX_HEALTH;

    @Property(order = 2)
    public String name = "generic.maxHealth";

    @Property(order = 3, required = true)
    public AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;

    @Property
    public EquipmentSlot slot = null;

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
