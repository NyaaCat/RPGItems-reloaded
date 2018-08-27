/*
 *  This file is part of RPG Items.
 *
 *  RPG Items is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RPG Items is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RPG Items.  If not, see <http://www.gnu.org/licenses/>.
 */
package think.rpgitems.power.impl;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import java.util.Collections;

import static think.rpgitems.utils.PowerUtils.AttachPermission;
import static think.rpgitems.utils.PowerUtils.checkCooldownByString;

/**
 * Power command.
 * <p>
 * The item will run {@link #command} on click
 * giving the permission {@link #permission} just for the use of the command.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = TriggerType.RIGHT_CLICK)
public class PowerCommand extends BasePower implements PowerRightClick, PowerLeftClick, PowerSprint, PowerSneak, PowerHurt {

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

    @Override
    public void init(ConfigurationSection section) {
        boolean isRight = section.getBoolean("isRight", true);
        triggers = Collections.singleton(isRight ? TriggerType.RIGHT_CLICK : TriggerType.LEFT_CLICK);
        super.init(section);
    }

    /**
     * Execute command
     *
     * @param player player
     */
    protected PowerResult<Void> executeCommand(Player player) {
        if (!player.isOnline()) return PowerResult.noop();

        AttachPermission(player, permission);
        boolean wasOp = player.isOp();

        Runnable run = () -> {
            String cmd = command;
            cmd = cmd.replaceAll("\\{player}", player.getName());
            cmd = cmd.replaceAll("\\{player.x}", Float.toString(-player.getLocation().getBlockX()));
            cmd = cmd.replaceAll("\\{player.y}", Float.toString(-player.getLocation().getBlockY()));
            cmd = cmd.replaceAll("\\{player.z}", Float.toString(-player.getLocation().getBlockZ()));
            cmd = cmd.replaceAll("\\{yaw}", Float.toString(player.getLocation().getYaw() + 90));
            cmd = cmd.replaceAll("\\{pitch}", Float.toString(-player.getLocation().getPitch()));
            player.performCommand(cmd);
        };

        if (permission.equals("*")) {
            try {
                player.setOp(true);
                run.run();
            } finally {
                if (!wasOp) {
                    player.setOp(false);
                }
            }
        } else {
            run.run();
        }
        return PowerResult.ok();
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldownByString(player, getItem(), command, cooldown, true))
            return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        return executeCommand(player);
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldownByString(player, getItem(), command, cooldown, true))
            return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        return executeCommand(player);
    }

    @Override
    public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
        if (!checkCooldownByString(player, getItem(), command, cooldown, true))
            return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        return executeCommand(player);
    }

    @Override
    public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
        if (!checkCooldownByString(player, getItem(), command, cooldown, true))
            return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        return executeCommand(player);
    }

    @Override
    public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
        if (!checkCooldownByString(target, getItem(), command, cooldown, true))
            return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        return executeCommand(target);
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + display;
    }

    @Override
    public String getName() {
        return "command";
    }
}
