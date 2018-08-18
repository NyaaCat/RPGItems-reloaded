package think.rpgitems.power.impl;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.TriggerType;

import static think.rpgitems.utils.PowerUtils.checkCooldownByString;

/**
 * Power delayedcommand.
 * <p>
 * The item will run {@link #command} on click with a {@link #delay}
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
    public PowerResult<Void> rightClick(final Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!triggers.contains(TriggerType.RIGHT_CLICK) || !checkCooldownByString(player, getItem(), command, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, consumption)) return PowerResult.cost();
        (new BukkitRunnable() {
            @Override
            public void run() {
                executeCommand(player);
            }
        }).runTaskLater(RPGItems.plugin, delay);
        return PowerResult.ok();
    }

    @Override
    public PowerResult<Void> leftClick(final Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!triggers.contains(TriggerType.LEFT_CLICK) || !checkCooldownByString(player, getItem(), command, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, consumption)) return PowerResult.cost();
        (new BukkitRunnable() {
            @Override
            public void run() {
                executeCommand(player);
            }
        }).runTaskLater(RPGItems.plugin, delay);
        return PowerResult.ok();
    }
}
