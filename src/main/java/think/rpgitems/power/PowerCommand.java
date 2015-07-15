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

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerLeftClick;
import think.rpgitems.power.types.PowerRightClick;

public class PowerCommand extends Power implements PowerRightClick, PowerLeftClick {

    public String command = "";
    public String display = "Runs command";
    public String permission = "";
    public boolean isRight = true;
    public long cooldownTime = 20;

    @Override
    public void rightClick(Player player) {
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false){
        }else{  
    	if (isRight) {
            long cooldown;
            RPGValue value = RPGValue.get(player, item, "command." + command + ".cooldown");
            if (value == null) {
                cooldown = System.currentTimeMillis() / 50;
                value = new RPGValue(player, item, "command." + command + ".cooldown", cooldown);
            } else {
                cooldown = value.asLong();
            }
            if (cooldown <= System.currentTimeMillis() / 50) {
                value.set(System.currentTimeMillis() / 50 + cooldownTime);
                if (permission.length() != 0 && !permission.equals("*")) {
                    PermissionAttachment attachment = player.addAttachment(Plugin.plugin, 1);
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
                player.chat("/" + command.replaceAll("\\{player\\}", player.getName()));
                if (permission.equals("*"))
                    player.setOp(wasOp);
            } else {
                player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown", Locale.getPlayerLocale(player)), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
            }
        }
        }
    }

    @Override
    public void leftClick(Player player) {
        if (!isRight) {
            long cooldown;
            RPGValue value = RPGValue.get(player, item, "command." + command + ".cooldown");
            if (value == null) {
                cooldown = System.currentTimeMillis() / 50;
                value = new RPGValue(player, item, "command." + command + ".cooldown", cooldown);
            } else {
                cooldown = value.asLong();
            }
            if (cooldown <= System.currentTimeMillis() / 50) {
                value.set(System.currentTimeMillis() / 50 + cooldownTime);
                if (permission.length() != 0 && !permission.equals("*")) {
                    PermissionAttachment attachment = player.addAttachment(Plugin.plugin, 1);
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
                player.chat("/" + command.replaceAll("\\{player\\}", player.getName()));
                if (permission.equals("*"))
                    player.setOp(wasOp);
            } else {
                player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown", Locale.getPlayerLocale(player)), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
            }
        }
    }

    @Override
    public String displayText(String locale) {
        return ChatColor.GREEN + display;
    }

    @Override
    public String getName() {
        return "command";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
        command = s.getString("command", "");
        display = s.getString("display", "");
        isRight = s.getBoolean("isRight", true);
        permission = s.getString("permission", "");
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("command", command);
        s.set("display", display);
        s.set("isRight", isRight);
        s.set("permission", permission);
    }
}
