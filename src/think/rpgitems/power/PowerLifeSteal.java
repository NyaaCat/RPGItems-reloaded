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

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerHit;

public class PowerLifeSteal extends Power implements PowerHit {

    public int chance = 20;
    private Random random = new Random();
    @Override
    public void hit(Player player, LivingEntity e, double damage) {
    	if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false){
        }else if (random.nextInt(chance) == 0){
          if ((player.getHealth() + damage) >= player.getMaxHealth()){
        	  player.setHealth(player.getMaxHealth());
          }else
        	player.setHealth(player.getHealth() + damage);
    }
    }	

    @Override
    public String displayText(String locale) {
        return ChatColor.GREEN + String.format(Locale.get("power.lifesteal", locale), (double) chance);
    }

    @Override
    public String getName() {
        return "lifesteal";
    }

    @Override
    public void init(ConfigurationSection s) {
        chance = s.getInt("chance");
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("chance", chance);
    }
}
