package think.rpgitems.power;

import com.google.common.reflect.TypeToken;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.item.RPGItem;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base interface for all powers
 */
public interface Power {
    /**
     * Loads configuration for this power
     *
     * @param s Configuration
     */
    void init(ConfigurationSection s);

    /**
     * Saves configuration for this power
     *
     * @param s Configuration
     */
    void save(ConfigurationSection s);

    /**
     * NamespacedKey of this power
     *
     * @return NamespacedKey
     */
    NamespacedKey getNamespacedKey();

    /**
     * Code name of this power
     *
     * @return Code name
     */
    @LangKey(skipCheck = true) String getName();

    /**
     * Localized name of this power
     *
     * @param locale Locale tag
     * @return Localized name
     */
    default String getLocalizedName(String locale) {
        return getName();
    }

    /**
     * Localized name of this power
     *
     * @param locale Locale
     * @return Localized name
     */
    default String getLocalizedName(Locale locale) {
        return getLocalizedName(locale.toLanguageTag());
    }

    /**
     * Display name of this power
     *
     * @return Display name
     */
    String displayName();

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

    /**
     * @return Item it belongs to
     */
    RPGItem getItem();

    void setItem(RPGItem item);

    Set<Trigger> getTriggers();

    Set<String> getSelectors();

    Set<String> getConditions();

    default void deinit() {
    }

    static Set<Trigger> getTriggers(Class<? extends Pimpl> cls) {
        return getDynamicInterfaces(cls)
                       .stream()
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
        return getStaticInterfaces(cls)
                       .stream()
                       .flatMap(i -> Stream.concat(
                               Stream.of(i),
                               PowerManager.adapters.row(i).keySet().stream()
                       ))
                       .collect(Collectors.toSet());
    }

    static Set<Trigger> getDefaultTriggers(Class<? extends Power> cls) {
        cls = getUserClass(cls);
        PowerMeta annotation = Objects.requireNonNull(cls.getAnnotation(PowerMeta.class));
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
