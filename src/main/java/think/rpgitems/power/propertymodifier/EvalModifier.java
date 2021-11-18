package think.rpgitems.power.propertymodifier;

import com.udojava.evalex.Expression;
import think.rpgitems.power.Meta;
import think.rpgitems.power.Property;
import think.rpgitems.power.Utils;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Meta(marker = true)
public class EvalModifier extends BaseModifier<Double> implements DoubleModifier {
    @Property
    public String expression;

    @Override
    public String getName() {
        return "evalmodifier";
    }

    @Override
    public Double apply(RgiParameter<Double> t) {
        Expression expression = new Expression(this.expression);
        expression.and("durability", Utils.lazyNumber(() -> Double.valueOf(t.getItem().getItemStackDurability(t.getItemStack()).orElse(0))));
        expression.and("random", Utils.lazyNumber(() -> ThreadLocalRandom.current().nextDouble()));
        expression.and("time", Utils.lazyNumber(() -> (double) (System.currentTimeMillis() / 50)));
        return expression.and("value", BigDecimal.valueOf(t.getValue())).eval().doubleValue();
    }
}
