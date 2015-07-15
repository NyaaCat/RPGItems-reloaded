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
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerRightClick;

public class PowerFood extends Power implements PowerRightClick {
	public int foodpoints;
	@Override
    public void rightClick(Player player) {
	       if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false){
	       }else{
          ItemStack item = player.getInventory().getItemInHand();
          int count = item.getAmount() - 1;
          if (count == 0) {
        	player.setFoodLevel(player.getFoodLevel() + foodpoints);
            item.setAmount(0);
            item.setType(Material.AIR);
            player.setItemInHand(item);
          } else {
        	player.setFoodLevel(player.getFoodLevel() + foodpoints);
            item.setAmount(count);
        }
	  }
    }

    @Override
    public void init(ConfigurationSection s) {
      foodpoints = s.getInt("foodpoints");
    }

    @Override
    public void save(ConfigurationSection s) {
      s.set("foodpoints", foodpoints); 
    }

    @Override
    public String getName() {
        return "food";
    }

    @Override
    public String displayText(String locale) {
        return ChatColor.GREEN + String.format(Locale.get("power.food", locale),foodpoints);
    }
}
