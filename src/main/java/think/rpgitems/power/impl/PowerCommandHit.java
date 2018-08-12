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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerHit;

import static think.rpgitems.utils.PowerUtils.AttachPermission;
import static think.rpgitems.utils.PowerUtils.checkCooldownByString;


/**
 * Power commandhit.
 * <p>
 * The item will run {@link #command} when player hits some {@link LivingEntity}
 * giving the permission {@link #permission} just for the use of the command.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public class PowerCommandHit extends BasePower implements PowerHit {

    /**
     * Command to be executed
     */
    @Property(order = 2, required = true)
    public String command = "";
    /**
     * Display text of this power
     */
    @Property(order = 1)
    public String display = "Runs command";
    /**
     * Permission will be given to user executing the {@link #command}
     */
    @Property(order = 3)
    public String permission = "";
    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;
    /**
     * Minimum damage to trigger
     */
    @Property
    public double minDamage = 0;

    /**
     * Execute command
     *
     * @param player player
     * @param e      entity
     */
    protected void executeCommand(Player player, LivingEntity e) {
        if (!player.isOnline()) return;

        AttachPermission(player, permission);
        boolean wasOp = player.isOp();
        if (permission.equals("*"))
            player.setOp(true);

        String cmd = command;

        cmd = cmd.replaceAll("\\{entity}", e.getName());
        cmd = cmd.replaceAll("\\{entity.uuid}", e.getUniqueId().toString());
        cmd = cmd.replaceAll("\\{entity.x}", Float.toString(e.getLocation().getBlockX()));
        cmd = cmd.replaceAll("\\{entity.y}", Float.toString(e.getLocation().getBlockY()));
        cmd = cmd.replaceAll("\\{entity.z}", Float.toString(e.getLocation().getBlockZ()));
        cmd = cmd.replaceAll("\\{entity.yaw}", Float.toString(90 + e.getEyeLocation().getYaw()));
        cmd = cmd.replaceAll("\\{entity.pitch}", Float.toString(-e.getEyeLocation().getPitch()));

        cmd = cmd.replaceAll("\\{player}", player.getName());
        cmd = cmd.replaceAll("\\{player.x}", Float.toString(-player.getLocation().getBlockX()));
        cmd = cmd.replaceAll("\\{player.y}", Float.toString(-player.getLocation().getBlockY()));
        cmd = cmd.replaceAll("\\{player.z}", Float.toString(-player.getLocation().getBlockZ()));
        cmd = cmd.replaceAll("\\{player.yaw}", Float.toString(90 + player.getEyeLocation().getYaw()));
        cmd = cmd.replaceAll("\\{player.pitch}", Float.toString(-player.getEyeLocation().getPitch()));
        Bukkit.getServer().dispatchCommand(player, cmd);
        if (permission.equals("*"))
            player.setOp(wasOp);
    }

    @Override
    public void hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (damage < minDamage) return;
        if (!checkCooldownByString(player, getItem(), command, cooldown, true)) return;
        if (!getItem().consumeDurability(stack, consumption)) return;

        executeCommand(player, entity);
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + display;
    }

    @Override
    public String getName() {
        return "commandhit";
    }
}
