package think.rpgitems.power.propertymodifier;

import com.udojava.evalex.Expression;
import think.rpgitems.power.Meta;
import think.rpgitems.power.Property;

import java.math.BigDecimal;

@Meta(marker = true)
public class EvalModifier extends BaseModifier<Double>  implements DoubleModifier {
    @Property
    public String expression;

    @Override
    public String getName() {
        return "evalmodifier";
    }

    @Override
    public Double apply(Double t) {
        return new Expression(expression).and("value", new BigDecimal(t)).eval().doubleValue();
    }
}
