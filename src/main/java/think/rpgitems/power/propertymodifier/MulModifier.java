package think.rpgitems.power.propertymodifier;

import think.rpgitems.power.Meta;
import think.rpgitems.power.Property;

@Meta(marker = true)
public class MulModifier extends BaseModifier<Double>  implements DoubleModifier {
    @Property
    public double value;

    @Override
    public String getName() {
        return "mulmodifier";
    }

    @Override
    public Double apply(Double t) {
        return t * value;
    }
}
