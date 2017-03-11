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
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerHitTaken;
import think.rpgitems.power.types.PowerHurt;

public class PowerRescue extends Power implements PowerHurt, PowerHitTaken {

    public String permission = "";
    public int healthTrigger = 4;
    public boolean useBed = true;
    public boolean inPlace = false;
    public long cooldownTime = 20;
    public int consumption = 0;
    public double damageTrigger = 1024;

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.rescue"), ((double) healthTrigger) / 2, (double) cooldownTime / 20d);
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
        permission = s.getString("permission", "");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("healthTrigger", healthTrigger);
        s.set("damageTrigger", damageTrigger);
        s.set("useBed", useBed);
        s.set("inPlace", inPlace);
        s.set("permission", permission);
        s.set("consumption", consumption);
    }

    @Override
    public void hurt(Player target, ItemStack i, EntityDamageEvent ev) {
        if (item.getHasPermission() && !target.hasPermission(item.getPermission())) {
        } else {
            double health = target.getHealth() - ev.getFinalDamage();
            if (health > healthTrigger) return;
            rescue(target, i, ev, false);
        }
    }

    @Override
    public double takeHit(Player target, ItemStack i, EntityDamageEvent ev) {
        if (item.getHasPermission() && !target.hasPermission(item.getPermission())) {
            return ev.getDamage();
        } else {
            if (ev.getFinalDamage() < damageTrigger) return ev.getFinalDamage();
            rescue(target, i, ev, true);
            return 0;
        }
    }

    private void rescue(Player target, ItemStack i, EntityDamageEvent ev, boolean canceled) {
        long cooldown;
        RPGValue value = RPGValue.get(target, item, "rescue.cooldown");
        if (value == null) {
            cooldown = System.currentTimeMillis() / 50;
            value = new RPGValue(target, item, "rescue.cooldown", cooldown);
        } else {
            cooldown = value.asLong();
        }
        if (cooldown <= System.currentTimeMillis() / 50) {
            if (!item.consumeDurability(i, consumption)) return;
            value.set(System.currentTimeMillis() / 50 + cooldownTime);
            target.sendMessage(ChatColor.AQUA + Locale.get("power.rescue.info"));
            DamageCause cause = ev.getCause();
            if (!canceled) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 1, 255));
                target.setHealth(healthTrigger + ev.getFinalDamage());
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
        } else {
            target.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown"), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
        }
    }
}
