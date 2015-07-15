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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerRightClick;

@Deprecated
public class PowerRush extends Power implements PowerRightClick {

    private long cd = 20;
    private int speed = 3;
    private int time = 20;

    @Override
    public void rightClick(Player player) {
        long cooldown;
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false){
        }else{
        RPGValue value = RPGValue.get(player, item, "rush.cooldown");
        if (value == null) {
            cooldown = System.currentTimeMillis() / 50;
            value = new RPGValue(player, item, "rush.cooldown", cooldown);
        } else {
            cooldown = value.asLong();
        }
        if (cooldown <= System.currentTimeMillis() / 50) {
            value.set(System.currentTimeMillis() / 50 + cd);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, time, speed));
        }
        }
    }

    @Override
    public void init(ConfigurationSection s) {
        cd = s.getLong("cooldown");
        speed = s.getInt("speed");
        time = s.getInt("time");
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cd);
        s.set("speed", speed);
        s.set("time", time);
    }

    @Override
    public String getName() {
        return "rush";
    }

    @Override
    public String displayText(String locale) {
        return ChatColor.GREEN + "Gives temporary speed boost";
    }

}
