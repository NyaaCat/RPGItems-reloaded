package think.rpgitems.power.propertymodifier;

public interface DoubleModifier extends Modifier<Double> {
  default Class<Double> getModifierTargetType() {
    return Double.class;
  }
}
