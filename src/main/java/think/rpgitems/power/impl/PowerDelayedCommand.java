package think.rpgitems.power.impl;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;
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
@PowerMeta(defaultTrigger = TriggerType.RIGHT_CLICK)
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
        if (!checkCooldownByString(player, getItem(), command, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
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
        if (!checkCooldownByString(player, getItem(), command, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        (new BukkitRunnable() {
            @Override
            public void run() {
                executeCommand(player);
            }
        }).runTaskLater(RPGItems.plugin, delay);
        return PowerResult.ok();
    }
}
