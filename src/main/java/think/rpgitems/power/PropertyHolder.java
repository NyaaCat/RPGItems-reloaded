package think.rpgitems.power;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import think.rpgitems.item.RPGItem;

import java.util.Locale;

public interface PropertyHolder {
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
     * Static. NamespacedKey of this power
     *
     * @return NamespacedKey
     */
    NamespacedKey getNamespacedKey();

    /**
     * @return Item it belongs to
     */
    RPGItem getItem();

    void setItem(RPGItem item);


    /**
     * Static. Code name of this power
     *
     * @return Code name
     */
    String getName();

    /**
     * Static. Localized name of this power
     *
     * @param locale Locale tag
     * @return Localized name
     */
    default String getLocalizedName(String locale) {
        return getName();
    }

    /**
     * Static. Localized name of this power
     *
     * @param locale Locale
     * @return Localized name
     */
    default String getLocalizedName(Locale locale) {
        return getLocalizedName(locale.toString());
    }
}
