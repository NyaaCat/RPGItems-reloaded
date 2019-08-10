package think.rpgitems.power.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;

import static think.rpgitems.power.Utils.checkAndSetCooldown;

/**
 * Power delayedcommand.
 * <p>
 * The item will run {@link #getCommand()} on click with a {@link #delay}
 * giving the permission {@link #getPermission()} just for the use of the command.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = "RIGHT_CLICK", implClass = PowerDelayedCommand.Impl.class)
public class PowerDelayedCommand extends PowerCommand {
    @Property(order = 0)
    public int delay = 20;

    /**
     * Delay before executing command
     */
    public int getDelay() {
        return delay;
    }

    @Override
    public String getName() {
        return "delayedcommand";
    }

    public class Impl extends PowerCommand.Impl {
        @Override
        public PowerResult<Void> leftClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player target, ItemStack stack) {
            if (!checkAndSetCooldown(getPower(), target, getCooldown(), true, false, getCommand()))
                return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            (new BukkitRunnable() {
                @Override
                public void run() {
                    executeCommand(target);
                }
            }).runTaskLater(RPGItems.plugin, getDelay());
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return PowerDelayedCommand.this;
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
        public PowerResult<Void> rightClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }
    }
}
