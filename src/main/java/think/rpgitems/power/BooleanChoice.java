package think.rpgitems.power;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BooleanChoice {
    String name() default "";

    String falseChoice() default "false";

    String trueChoice() default "true";
}
