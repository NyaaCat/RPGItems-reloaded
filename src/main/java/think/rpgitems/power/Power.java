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

    @SuppressWarnings("unchecked")
    static Set<Trigger> getTriggers(Class<? extends Power> cls) {
        return TypeToken.of(cls).getTypes().interfaces().stream()
                        .map(TypeToken::getRawType)
                        .filter(Power.class::isAssignableFrom)
                        .filter(i -> !Objects.equals(i, Power.class))
                        .flatMap(i -> Trigger.fromInterface((Class<? extends Power>) i))
                        .collect(Collectors.toSet());
    }

    static Set<Trigger> getDefaultTriggers(Class<? extends Power> cls) {
        PowerMeta annotation = cls.getAnnotation(PowerMeta.class);
        if (annotation != null) {
            if (annotation.defaultTrigger().length > 0) {
                return Trigger.valueOf(annotation.defaultTrigger());
            }
            if (annotation.marker()) {
                return Collections.emptySet();
            }
        }
        return getTriggers(cls);
    }
}
