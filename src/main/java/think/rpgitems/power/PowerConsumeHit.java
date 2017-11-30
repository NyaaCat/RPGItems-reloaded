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

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;
import think.rpgitems.power.types.PowerHit;

/**
 * Power consumehit.
 * <p>
 * The consume power will remove one item when player hit something. With {@link #cooldownTime cooldown} time (ticks).
 * </p>
 */
public class PowerConsumeHit extends Power implements PowerHit {
    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public int cooldownTime = 0;
    //TODO:ADD delay.
    @Override
    public void hit(final Player player, ItemStack stack, LivingEntity entity, double damage) {
        if (!checkCooldown(player, cooldownTime, false)) return;
        if (!item.checkPermission(player, true)) return;
        int count = stack.getAmount() - 1;
        if (count == 0) {
            stack.setAmount(0);
            stack.setType(Material.AIR);
        } else {
            stack.setAmount(count);
        }
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getInt("cooldown", 0);
    }    //TODO:ADD delay.


    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
    }    //TODO:ADD delay.


    @Override
    public String getName() {
        return "consumehit";
    }

    @Override
    public String displayText() {
        return I18n.format("power.consumehit");
    }
}
