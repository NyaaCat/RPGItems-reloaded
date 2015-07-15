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

public class PowerPotionSelf extends Power implements PowerRightClick {

    public long cooldownTime = 20;
    public int amplifier = 3;
    public int time = 20;
    public PotionEffectType type = PotionEffectType.HEAL;

    @Override
    public void rightClick(Player player) {
        long cooldown;
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false){
        }else{  
        RPGValue value = RPGValue.get(player, item, "potionself.cooldown");
        if (value == null) {
            cooldown = System.currentTimeMillis() / 50;
            value = new RPGValue(player, item, "potionself.cooldown", cooldown);
        } else {
            cooldown = value.asLong();
        }
        if (cooldown <= System.currentTimeMillis() / 50) {
            value.set(System.currentTimeMillis() / 50 + cooldownTime);
            player.addPotionEffect(new PotionEffect(type, time, amplifier));
        } else {
            player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown", Locale.getPlayerLocale(player)), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
        }
        }
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown");
        amplifier = s.getInt("amp");
        time = s.getInt("time");
        type = PotionEffectType.getByName(s.getString("type", "heal"));
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("amp", amplifier);
        s.set("time", time);
        s.set("type", type.getName());
    }

    @Override
    public String getName() {
        return "potionself";
    }

    @Override
    public String displayText(String locale) {
        return ChatColor.GREEN + String.format(Locale.get("power.potionself", locale), type.getName().toLowerCase().replaceAll("_", " "), amplifier + 1, ((double) time) / 20d);
    }

}
