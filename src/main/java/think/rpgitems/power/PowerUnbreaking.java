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

public class PowerUnbreaking extends Power implements PowerHit {

    public int level = 1;
    private Random random = new Random();

    @SuppressWarnings("deprecation")
    @Override
    public void hit(Player player, LivingEntity e, double damage) {
        if (random.nextDouble() < ((double) level) / 100d) {
            System.out.println(player.getItemInHand().getDurability());
            player.getItemInHand().setDurability((short) (player.getItemInHand().getDurability() - 1));
            System.out.println(player.getItemInHand().getDurability());
            player.updateInventory();
        }
    }

    @Override
    public void init(ConfigurationSection s) {
        level = s.getInt("level", 1);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("level", level);
    }

    @Override
    public String getName() {
        return "unbreaking";
    }

    @Override
    public String displayText(String locale) {
        return String.format(ChatColor.GREEN + Locale.get("power.unbreaking", locale), level);
    }
}
