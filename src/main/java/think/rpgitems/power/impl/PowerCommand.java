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

import java.util.Collections;

import static think.rpgitems.power.Utils.attachPermission;
import static think.rpgitems.power.Utils.checkCooldownByString;

/**
 * Power command.
 * <p>
 * The item will run {@link #command} on click
 * giving the permission {@link #permission} just for the use of the command.
 * </p>
 */
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class)
public class PowerCommand extends BasePower implements PowerRightClick, PowerLeftClick, PowerSprint, PowerSneak, PowerHurt, PowerHitTaken, PowerPlain, PowerBowShoot {

    /**
     * Command to be executed
     */
    @Property(order = 4, required = true)
    public String command = "";
    /**
     * Display text of this power
     */
    @Property(order = 3)
    public String display = "Runs command";
    /**
     * Permission will be given to user executing the {@code command}
     */
    @Property(order = 8)
    public String permission = "";
    /**
     * Cooldown time of this power
     */
    @Property(order = 1)
    public long cooldown = 20;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    /**
     * Whether to require hurt by entity for HURT trigger
     */
    @Property
    public boolean requireHurtByEntity = true;

    @Override
    public void init(ConfigurationSection section) {
        if (section.isBoolean("isRight")) {
            boolean isRight = section.getBoolean("isRight", true);
            triggers = Collections.singleton(isRight ? Trigger.RIGHT_CLICK : Trigger.LEFT_CLICK);
        }
        super.init(section);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> fire(Player target, ItemStack stack) {
        if (!checkCooldownByString(this, target, command, cooldown, true, false))
            return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        return executeCommand(target);
    }

    /**
     * Execute command
     *
     * @param player player
     * @return PowerResult
     */
    protected PowerResult<Void> executeCommand(Player player) {
        if (!player.isOnline()) return PowerResult.noop();

        String cmd = handlePlayerPlaceHolder(player, command);
        if (permission.equals("console")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else {
            boolean wasOp = player.isOp();
            attachPermission(player, permission);
            if (permission.equals("*")) {
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

    public static String handlePlayerPlaceHolder(Player player, String cmd) {
        cmd = cmd.replaceAll("\\{player}", player.getName());
        cmd = cmd.replaceAll("\\{player\\.x}", Float.toString(-player.getLocation().getBlockX()));
        cmd = cmd.replaceAll("\\{player\\.y}", Float.toString(-player.getLocation().getBlockY()));
        cmd = cmd.replaceAll("\\{player\\.z}", Float.toString(-player.getLocation().getBlockZ()));
        cmd = cmd.replaceAll("\\{player\\.yaw}", Float.toString(90 + player.getEyeLocation().getYaw()));
        cmd = cmd.replaceAll("\\{player\\.pitch}", Float.toString(-player.getEyeLocation().getPitch()));
        cmd = cmd.replaceAll("\\{yaw}", Float.toString(player.getLocation().getYaw() + 90));
        cmd = cmd.replaceAll("\\{pitch}", Float.toString(-player.getLocation().getPitch()));
        return cmd;
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
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
        if (!requireHurtByEntity || event instanceof EntityDamageByEntityEvent) {
            return fire(target, stack);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
        if (!requireHurtByEntity || event instanceof EntityDamageByEntityEvent) {
            return fire(target, stack).with(damage);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
        return fire(player, stack).with(event.getForce());
    }

    @Override
    public String getName() {
        return "command";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + display;
    }
}
