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

import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerRightClick;

import static think.rpgitems.utils.PowerUtils.checkCooldown;

/**
 * Power tippedarrow.
 * <p>
 * The arrow power will fire an tipped arrow on right click
 * with {@link #type effect} for {@link #duration} ticks at power {@link #amplifier}
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public class PowerTippedArrow extends BasePower implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Amplifier of potion effect
     */
    @Property(order = 3,required=true)
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
    public PotionEffectType type = PotionEffectType.POISON;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, consumption)) return PowerResult.cost();
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        TippedArrow arrow = player.launchProjectile(TippedArrow.class);
        Events.rpgProjectiles.put(arrow.getEntityId(), getItem().getID());
        arrow.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
        Events.removeArrows.add(arrow.getEntityId());
        return PowerResult.ok();
    }

    @Override
    public String displayText() {
        return I18n.format("power.tippedarrow", type.getName().toLowerCase().replaceAll("_", " "), amplifier + 1, ((double) duration) / 20d, (double) cooldown / 20d);
    }

    @Override
    public String getName() {
        return "tippedarrow";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldown = s.getLong("cooldown", 20);
        duration = s.getInt("duration", 1);
        amplifier = s.getInt("amplifier", 15);
        String potionEffectName = s.getString("type", "HARM");
        type = PotionEffectType.getByName(potionEffectName);
        consumption = s.getInt("consumption", 1);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldown);
        s.set("duration", duration);
        s.set("amplifier", amplifier);
        s.set("type", type.getName());
        s.set("consumption", consumption);
    }

}
