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
package think.rpgitems.item;

import org.bukkit.ChatColor;

public enum Quality {
    TRASH(ChatColor.GRAY.toString(), "7"), COMMON(ChatColor.WHITE.toString(), "f"), UNCOMMON(ChatColor.GREEN.toString(), "a"), RARE(ChatColor.BLUE.toString(), "9"), EPIC(ChatColor.DARK_PURPLE.toString(), "5"), LEGENDARY(ChatColor.GOLD.toString(), "6");

    public final String colour;
    public final String cCode;

    Quality(String colour, String code) {
        this.colour = colour;
        this.cCode = code;
    }
}
