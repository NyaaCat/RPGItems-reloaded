package think.rpgitems.power.marker;

import java.util.regex.Pattern;
import think.rpgitems.power.Meta;
import think.rpgitems.power.Property;

/**
 * Power lorefilter.
 *
 * <p>Not a triggerable power. Preserve all lore lines match the {@link #regex}.
 */
@SuppressWarnings("WeakerAccess")
@Meta(marker = true)
public class LoreFilter extends BaseMarker {
  /** Regex to filter the lore */
  @Property(order = 0)
  public String regex = null;
  /** Display text */
  @Property(order = 1, required = true)
  public String desc = "";

  /** Whether use .find() instead of .match() */
  @Property public boolean find = false;

  private Pattern pattern;

  public LoreFilter compile() {
    pattern = Pattern.compile(regex);
    return this;
  }

  public Pattern pattern() {
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
