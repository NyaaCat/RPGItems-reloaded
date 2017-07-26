package think.rpgitems.commands;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Property {
    int order() default Integer.MAX_VALUE;

    boolean required() default false;
}
