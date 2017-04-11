package think.rpgitems.power;

import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.Plugin;

/**
 * Power delayedcommand.
 * <p>
 * The item will run {@link #command} on {@link #isRight click} with a {@link #delay}
 * giving the permission {@link #permission} just for the use of the command.
 * </p>
 */
public class PowerDelayedCommand extends PowerCommand {
    /**
     * Delay before executing command
     */
    public int delay = 20;

    @Override
    public String getName() {
        return "delayedcommand";
    }

    @Override
    public void init(ConfigurationSection s) {
        delay = s.getInt("delay", 20);
        super.init(s);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("delay", delay);
        super.save(s);
    }

    @Override
    public void rightClick(final Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true))return;
        if (!isRight || !checkCooldownByString(player, item, command, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        (new BukkitRunnable() {
            @Override
            public void run() {
                executeCommand(player);
            }
        }).runTaskLater(Plugin.plugin, delay);
    }

    @Override
    public void leftClick(final Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true))return;
        if (isRight || !checkCooldownByString(player, item, command, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        (new BukkitRunnable() {
            @Override
            public void run() {
                executeCommand(player);
            }
        }).runTaskLater(Plugin.plugin, delay);
    }
}
