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

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.Property;
import think.rpgitems.commands.BooleanChoice;
import think.rpgitems.power.PowerLeftClick;
import think.rpgitems.power.PowerRightClick;

import static think.rpgitems.utils.PowerUtils.checkCooldown;

/**
 * Power consume.
 * <p>
 * The consume power will remove one item on {@link #isRight click}.
 * With {@link #cooldownTime cooldown} time (ticks).
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public class PowerConsume extends BasePower implements PowerRightClick, PowerLeftClick {
    /**
     * Cooldown time of this power
     */
    @Property(order = 1)
    public int cooldownTime = 0;
    /**
     * Whether triggers when right click.
     */
    @Property(order = 0)
    @BooleanChoice(name = "mouse", falseChoice = "left", trueChoice = "right")
    public boolean isRight = true;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;

    @Override
    public void rightClick(final Player player, ItemStack stack, Block clicked) {
        if (!getItem().checkPermission(player, true)) return;
        if (isRight && checkCooldown(this, player, cooldownTime, false)) {
            consume(player);
        }
    }

    @Override
    public void leftClick(final Player player, ItemStack stack, Block clicked) {
        if (!getItem().checkPermission(player, true)) return;
        if (!isRight && checkCooldown(this, player, cooldownTime, false)) {
            consume(player);
        }
    }

    private void consume(final Player player) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (!getItem().consumeDurability(stack, consumption)) return;
        int count = stack.getAmount() - 1;
        if (count == 0) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(RPGItems.plugin, new Runnable() {
                @Override
                public void run() {
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
            }, 1L);
        } else {
            stack.setAmount(count);
        }
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getInt("cooldown", 0);
        isRight = s.getBoolean("isRight", true);
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("isRight", isRight);
        s.set("consumption", consumption);
    }

    @Override
    public String getName() {
        return "consume";
    }

    @Override
    public String displayText() {
        return I18n.format("power.consume");
    }
}
