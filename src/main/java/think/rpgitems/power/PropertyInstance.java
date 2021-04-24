package think.rpgitems.power;

import java.lang.reflect.Field;

public class PropertyInstance {
    private final String name;

    private final boolean required;

    private final int order;

    private final String[] alias;

    private final Field field;

    private PropertyInstance(
            String name, boolean required, int order, String[] alias, Field field) {
        this.name = name;
        this.required = required;
        this.order = order;
        this.alias = alias;
        this.field = field;
    }

    public String name() {
        return name;
    }

    public boolean required() {
        return required;
    }

    public int order() {
        return order;
    }

    public String[] alias() {
        return alias;
    }

    public Field field() {
        return field;
    }

    public static PropertyInstance from(Field field, Property annotation, boolean required) {
        if (annotation == null) return null;
        return new PropertyInstance(
                field.getName(), required, annotation.order(), annotation.alias(), field);
    }
}
