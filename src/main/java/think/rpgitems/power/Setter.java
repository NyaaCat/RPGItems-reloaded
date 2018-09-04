package think.rpgitems.power;

import java.lang.reflect.InvocationTargetException;

public interface Setter<T> {
    T set(String value) throws IllegalArgumentException;

    static Setter from(Power p, Class<? extends Setter> cls) {
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
