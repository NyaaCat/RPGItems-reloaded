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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerHit;

public class PowerConsumeHit extends Power implements PowerHit {
    public int cooldowmTime = 0;

    @Override
    public void hit(final Player player, ItemStack is, LivingEntity e, double damage) {
        if (checkCooldown(player, cooldowmTime)) {
            if (item.getHasPermission() && !player.hasPermission(item.getPermission())) return;
            int count = is.getAmount() - 1;
            if (count == 0) {
                is.setAmount(0);
                is.setType(Material.AIR);
            } else {
                is.setAmount(count);
            }
        }
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldowmTime = s.getInt("cooldown", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldowmTime);
    }

    @Override
    public String getName() {
        return "consumehit";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + Locale.get("power.consumehit");
    }
}
