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

import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.commands.PowerMeta;
import think.rpgitems.commands.Property;
import think.rpgitems.commands.Deserializer;
import think.rpgitems.commands.Serializer;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerRightClick;
import think.rpgitems.utils.PotionEffectUtils;

import static think.rpgitems.utils.PowerUtils.checkCooldown;

/**
 * Power potionself.
 * <p>
 * On right click it will apply {@link #type effect}
 * for {@link #duration} ticks at power {@link #amplifier}.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(immutableTrigger = true)
public class PowerPotionSelf extends BasePower implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Amplifier of potion effect
     */
    @Property(order = 2)
    public int amplifier = 1;
    /**
     * Time of potion effect, in ticks
     */
    @Property(order = 1)
    public int duration = 20;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;
    /**
     * Type of potion effect
     */
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @Property(order = 3, required = true)
    public PotionEffectType type = PotionEffectType.HEAL;

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        player.addPotionEffect(new PotionEffect(type, duration, amplifier), true);
        return PowerResult.ok();
    }

    @Override
    public String getName() {
        return "potionself";
    }

    @Override
    public String displayText() {
        return I18n.format("power.potionself", type.getName().toLowerCase().replaceAll("_", " "), amplifier + 1, ((double) duration) / 20d);
    }
}
