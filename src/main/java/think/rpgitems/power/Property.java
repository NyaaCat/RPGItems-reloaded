package think.rpgitems.power;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Property {
    int order() default Integer.MAX_VALUE;

    boolean required() default false;
}
