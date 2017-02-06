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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import org.bukkit.inventory.ItemStack;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerHitTaken;

public class PowerRescue extends Power implements PowerHitTaken {

    public String permission = "";
    public int healthTrigger = 4;
    public boolean useBed = true;
    public long cooldownTime = 20;
    public int consumption = 0;

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.rescue"), ((double) healthTrigger) / 2,(double) cooldownTime / 20d);
    }

    @Override
    public String getName() {
        return "rescue";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
        healthTrigger = s.getInt("healthTrigger", 4);
        useBed = s.getBoolean("useBed", true);
        permission = s.getString("permission", "");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("healthTrigger", healthTrigger);
        s.set("useBed", useBed);
        s.set("permission", permission);
        s.set("consumption", consumption);
    }

    @Override
    public double takeHit(Player target, ItemStack i, Entity damager, double damage) {
        if (item.getHasPermission() == true && target.hasPermission(item.getPermission()) == false) {
            return -1;
        } else {
            double health = target.getHealth() - damage;
            if(health > healthTrigger) return -1;
            long cooldown;
            RPGValue value = RPGValue.get(target, item, "rescue.cooldown");
            if (value == null) {
                cooldown = System.currentTimeMillis() / 50;
                value = new RPGValue(target, item, "rescue.cooldown", cooldown);
            } else {
                cooldown = value.asLong();
            }
            if (cooldown <= System.currentTimeMillis() / 50) {
                if(!item.consumeDurability(i,consumption))return damage;
                value.set(System.currentTimeMillis() / 50 + cooldownTime);
                target.sendMessage(ChatColor.AQUA + Locale.get("power.rescue.info"));
                if(target.getBedSpawnLocation() != null)
                    target.teleport(target.getBedSpawnLocation());
                else
                    target.teleport(target.getWorld().getSpawnLocation());
                if (health < 0.1D) {
                    return target.getHealth() - 0.1;
                } else {
                    return damage;
                }
            } else {
                target.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown"), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
                return -1;
            }
        }
    }

}
