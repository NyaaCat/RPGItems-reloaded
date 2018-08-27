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

import think.rpgitems.I18n;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.Property;


/**
 * Power ranged.
 * <p>
 * Not a triggerable power.
 * Mark this item as ranged.
 * </p>
 */
@PowerMeta(marker = true)
public class PowerRanged extends BasePower {
    /**
     * Maximum radius
     */
    @Property(order = 1)
    public int r = Integer.MAX_VALUE;

    /**
     * Minimum radius
     */
    @Property(order = 0)
    public int rm = 0;

    @Override
    public String getName() {
        return "ranged";
    }

    @Override
    public String displayText() {
        return I18n.format("power.ranged");
    }
}
