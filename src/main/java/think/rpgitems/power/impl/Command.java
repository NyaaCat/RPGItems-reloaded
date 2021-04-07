package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.BaseTriggers;

import java.util.Collections;

import static think.rpgitems.power.Utils.attachPermission;

/**
 * Power command.
 * <p>
 * The item will run {@link #command} on click
 * giving the permission {@link #permission} just for the use of the command.
 * </p>
 */
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = Command.Impl.class)
public class Command extends BasePower {

    @Property(order = 4, required = true)
    public String command = "";
    @Property(order = 3)
    public String display = "Runs command";
    @Property(order = 8)
    public String permission = "";

    @Property
    public boolean requireHurtByEntity = true;

    public static String handlePlayerPlaceHolder(Player player, String cmd) {
        cmd = cmd.replaceAll("\\{player}", player.getName());
        cmd = cmd.replaceAll("\\{player\\.x}", Double.toString(player.getLocation().getX()));
        cmd = cmd.replaceAll("\\{player\\.y}", Double.toString(player.getLocation().getY()));
        cmd = cmd.replaceAll("\\{player\\.z}", Double.toString(player.getLocation().getZ()));
        cmd = cmd.replaceAll("\\{player\\.yaw}", Float.toString(90 + player.getEyeLocation().getYaw()));
        cmd = cmd.replaceAll("\\{player\\.pitch}", Float.toString(-player.getEyeLocation().getPitch()));
        cmd = cmd.replaceAll("\\{yaw}", Float.toString(player.getLocation().getYaw() + 90));
        cmd = cmd.replaceAll("\\{pitch}", Float.toString(-player.getLocation().getPitch()));
        return cmd;
    }

    @Override
    public void init(ConfigurationSection section) {
        if (section.isBoolean("isRight")) {
            boolean isRight = section.getBoolean("isRight", true);
            triggers = Collections.singleton(isRight ? BaseTriggers.RIGHT_CLICK : BaseTriggers.LEFT_CLICK);
        }
        super.init(section);
    }

    /**
     * Command to be executed
     */
    public String getCommand() {
        return command;
    }

    @Override
    public String getName() {
        return "command";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + getDisplay();
    }

    /**
     * Display text of this power
     */
    public String getDisplay() {
        return display;
    }

    /**
     * Permission will be given to user executing the {@code command}
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Whether to require hurt by entity for HURT trigger
     */
    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public static class Impl extends Base implements PowerRightClick<Command>, PowerLeftClick<Command>, PowerSprint<Command>, PowerSneak<Command>, PowerHurt<Command>, PowerHitTaken<Command>, PowerPlain<Command>, PowerBowShoot<Command> {
        @Override
        public PowerResult<Void> leftClick(Command power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(Command power, Player target, ItemStack stack) {
            return executeCommand(power, target);
        }

        @Override
        public Class<? extends Command> getPowerClass() {
            return Command.class;
        }

        @Override
        public PowerResult<Void> sneak(Command power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sprint(Command power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> hurt(Command power, Player target, ItemStack stack, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Double> takeHit(Command power, Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Float> bowShoot(Command power, Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(power, player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Void> rightClick(Command power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }
    }

    public static class Base {
        /**
         * Execute command
         *
         * @param player player
         * @return PowerResult
         */
        protected PowerResult<Void> executeCommand(Command power, Player player) {
            String cmd = Command.handlePlayerPlaceHolder(player, power.getCommand());
            return executeCommand(power, player, cmd);
        }

        protected PowerResult<Void> executeCommand(Command power, Player player, String cmd) {
            if (!player.isOnline()) return PowerResult.noop();

            if (power.getPermission().equals("console")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else {
                boolean wasOp = player.isOp();
                attachPermission(player, power.getPermission());
                if (power.getPermission().equals("*")) {
                    try {
                        player.setOp(true);
                        player.performCommand(cmd);
                    } finally {
                        if (!wasOp) {
                            player.setOp(false);
                        }
                    }
                } else {
                    player.performCommand(cmd);
                }
            }
            return PowerResult.ok();
        }
    }

}
