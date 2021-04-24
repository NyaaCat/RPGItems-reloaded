package think.rpgitems.power.marker;

import java.util.UUID;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EquipmentSlot;
import think.rpgitems.power.Meta;
import think.rpgitems.power.Property;

/**
 * Power attributemodifier.
 *
 * <p>Add attribute modifier to item
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
  public org.bukkit.attribute.AttributeModifier.Operation operation =
      org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER;

  @Property public EquipmentSlot slot = null;

  @Property public int uuidLeast = (int) UUID.randomUUID().getLeastSignificantBits();

  @Property public int uuidMost = (int) UUID.randomUUID().getMostSignificantBits();

  @Override
  public String getName() {
    return "attributemodifier";
  }

  @Override
  public String displayText() {
    return null;
  }
}
