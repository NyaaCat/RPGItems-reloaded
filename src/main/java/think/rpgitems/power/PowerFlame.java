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
import think.rpgitems.I18n;
import think.rpgitems.commands.ArgumentPriority;

import think.rpgitems.power.types.PowerHit;

/**
 * Power flame.
 * <p>
 * The flame power will set the target on fire on hit in {@link #burnTime} ticks.
 * </p>
 */
public class PowerFlame extends Power implements PowerHit {

    /**
     * Duration of the fire, in ticks
     */
    @ArgumentPriority
    public int burnTime = 20;
    /**
     * Cost of this power
     */
    public int consumption = 0;

    @Override
    public void hit(Player player, ItemStack stack, LivingEntity entity, double damage) {
        if (!item.checkPermission(player, true))return;
        if (!item.consumeDurability(stack, consumption)) return;
        entity.setFireTicks(burnTime);
    }

    @Override
    public String displayText() {
        return I18n.format("power.flame", (double) burnTime / 20d);
    }

    @Override
    public String getName() {
        return "flame";
    }

    @Override
    public void init(ConfigurationSection s) {
        burnTime = s.getInt("burntime");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("burntime", burnTime);
        s.set("consumption", consumption);
    }

}
