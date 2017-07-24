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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.commands.ArgumentPriority;
import think.rpgitems.power.types.PowerHit;
import think.rpgitems.power.types.PowerProjectileHit;

import java.util.Random;

/**
 * Power lightning.
 * <p>
 * The lightning power will strike the hit target with lightning with a chance of 1/{@link #chance}.
 * </p>
 */
public class PowerLightning extends Power implements PowerHit, PowerProjectileHit {
    /**
     * Chance of triggering this power
     */
    @ArgumentPriority
    public int chance = 20;
    /**
     * Cost of this power
     */
    public int consumption = 0;

    private Random random = new Random();

    @Override
    public void hit(Player player, ItemStack stack, LivingEntity entity, double damage) {
        if (!item.checkPermission(player, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        if (random.nextInt(chance) == 0) {
            entity.getWorld().strikeLightning(entity.getLocation());
        }
    }

    @Override
    public void projectileHit(Player player, ItemStack stack, Projectile p) {
        if (!item.checkPermission(player, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        if (random.nextInt(chance) == 0) {
            p.getWorld().strikeLightning(p.getLocation());
        }
    }

    @Override
    public String displayText() {
        return I18n.format("power.lightning", (int) ((1d / (double) chance) * 100d));
    }

    @Override
    public String getName() {
        return "lightning";
    }

    @Override
    public void init(ConfigurationSection s) {
        chance = s.getInt("chance");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("chance", chance);
        s.set("consumption", consumption);
    }

}
