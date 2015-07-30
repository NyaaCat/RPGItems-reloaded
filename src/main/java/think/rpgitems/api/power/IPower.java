package think.rpgitems.api.power;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Interface represents a power
 * Instances contains different parameters for different weapons.
 * You should *NOT* implement this interface directly.
 */
public interface IPower {
    /**
     * The name of the power, should be a constant.
     * will be used in code.
     *
     * @return name of the power
     */
    String getPowerName();

    /**
     * Load parameters from configure.
     * Also fill with default value if not found.
     *
     * @param s the configure section
     */
    void load(ConfigurationSection s);

    /**
     * Write current parameters into configure.
     *
     * @param s the configure section to write to.
     */
    void save(ConfigurationSection s);

    /**
     * Invoked when power triggered if item is neither single-use nor unbreakable.
     *
     * @param full    full duration of the item
     * @param current current duration remaining
     * @return duration should consume this time.
     */
    Double consumeDurability(Double full, Double current);

    /**
     * Parse arguments of 'add power' command and save to instance.
     * return
     *
     * @param sender sender of the command
     * @param cmd    the command
     * @return lang string
     */
    String parseCommand(CommandSender sender, String[] cmd);
}
