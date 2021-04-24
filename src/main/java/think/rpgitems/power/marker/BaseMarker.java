package think.rpgitems.power.marker;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import think.rpgitems.power.*;

/** Base class containing common methods and fields. */
public abstract class BaseMarker extends BasePropertyHolder implements Serializable, Marker {
  @Property public String markerId = "";
  @Property public Set<String> tags = new HashSet<>();

  @Override
  public Set<String> getTags() {
    return tags;
  }

  @Override
  public void addTag(String tag) {
    tags.add(tag);
  }

  @Override
  public void removeTag(String tag) {
    tags.remove(tag);
  }

  @Property public String displayName;

  @Override
  public String displayName() {
    return displayName;
  }

  @Override
  public final String getPropertyHolderType() {
    return "marker";
  }
}
