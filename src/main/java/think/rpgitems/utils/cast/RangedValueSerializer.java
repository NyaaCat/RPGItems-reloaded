package think.rpgitems.utils.cast;

import think.rpgitems.power.Getter;
import think.rpgitems.power.Setter;

import java.util.Optional;

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
