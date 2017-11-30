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

import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.TippedArrow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.Property;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.types.PowerRightClick;

/**
 * Power tippedarrow.
 * <p>
 * The arrow power will fire an tipped arrow on right click
 * with {@link #type effect} for {@link #duration} ticks at power {@link #amplifier}
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public class PowerTippedArrow extends Power implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldownTime = 20;
    /**
     * Amplifier of potion effect
     */
    @Property(order = 3)
    public int amplifier = 1;
    /**
     * Duration of potion effect, in ticks
     */
    @Property(order = 2)
    public int duration = 15;
    /**
     * Type of potion effect
     */
    @Property(order = 1)
    public PotionEffectType type = null;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;
    /**
     * delay before power activate.
     */
    @Property(order = 4)
    public int delay = 0;


    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true)) return;
        if (!checkCooldown(player, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
                TippedArrow arrow = player.launchProjectile(TippedArrow.class);
                Events.rpgProjectiles.put(arrow.getEntityId(), item.getID());
                arrow.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
                Events.removeArrows.add(arrow.getEntityId());
            }
        }.runTaskLater(RPGItems.plugin,delay);
    }

    @Override
    public String displayText() {
        return I18n.format("power.tippedarrow", type.getName().toLowerCase().replaceAll("_", " "), amplifier + 1, ((double) duration) / 20d, (double) cooldownTime / 20d);
    }

    @Override
    public String getName() {
        return "tippedarrow";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
        duration = s.getInt("duration", 1);
        amplifier = s.getInt("amplifier", 15);
        String potionEffectName = s.getString("type", "HARM");
        type = PotionEffectType.getByName(potionEffectName);
        consumption = s.getInt("consumption", 1);
        delay = s.getInt("delay",0);
    }
    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("duration", duration);
        s.set("amplifier", amplifier);
        s.set("type", type.getName());
        s.set("consumption", consumption);
        s.set("delay",delay);
    }

}
