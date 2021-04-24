package think.rpgitems.power.marker;

import think.rpgitems.I18n;
import think.rpgitems.power.Meta;
import think.rpgitems.power.Property;

/**
 * Power ranged.
 *
 * <p>Not a triggerable power. Mark this item as ranged.
 */
@Meta(marker = true)
public class Ranged extends BaseMarker {
  /** Maximum radius */
  @Property(order = 1)
  public int r = Integer.MAX_VALUE;

  /** Minimum radius */
  @Property(order = 0)
  public int rm = 0;

  @Override
  public String getName() {
    return "ranged";
  }

  @Override
  public String displayText() {
    return I18n.formatDefault("power.ranged");
  }
}
