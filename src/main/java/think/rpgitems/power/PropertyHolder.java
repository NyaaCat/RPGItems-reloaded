package think.rpgitems.power;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import think.rpgitems.item.RPGItem;

import java.util.Locale;

public interface PropertyHolder {
    /**
     * Loads configuration for this object
     *
     * @param s Configuration
     */
    void init(ConfigurationSection s);

    /**
     * Saves configuration for this object
     *
     * @param s Configuration
     */
    void save(ConfigurationSection s);

    /**
     * Static. NamespacedKey of this object
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
     * Static. Code name of this object
     *
     * @return Code name
     */
    String getName();

    /**
     * Static. Type of this object
     *
     * @return Type
     */
    String getPropertyHolderType();

    /**
     * Static. Localized name of this object
     *
     * @param locale Locale tag
     * @return Localized name
     */
    default String getLocalizedName(String locale) {
        return getName();
    }

    /**
     * Static. Localized name of this object
     *
     * @param locale Locale
     * @return Localized name
     */
    default String getLocalizedName(Locale locale) {
        return getLocalizedName(locale.toString());
    }
}
