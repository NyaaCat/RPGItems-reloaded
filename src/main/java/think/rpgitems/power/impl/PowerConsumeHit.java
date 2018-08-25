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
package think.rpgitems.power.impl;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;
import think.rpgitems.commands.PowerMeta;
import think.rpgitems.power.PowerHit;
import think.rpgitems.power.PowerResult;

import static think.rpgitems.utils.PowerUtils.checkCooldown;

/**
 * Power consumehit.
 * <p>
 * The consume power will remove one item when player hits something. With {@link #cooldown cooldown} time (ticks).
 * </p>
 */
@PowerMeta(immutableTrigger = true)
public class PowerConsumeHit extends BasePower implements PowerHit {
    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public int cooldown = 0;

    @Override
    public PowerResult<Double> hit(final Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (!checkCooldown(this, player, cooldown, false)) return PowerResult.cd();
        int count = stack.getAmount() - 1;
        if (count == 0) {
            stack.setAmount(0);
            stack.setType(Material.AIR);
        } else {
            stack.setAmount(count);
        }
        return PowerResult.ok(damage);
    }

    @Override
    public String getName() {
        return "consumehit";
    }

    @Override
    public String displayText() {
        return I18n.format("power.consumehit");
    }
}
