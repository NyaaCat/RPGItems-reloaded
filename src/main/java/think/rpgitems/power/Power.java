package think.rpgitems.power;

import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import think.rpgitems.RPGItems;
import think.rpgitems.power.trigger.Trigger;

/** Base interface for all powers */
public interface Power extends PropertyHolder, TagHolder {

    /**
     * Display name of this power
     *
     * @return Display name
     */
    @Nullable
    String displayName();

    /**
     * Display name or default name of this power
     *
     * @return Display name or default name
     */
    default String getLocalizedDisplayName() {
        return Strings.isNullOrEmpty(displayName())
                ? getLocalizedName(RPGItems.plugin.cfg.language)
                : displayName();
    }

    /**
     * Display text of this power
     *
     * @return Display text
     */
    String displayText();

    /**
     * Localized name of this power
     *
     * @param locale Locale tag
     * @return Localized name
     */
    default String localizedDisplayText(String locale) {
        return displayText();
    }

    /**
     * Localized name of this power
     *
     * @param locale Locale tag
     * @return Localized name
     */
    default String localizedDisplayText(Locale locale) {
        return localizedDisplayText(locale.toLanguageTag());
    }

    Set<Trigger> getTriggers();

    Set<String> getSelectors();

    Set<String> getConditions();

    String requiredContext();

    default void deinit() {}

    static Set<Trigger> getTriggers(Class<? extends Pimpl> cls) {
        return getDynamicInterfaces(cls).stream()
                .flatMap(Trigger::fromInterface)
                .collect(Collectors.toSet());
    }

    /**
     * @param cls Class of Power
     * @return All static implemented interfaces
     */
    @SuppressWarnings("unchecked")
    static Set<Class<? extends Pimpl>> getStaticInterfaces(Class<? extends Pimpl> cls) {
        return TypeToken.of(cls).getTypes().interfaces().stream()
                .map(TypeToken::getRawType)
                .filter(Pimpl.class::isAssignableFrom)
                .filter(i -> !Objects.equals(i, Pimpl.class))
                .map(i -> (Class<? extends Pimpl>) i)
                .collect(Collectors.toSet());
    }

    /**
     * @param cls Class of Power
     * @return All static and dynamic implemented interfaces
     */
    static Set<Class<? extends Pimpl>> getDynamicInterfaces(Class<? extends Pimpl> cls) {
        return getStaticInterfaces(cls).stream()
                .flatMap(
                        i ->
                                Stream.concat(
                                        Stream.of(i),
                                        PowerManager.adapters.row(i).keySet().stream()))
                .collect(Collectors.toSet());
    }

    static Set<Trigger> getDefaultTriggers(Class<? extends Pimpl> cls) {
        cls = getUserClass(cls);
        Meta annotation = Objects.requireNonNull(cls.getAnnotation(Meta.class));
        if (annotation.defaultTrigger().length > 0) {
            return Trigger.valueOf(annotation.defaultTrigger());
        }
        if (annotation.marker()) {
            return Collections.emptySet();
        }

        return getTriggers(annotation.implClass());
    }

    String CGLIB_CLASS_SEPARATOR = "$$";

    @SuppressWarnings("unchecked")
    static <T> Class<T> getUserClass(Class<T> clazz) {
        if (clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
            Class<T> superclass = (Class<T>) clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return superclass;
            }
        }
        return clazz;
    }
}
