package think.rpgitems.power;

import java.lang.reflect.InvocationTargetException;

@FunctionalInterface
public interface Getter<T> {
    String get(T object);

    static Getter from(Power p, Class<? extends Getter> cls) {
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            try {
                return cls.getDeclaredConstructor(p.getClass()).newInstance(p);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
