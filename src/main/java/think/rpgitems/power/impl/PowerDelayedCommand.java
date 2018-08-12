package think.rpgitems.power.impl;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.Property;

import static think.rpgitems.utils.PowerUtils.checkCooldownByString;

/**
 * Power delayedcommand.
 * <p>
 * The item will run {@link #command} on {@link #isRight click} with a {@link #delay}
 * giving the permission {@link #permission} just for the use of the command.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public class PowerDelayedCommand extends PowerCommand {
    /**
     * Delay before executing command
     */
    @Property(order = 0)
    public int delay = 20;

    @Override
    public String getName() {
        return "delayedcommand";
    }

    @Override
    public void rightClick(final Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!isRight || !checkCooldownByString(player, getItem(), command, cooldown, true)) return;
        if (!getItem().consumeDurability(stack, consumption)) return;
        (new BukkitRunnable() {
            @Override
            public void run() {
                executeCommand(player);
            }
        }).runTaskLater(RPGItems.plugin, delay);
    }

    @Override
    public void leftClick(final Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (isRight || !checkCooldownByString(player, getItem(), command, cooldown, true)) return;
        if (!getItem().consumeDurability(stack, consumption)) return;
        (new BukkitRunnable() {
            @Override
            public void run() {
                executeCommand(player);
            }
        }).runTaskLater(RPGItems.plugin, delay);
    }
}
