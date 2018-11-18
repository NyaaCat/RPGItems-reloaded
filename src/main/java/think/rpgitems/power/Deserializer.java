package think.rpgitems.power;

import org.librazy.nclangchecker.LangKey;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Deserializer {
    Class<? extends Setter> value();

    @LangKey String message() default "";
}
