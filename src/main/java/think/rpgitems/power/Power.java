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

import gnu.trove.map.hash.TObjectIntHashMap;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import think.rpgitems.data.RPGValue;
import think.rpgitems.item.RPGItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Power {

    public static HashMap<String, Class<? extends Power>> powers = new HashMap<String, Class<? extends Power>>();
    public static TObjectIntHashMap<String> powerUsage = new TObjectIntHashMap<String>();

    public RPGItem item;

    public Power() {

    }

    public abstract void init(ConfigurationSection s);

    public abstract void save(ConfigurationSection s);

    public abstract String getName();

    public abstract String displayText();

    public static Entity[] getNearbyEntities(Location l, double radius) {
        List<Entity> entities = new ArrayList<>();
        for (Entity e : l.getWorld().getNearbyEntities(l, radius, radius, radius)) {
            try {
                if (l.distance(e.getLocation()) <= radius) {
                    entities.add(e);
                }
            } catch(RuntimeException ex) {
                ex.printStackTrace();
            }
        }
        return entities.toArray(new Entity[entities.size()]);
    }

    protected final boolean checkCooldown(Player p, int cdTicks) {
        long cooldown;
        RPGValue value = RPGValue.get(p, item, getName() + ".cooldown");
        if (value == null) {
            cooldown = System.currentTimeMillis() / 50;
            value = new RPGValue(p, item, getName() + ".cooldown", cooldown);
        } else {
            cooldown = value.asLong();
        }
        if (cooldown <= System.currentTimeMillis() / 50) {
            value.set(System.currentTimeMillis() / 50 + cdTicks);
            return true;
        } else {
            return false;
        }
    }
}
