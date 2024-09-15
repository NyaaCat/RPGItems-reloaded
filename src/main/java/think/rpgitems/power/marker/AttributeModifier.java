package think.rpgitems.power.marker;

import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import think.rpgitems.RPGItems;
import think.rpgitems.power.Meta;
import think.rpgitems.power.Property;
import org.bukkit.NamespacedKey;

import java.util.UUID;

/**
 * Power attributemodifier.
 * <p>
 * Add attribute modifier to item
 * </p>
 */
@Meta(marker = true)
public class AttributeModifier extends BaseMarker {

    @Property(order = 0)
    public double amount = 2;

    @Property(order = 1)
    public Attribute attribute = Attribute.GENERIC_MAX_HEALTH;

    @Property(order = 2)
    public String name = "generic.maxHealth";

    @Property(order = 3, required = true)
    public org.bukkit.attribute.AttributeModifier.Operation operation = org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER;

    @Property
    public EquipmentSlotGroup slot = null;

    @Property
    public String namespacedKey = new NamespacedKey(RPGItems.plugin,UUID.randomUUID().toString()).toString();

    @Override
    public String getName() {
        return "attributemodifier";
    }

    @Override
    public String displayText() {
        return null;
    }
}
