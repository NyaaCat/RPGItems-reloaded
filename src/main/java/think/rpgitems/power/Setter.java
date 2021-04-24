package think.rpgitems.power;

import static think.rpgitems.power.Getter.getAccessor;

import java.util.Optional;

public interface Setter<T> {

  /**
   * @param value String representation of the object
   * @return The object to be set to field, or empty if field are already set by this setter
   * @throws IllegalArgumentException {@code value} is not a valid representation of the object
   */
  Optional<T> set(String value) throws IllegalArgumentException;

  static Setter from(PropertyHolder p, Class<? extends Setter> cls) {
    return getAccessor(p, cls);
  }
}
