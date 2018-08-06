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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerHit;

import java.util.Random;

/**
 * Power knockup.
 * <p>
 * The knockup power will send the hit target flying
 * with a chance of 1/{@link #chance} and a power of {@link #power}.
 * </p>
 */
public class PowerKnockup extends BasePower implements PowerHit {

    /**
     * Chance of triggering this power
     */
    @Property(order = 0)
    public int chance = 20;
    /**
     * Power of knock up
     */
    @Property(order = 1)
    public double power = 2;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;

    private Random rand = new Random();

    @Override
    public void hit(Player player, ItemStack stack, LivingEntity entity, double damage) {
        if (!getItem().checkPermission(player, true)) return;
        if (!getItem().consumeDurability(stack, consumption)) return;
        if (rand.nextInt(chance) == 0) {
            entity.setVelocity(player.getLocation().getDirection().setY(power));
        }
    }

    @Override
    public String displayText() {
        return I18n.format("power.knockup", (int) ((1d / (double) chance) * 100d));
    }

    @Override
    public String getName() {
        return "knockup";
    }

    @Override
    public void init(ConfigurationSection s) {
        chance = s.getInt("chance");
        power = s.getDouble("power", 2);
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("chance", chance);
        s.set("power", power);
        s.set("consumption", consumption);
    }

}
