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
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerRightClick;

/**
 * Power potionself.
 * <p>
 * On right click it will apply {@link #type effect}
 * for {@link #duration} ticks at power {@link #amplifier}.
 * </p>
 */
public class PowerPotionSelf extends Power implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    public long cooldownTime = 20;
    /**
     * Amplifier of potion effect
     */
    public int amplifier = 1;
    /**
     * Time of potion effect, in ticks
     */
    public int duration = 20;
    /**
     * Cost of this power
     */
    public int consumption = 0;
    /**
     * Type of potion effect
     */
    public PotionEffectType type = PotionEffectType.HEAL;

    @Override
    public void rightClick(Player player, ItemStack item, Block clicked) {
        long cooldown;
        if (this.item.getHasPermission() && !player.hasPermission(this.item.getPermission())) {
        } else {
            RPGValue value = RPGValue.get(player, this.item, "potionself.cooldown");
            if (value == null) {
                cooldown = System.currentTimeMillis() / 50;
                value = new RPGValue(player, this.item, "potionself.cooldown", cooldown);
            } else {
                cooldown = value.asLong();
            }
            if (cooldown <= System.currentTimeMillis() / 50) {
                if (!this.item.consumeDurability(item, consumption)) return;
                value.set(System.currentTimeMillis() / 50 + cooldownTime);
                player.addPotionEffect(new PotionEffect(type, duration, amplifier), true);
            } else {
                player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown"), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
            }
        }
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown");
        amplifier = s.getInt("amp");
        duration = s.getInt("time");
        type = PotionEffectType.getByName(s.getString("type", "heal"));
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("amp", amplifier);
        s.set("time", duration);
        s.set("type", type.getName());
        s.set("consumption", consumption);
    }

    @Override
    public String getName() {
        return "potionself";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.potionself"), type.getName().toLowerCase().replaceAll("_", " "), amplifier + 1, ((double) duration) / 20d);
    }

}
