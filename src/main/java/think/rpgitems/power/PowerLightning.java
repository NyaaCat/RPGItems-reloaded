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
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerConsuming;
import think.rpgitems.power.types.PowerHit;
import think.rpgitems.power.types.PowerProjectileHit;

import java.util.Random;

public class PowerLightning extends Power implements PowerHit, PowerProjectileHit, PowerConsuming {

    public int chance = 20;
    public int consumption = 0;

    private Random random = new Random();

    @Override
    public void hit(Player player, ItemStack i, LivingEntity e, double damage) {
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false) {
        } else if (random.nextInt(chance) == 0){
            if(!item.consumeDurability(i,consumption))return;
            e.getWorld().strikeLightning(e.getLocation());
        }
    }

    @Override
    public void projectileHit(Player player, ItemStack i, Projectile p) {
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false) {
            player.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.permission")));
        } else if (random.nextInt(chance) == 0){
            if(!item.consumeDurability(i,consumption))return;
            p.getWorld().strikeLightning(p.getLocation());
        }
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.lightning"), (int) ((1d / (double) chance) * 100d));
    }

    @Override
    public String getName() {
        return "lightning";
    }

    @Override
    public void init(ConfigurationSection s) {
        chance = s.getInt("chance");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("chance", chance);
        s.set("consumption", consumption);
    }

    public int getConsumption(){
        return consumption;
    }

    public void setConsumption(int cost){
        consumption = cost;
    }
}
