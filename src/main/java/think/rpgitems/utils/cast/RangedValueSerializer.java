package think.rpgitems.utils.cast;

import java.util.Optional;
import think.rpgitems.power.Getter;
import think.rpgitems.power.Setter;

public class RangedValueSerializer implements Getter<RangedDoubleValue>, Setter<RangedDoubleValue> {
  @Override
  public String get(RangedDoubleValue object) {
    return object.toString();
  }

  @Override
  public Optional<RangedDoubleValue> set(String value) throws IllegalArgumentException {
    return Optional.of(RangedDoubleValue.of(value));
  }
}
