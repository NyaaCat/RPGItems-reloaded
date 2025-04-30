package think.rpgitems.power.impl;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import java.util.HashMap;

import static think.rpgitems.power.Utils.checkAndSetCooldown;

/**
 * Power delayedcommand.
 * <p>
 * The item will run {@link #getCommand()} on click with a {@link #delay}
 * giving the permission {@link #getPermission()} just for the use of the command.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = DelayedCommand.Impl.class)
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

    public class Impl extends Command.Impl implements PowerRightClick, PowerLeftClick, PowerSprint, PowerSneak, PowerHurt, PowerHitTaken, PowerPlain, PowerBowShoot, PowerJump, PowerSwim, PowerConsume {
        @Override
        public PowerResult<Void> leftClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player target, ItemStack stack) {
            String cmd;
            if (!cmdInPlace) {
                cmd = handlePlayerPlaceHolder(target, getCommand());
            } else {
                cmd = null;
            }
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("command",cmd);
            PowerActivateEvent powerEvent = new PowerActivateEvent(target,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkAndSetCooldown(getPower(), target, getCooldown(), showCooldownWarning(), false, getItem().getUid() + "." + getCommand())) {
                return PowerResult.cd();
            }
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            (new BukkitRunnable() {
                @Override
                public void run() {
                    if (cmd == null) {
                        executeCommand(target);
                    } else {
                        executeCommand(target, cmd);
                    }
                }
            }).runTaskLater(RPGItems.plugin, getDelay());
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return DelayedCommand.this;
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
        @Override
        public PowerResult<Void> consume(Player player, ItemStack stack, PlayerItemConsumeEvent event) {
            return fire(player,stack);
        }

        @Override
        public PowerResult<Void> jump(Player player, ItemStack stack, PlayerJumpEvent event) {
            return fire(player,stack);
        }

        @Override
        public PowerResult<Void> swim(Player player, ItemStack stack, EntityToggleSwimEvent event) {
            return fire(player,stack);
        }
    }
}
