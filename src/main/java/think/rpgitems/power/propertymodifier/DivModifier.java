package think.rpgitems.power.propertymodifier;

import think.rpgitems.power.Meta;
import think.rpgitems.power.Property;

@Meta(marker = true)
public class DivModifier extends BaseModifier<Double> implements DoubleModifier {
    @Property
    public double value;

    @Override
    public String getName() {
        return "divmodifier";
    }

    @Override
    public Double apply(RgiParameter<Double> t) {
        return t.getValue() / value;
    }
}
