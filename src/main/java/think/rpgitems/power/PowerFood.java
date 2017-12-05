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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.Property;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.types.PowerRightClick;

/**
 * Power food.
 * <p>
 * Restore {@link #foodpoints food points} when eaten.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public class PowerFood extends Power implements PowerRightClick {
    /**
     * Food Points
     */
    @Property(order = 0, required = true)
    public int foodpoints;
    /**
     * delay before power activate.
     */
    @Property(order = 1)
    public int delay = 0;

    @Override
    public void rightClick(final Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true)) return;
        new BukkitRunnable(){
            @Override
            public void run() {
                ItemStack item = player.getInventory().getItemInMainHand();
                int count = item.getAmount() - 1;
                if (count == 0) {
                    int newFoodPoint = player.getFoodLevel() + foodpoints;
                    if (newFoodPoint > 20) newFoodPoint = 20;
                    player.setFoodLevel(newFoodPoint);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(RPGItems.plugin, new Runnable() {
                        @Override
                        public void run() {
                            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                        }
                    }, 1L);
                } else {
                    player.setFoodLevel(player.getFoodLevel() + foodpoints);
                    item.setAmount(count);
                }
            }
        }.runTaskLater(RPGItems.plugin,delay);
    }
    @Override
    public void init(ConfigurationSection s) {
        foodpoints = s.getInt("foodpoints");
        delay = s.getInt("delay");
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("foodpoints", foodpoints);
        s.set("delay",delay);
    }

    @Override
    public String getName() {
        return "food";
    }

    @Override
    public String displayText() {
        return I18n.format("power.food", foodpoints);
    }
}
