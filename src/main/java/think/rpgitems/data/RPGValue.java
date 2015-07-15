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
package think.rpgitems.data;

import java.util.HashMap;

import org.bukkit.entity.Player;

import think.rpgitems.item.RPGItem;

public class RPGValue {

    Object value;

    static HashMap<String, RPGValue> map = new HashMap<String, RPGValue>();

    public static RPGValue get(Player player, RPGItem item, String name) {
        return map.get(player.getName() + "." + item.getID() + "." + name);
    }

    public RPGValue(Player player, RPGItem item, String name, Object value) {
        this.value = value;
        map.put(player.getName() + "." + item.getID() + "." + name, this);
    }

    public void set(Object value) {
        this.value = value;
    }

    public boolean asBoolean() {
        return (Boolean) value;
    }

    public byte asByte() {
        return (Byte) value;
    }

    public double asDouble() {
        return (Double) value;
    }

    public float asFloat() {
        return (Float) value;
    }

    public int asInt() {
        return (Integer) value;
    }

    public long asLong() {
        return (Long) value;
    }

    public short asShort() {
        return (Short) value;
    }

    public String asString() {
        return (String) value;
    }

    public Object value() {
        return value;
    }

}
