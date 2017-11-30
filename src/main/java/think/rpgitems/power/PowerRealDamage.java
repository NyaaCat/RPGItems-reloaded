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

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;
import think.rpgitems.power.types.PowerHit;

import static java.lang.Double.max;
import static java.lang.Double.min;


/**
 * Power realdamage.
 * <p>
 * The item will do {@link #realDamage} to {@link LivingEntity} player hits
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public class PowerRealDamage extends Power implements PowerHit {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldownTime = 20;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;
    /**
     * Damage of this power
     */
    @Property(order = 1, required = true)
    public double realDamage = 0;
    /**
     * Minimum damage to trigger
     */
    @Property
    public double minDamage = 0;
    //TODO:ADD delay.

    @Override
    public void hit(Player player, ItemStack stack, LivingEntity entity, double damage) {
        if (damage < minDamage) return;
        if (!item.checkPermission(player, true)) return;
        if (!checkCooldown(player, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        if (entity.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
            PotionEffect e = entity.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            if (e.getAmplifier() >= 4) return;
        }
        double health = entity.getHealth();
        double newHealth = health - realDamage;
        newHealth = max(newHealth, 0.1);//Bug workaround
        newHealth = min(newHealth, entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        entity.setHealth(newHealth);
    }
    //TODO:ADD delay.

    @Override
    public String displayText() {
        return I18n.format("power.realdamage", realDamage);
    }

    @Override
    public String getName() {
        return "realdamage";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
        consumption = s.getInt("consumption", 0);
        minDamage = s.getInt("minDamage", 0);
        realDamage = s.getInt("realDamage", 0);
    }
    //TODO:ADD delay.

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("consumption", consumption);
        s.set("minDamage", minDamage);
        s.set("realDamage", realDamage);
    }
    //TODO:ADD delay.

}
