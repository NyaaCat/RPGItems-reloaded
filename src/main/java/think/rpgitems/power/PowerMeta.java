package think.rpgitems.power;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PowerMeta {
    Class<? extends Pimpl> implClass() default Pimpl.class;

    boolean immutableTrigger() default false;

    String[] defaultTrigger() default {};

    boolean marker() default false;

    boolean withSelectors() default false;

    boolean withConditions() default false;

    Class<? extends Pimpl>[] generalInterface() default {};
}
