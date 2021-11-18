package think.rpgitems.power;

import java.lang.reflect.InvocationTargetException;

@FunctionalInterface
public interface Getter<T> {
    static Getter from(PropertyHolder p, Class<? extends Getter> cls) {
        return getAccessor(p, cls);
    }

    static <T> T getAccessor(PropertyHolder p, Class<? extends T> cls) {
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            try {
                return cls.getDeclaredConstructor(cls.getEnclosingClass()).newInstance(p);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * @param object Object to serialize to String
     * @return String representation of the object
     */
    String get(T object);
}
