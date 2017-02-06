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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerHit;

import java.util.Random;

public class PowerPotionHit extends Power implements PowerHit {

    public int chance = 20;
    private Random random = new Random();
    public PotionEffectType type = PotionEffectType.HARM;
    public int duration = 20;
    public int amplifier = 1;
    public int consumption = 0;

    @Override
    public void hit(Player player, ItemStack i, LivingEntity e, double damage) {
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false) {
        } else if (random.nextInt(chance) == 0){
            e.addPotionEffect(new PotionEffect(type, duration, amplifier));
            if(!item.consumeDurability(i,consumption))return;
        }
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.potionhit"), (int) ((1d / (double) chance) * 100d), type.getName().toLowerCase().replace('_', ' '));
    }

    @Override
    public String getName() {
        return "potionhit";
    }

    @Override
    public void init(ConfigurationSection s) {
        chance = s.getInt("chance", 20);
        duration = s.getInt("duration", 20);
        amplifier = s.getInt("amplifier", 1);
        type = PotionEffectType.getByName(s.getString("type", PotionEffectType.HARM.getName()));
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("chance", chance);
        s.set("duration", duration);
        s.set("amplifier", amplifier);
        s.set("type", type.getName());
        s.set("consumption", consumption);
    }

}
