package think.rpgitems.power.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;


/**
 * Power delayedcommand.
 * <p>
 * The item will run {@link #getCommand()} on click with a {@link #delay}
 * giving the permission {@link #getPermission()} just for the use of the command.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", implClass = DelayedCommand.Impl.class)
public class DelayedCommand extends Command {
    @Property(order = 0)
    public int delay = 20;

    @Property
    public boolean cmdInPlace = false;

    /**
     * Delay before executing command
     */
    public int getDelay() {
        return delay;
    }

    public boolean isCmdInPlace() {
        return cmdInPlace;
    }

    @Override
    public String getName() {
        return "delayedcommand";
    }

    public static class Impl extends Command.Base implements PowerRightClick<DelayedCommand>, PowerLeftClick<DelayedCommand>, PowerSprint<DelayedCommand>, PowerSneak<DelayedCommand>, PowerHurt<DelayedCommand>, PowerPlain<DelayedCommand> {
        @Override
        public PowerResult<Void> leftClick(DelayedCommand power, final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(DelayedCommand power, Player target, ItemStack stack) {
            String cmd;
            if (!power.isCmdInPlace()) {
                cmd = handlePlayerPlaceHolder(target, power.getCommand());
            }else {
                cmd = null;
            }
            (new BukkitRunnable() {
                @Override
                public void run() {
                    if (cmd == null){
                        executeCommand(power, target);
                    }else {
                        executeCommand(power, target, cmd);
                    }
                }
            }).runTaskLater(RPGItems.plugin, power.getDelay());
            return PowerResult.ok();
        }

        @Override
        public Class<? extends DelayedCommand> getPowerClass() {
            return DelayedCommand.class;
        }

        @Override
        public PowerResult<Void> sneak(DelayedCommand power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sprint(DelayedCommand power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> hurt(DelayedCommand power, Player target, ItemStack stack, EntityDamageEvent event) {
            return fire(power, target, stack);
        }

        @Override
        public PowerResult<Void> rightClick(DelayedCommand power, final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }
    }
}
