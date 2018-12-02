package think.rpgitems.power.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;

import static think.rpgitems.power.Utils.checkCooldownByString;

/**
 * Power delayedcommand.
 * <p>
 * The item will run {@link #command} on click with a {@link #delay}
 * giving the permission {@link #permission} just for the use of the command.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = "RIGHT_CLICK")
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
    public PowerResult<Void> rightClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> leftClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
        return fire(target, stack);
    }

    @Override
    public PowerResult<Void> fire(Player target, ItemStack stack) {
        if (!checkCooldownByString(this, target, command, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        (new BukkitRunnable() {
            @Override
            public void run() {
                executeCommand(target);
            }
        }).runTaskLater(RPGItems.plugin, delay);
        return PowerResult.ok();
    }
}
