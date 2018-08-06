package think.rpgitems.power;

import org.bukkit.configuration.ConfigurationSection;
import think.rpgitems.item.RPGItem;

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
     * Name of this power
     *
     * @return name
     */
    String getName();

    /**
     * Display text of this power
     *
     * @return Display text
     */
    String displayText();

    /**
     * Item it belongs to
     */
    RPGItem getItem();

    void setItem(RPGItem item);
}
