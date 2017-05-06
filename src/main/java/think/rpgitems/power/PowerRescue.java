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

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.commands.ArgumentPriority;

import think.rpgitems.item.RPGItem;
import think.rpgitems.power.types.PowerHitTaken;
import think.rpgitems.power.types.PowerHurt;

/**
 * Power rescue.
 * <p>
 * The rescue power teleports the user to spawn (or to their bed when {@link #useBed} is active)
 * or rescue them in place when {@link #inPlace}
 * when their health gets below the {@link #healthTrigger} while in combat with an enemy
 * or when they takes a damage greater than {@link #damageTrigger}
 * </p>
 */
public class PowerRescue extends Power implements PowerHurt, PowerHitTaken {

    /**
     * Health trigger of rescue
     */
    @ArgumentPriority(1)
    public int healthTrigger = 4;
    /**
     * Whether use bed instead of home
     */
    @ArgumentPriority(2)
    public boolean useBed = true;
    /**
     * Whether rescue in place instead of teleport
     */
    @ArgumentPriority(3)
    public boolean inPlace = false;
    /**
     * Cooldown time of this power
     */
    @ArgumentPriority
    public long cooldownTime = 20;
    /**
     * Cost of this power
     */
    public int consumption = 0;
    /**
     * Damage trigger of rescue
     */
    public double damageTrigger = 1024;

    @Override
    public String displayText() {
        return I18n.format("power.rescue", ((double) healthTrigger) / 2, (double) cooldownTime / 20d);
    }

    @Override
    public String getName() {
        return "rescue";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
        healthTrigger = s.getInt("healthTrigger", 4);
        damageTrigger = s.getDouble("damageTrigger", 1024);
        useBed = s.getBoolean("useBed", true);
        inPlace = s.getBoolean("inPlace", false);
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("healthTrigger", healthTrigger);
        s.set("damageTrigger", damageTrigger);
        s.set("useBed", useBed);
        s.set("inPlace", inPlace);
        s.set("consumption", consumption);
    }

    @Override
    public void hurt(Player target, ItemStack stack, EntityDamageEvent event) {
        if (!item.checkPermission(target, false))return;
        double health = target.getHealth() - event.getFinalDamage();
        if (health > healthTrigger) return;
        rescue(target, stack, event, false);
    }

    @Override
    public double takeHit(Player target, ItemStack stack, EntityDamageEvent event) {
        if (!item.checkPermission(target, false)) {
            return event.getDamage();
        } else {
            if (event.getFinalDamage() < damageTrigger) return event.getDamage();
            rescue(target, stack, event, true);
            return 0;
        }
    }

    private void rescue(Player target, ItemStack stack, EntityDamageEvent event, boolean canceled) {
        if (!checkCooldown(target, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        target.sendMessage(I18n.format("power.rescue.info"));
        DamageCause cause = event.getCause();
        if (!canceled) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 1, 255));
            target.setHealth(healthTrigger + event.getFinalDamage());
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 10));
        target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 2));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 400, 2));
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 10, 1);

        if (inPlace && cause != DamageCause.DRAGON_BREATH
                && cause != DamageCause.DROWNING
                && cause != DamageCause.SUFFOCATION
                && cause != DamageCause.VOID) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 160, 10));
        } else if (useBed && target.getBedSpawnLocation() != null)
            target.teleport(target.getBedSpawnLocation());
        else
            target.teleport(target.getWorld().getSpawnLocation());
    }
}
