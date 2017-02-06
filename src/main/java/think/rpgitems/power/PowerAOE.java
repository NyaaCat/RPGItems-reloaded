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
import org.bukkit.Effect;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerRightClick;

public class PowerAOE extends Power implements PowerRightClick {

    public long cooldownTime = 20;
    public int amplifier = 1;
    public int duration = 15;
    public int range = 5;
    public boolean selfapplication = true;
    public PotionEffectType type;
    public String name = null;
    public int consumption = 0;

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
        duration = s.getInt("duration", 60);
        amplifier = s.getInt("amplifier", 1);
        range = s.getInt("range", 5);
        selfapplication = s.getBoolean("selfapplication", true);
        type = PotionEffectType.getByName(s.getString("type", "HARM"));
        name = s.getString("name");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("range", range);
        s.set("duration", duration);
        s.set("amplifier", amplifier);
        s.set("selfapplication", selfapplication);
        s.set("type", type.getName());
        s.set("name", name);
        s.set("consumption", consumption);
    }

    @Override
    public void rightClick(final Player player, ItemStack i, Block clicked) {
        long cooldown;
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false) {
        } else {
            RPGValue value = RPGValue.get(player, item, "aoe.cooldown");
            if (value == null) {
                cooldown = System.currentTimeMillis() / 50;
                value = new RPGValue(player, item, "aoe.cooldown", cooldown);
            } else {
                cooldown = value.asLong();
            }
            if (cooldown <= System.currentTimeMillis() / 50) {
                if(!item.consumeDurability(i,consumption))return;
                value.set(System.currentTimeMillis() / 50 + cooldownTime);
                PotionEffect effect = new PotionEffect(type, duration, amplifier-1);
                if(selfapplication)
                    player.addPotionEffect(effect);
                player.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, 1);
                for(Entity ent : player.getNearbyEntities(range, range, range))
                    if(ent instanceof LivingEntity)
                        ((LivingEntity) ent).addPotionEffect(effect);
            } else {
                player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown"), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
            }
        }
    }

    @Override
    public String getName() {
        return "aoe";
    }

    @Override
    public String displayText() {
        return name!=null?name:ChatColor.GREEN + String.format(Locale.get("power.aoe"), type.getName(), amplifier, duration, selfapplication ? Locale.get("power.aoe.selfapplication.true"):Locale.get("power.aoe.selfapplication.false"), range, (double) cooldownTime / 20d);
    }

}
