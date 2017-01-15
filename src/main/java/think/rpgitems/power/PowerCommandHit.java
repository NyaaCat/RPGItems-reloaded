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
package think.rpgitems.power;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import think.rpgitems.Plugin;
import think.rpgitems.power.types.PowerHit;

import java.util.Set;

public class PowerCommandHit extends Power implements PowerHit {

    public String command = "";
    public String display = "Runs command";
    public String permission = "";

    protected void executeCommand(Player player, LivingEntity e) {
        if (!player.isOnline()) return;

        if (permission.length() != 0 && !permission.equals("*")) {
            PermissionAttachment attachment = player.addAttachment(Plugin.plugin, 10);
            String[] perms = permission.split("\\.");
            StringBuilder p = new StringBuilder();
            for (int i = 0; i < perms.length; i++) {
                p.append(perms[i]);
                attachment.setPermission(p.toString(), true);
                p.append('.');
            }
        }
        boolean wasOp = player.isOp();
        if (permission.equals("*"))
            player.setOp(true);

        String cmd = command;

        cmd = cmd.replaceAll("\\{e\\}", e.getName());
        cmd = cmd.replaceAll("\\{e.uuid\\}", e.getUniqueId().toString());
        cmd = cmd.replaceAll("\\{e.x\\}", Float.toString(e.getLocation().getBlockX()));
        cmd = cmd.replaceAll("\\{e.y\\}", Float.toString(e.getLocation().getBlockY()));
        cmd = cmd.replaceAll("\\{e.z\\}", Float.toString(e.getLocation().getBlockZ()));
        cmd = cmd.replaceAll("\\{e.yaw\\}", Float.toString(e.getLocation().getYaw()+90));
        cmd = cmd.replaceAll("\\{e.pitch\\}", Float.toString(-e.getLocation().getPitch()));

        cmd = cmd.replaceAll("\\{p\\}", player.getName());
        cmd = cmd.replaceAll("\\{p.x\\}", Float.toString(-player.getLocation().getBlockX()));
        cmd = cmd.replaceAll("\\{p.y\\}", Float.toString(-player.getLocation().getBlockY()));
        cmd = cmd.replaceAll("\\{p.z\\}", Float.toString(-player.getLocation().getBlockZ()));
        cmd = cmd.replaceAll("\\{p.yaw\\}", Float.toString(player.getLocation().getYaw()+90));
        cmd = cmd.replaceAll("\\{p.pitch\\}", Float.toString(-player.getLocation().getPitch()));
        Bukkit.getServer().dispatchCommand(player, cmd);
        if (permission.equals("*"))
            player.setOp(wasOp);
    }

    @Override
    public void hit(Player player, LivingEntity e, double damage) {
        if (item.getHasPermission() && !player.hasPermission(item.getPermission())) return;
        executeCommand(player, e);


    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + display;
    }

    @Override
    public String getName() {
        return "commandhit";
    }

    @Override
    public void init(ConfigurationSection s) {
        command = s.getString("command", "");
        display = s.getString("display", "");
        permission = s.getString("permission", "");
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("command", command);
        s.set("display", display);
        s.set("permission", permission);
    }
}
