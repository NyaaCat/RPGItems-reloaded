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

import org.bukkit.configuration.ConfigurationSection;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;


/**
 * Power ranged.
 * <p>
 * Not a triggerable power.
 * Mark this item as ranged.
 * </p>
 */
public class PowerRanged extends Power {
    /**
     * Maximum radius
     */
    @Property(order = 6)
    public int r = 10;

    /**
     * Minimum radius
     */
    @Property(order = 5)
    public int rm = 0;

    @Override
    public void init(ConfigurationSection s) {
        r = s.getInt("r", Integer.MAX_VALUE);
        rm = s.getInt("rm", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("r", r);
        s.set("rm", rm);
    }

    @Override
    public String getName() {
        return "ranged";
    }

    @Override
    public String displayText() {
        return I18n.format("power.ranged");
    }
}
