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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerRightClick;

public class PowerConsume extends Power implements PowerRightClick {

    @Override
    public void rightClick(Player player, Block clicked) {
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false) {
        } else {
            ItemStack item = player.getInventory().getItemInHand();
            int count = item.getAmount() - 1;
            if (count == 0) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin.plugin, new Runnable() {
                    @Override
                    public void run() {
                        player.getInventory().setItemInHand(new ItemStack(Material.AIR));
                    }
                }, 1L);
            } else {
                item.setAmount(count);
            }
        }
    }

    @Override
    public void init(ConfigurationSection s) {

    }

    @Override
    public void save(ConfigurationSection s) {

    }

    @Override
    public String getName() {
        return "consume";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + Locale.get("power.consume");
    }
}
