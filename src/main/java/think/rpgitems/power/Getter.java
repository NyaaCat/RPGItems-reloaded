package think.rpgitems.power;

import java.lang.reflect.InvocationTargetException;

@FunctionalInterface
public interface Getter<T> {
    String get(T object);

    static Getter from(Class<? extends Getter> cls) {
        try {
            return cls.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
