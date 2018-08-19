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

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerHit;
import think.rpgitems.power.PowerProjectileHit;
import think.rpgitems.power.PowerResult;

import java.util.Random;

/**
 * Power lightning.
 * <p>
 * The lightning power will strike the hit target with lightning with a chance of 1/{@link #chance}.
 * </p>
 */
public class PowerLightning extends BasePower implements PowerHit, PowerProjectileHit {
    /**
     * Chance of triggering this power
     */
    @Property(order = 0)
    public int chance = 20;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    private Random random = new Random();

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (random.nextInt(chance) == 0) {
            if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
            entity.getWorld().strikeLightning(entity.getLocation());
            return PowerResult.ok(damage);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> projectileHit(Player player, ItemStack stack, Projectile p, ProjectileHitEvent event) {
        if (random.nextInt(chance) == 0) {
            if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
            p.getWorld().strikeLightning(p.getLocation());
            return PowerResult.ok();
        }
        return PowerResult.noop();
    }

    @Override
    public String displayText() {
        return I18n.format("power.lightning", (int) ((1d / (double) chance) * 100d));
    }

    @Override
    public String getName() {
        return "lightning";
    }
}
