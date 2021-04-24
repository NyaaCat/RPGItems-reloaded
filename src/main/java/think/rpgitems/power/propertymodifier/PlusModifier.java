package think.rpgitems.power.propertymodifier;

import think.rpgitems.power.Meta;
import think.rpgitems.power.Property;

@Meta(marker = true)
public class PlusModifier extends BaseModifier<Double> implements DoubleModifier {
  @Property public double value;

  @Override
  public String getName() {
    return "plusmodifier";
  }

  @Override
  public Double apply(RgiParameter<Double> t) {
    return t.getValue() + value;
  }
}
