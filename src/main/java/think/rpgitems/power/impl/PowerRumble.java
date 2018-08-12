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

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerRightClick;

import java.util.List;
import java.util.Random;

import static think.rpgitems.utils.PowerUtils.checkCooldown;
import static think.rpgitems.utils.PowerUtils.getNearbyEntities;

/**
 * Power rumble.
 * <p>
 * The rumble power sends a shockwave through the ground
 * and sends any hit entities flying with power {@link #power}.
 * The wave will travel {@link #distance} blocks.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public class PowerRumble extends BasePower implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Power of rumble
     */
    @Property(order = 1)
    public int power = 2;
    /**
     * Maximum distance of rumble
     */
    @Property(order = 2, required = true)
    public int distance = 15;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;

    @Override
    public void rightClick(final Player player, ItemStack stack, Block block, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return;
        if (!getItem().consumeDurability(stack, consumption)) return;
        final Location location = player.getLocation().add(0, -0.2, 0);
        final Vector direction = player.getLocation().getDirection();
        direction.setY(0);
        direction.normalize();
        BukkitRunnable task = new BukkitRunnable() {
            private int count = 0;

            public void run() {
                Location above = location.clone().add(0, 1, 0);
                if (above.getBlock().getType().isSolid() || !location.getBlock().getType().isSolid()) {
                    cancel();
                    return;
                }

                Location temp = location.clone();
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        temp.setX(x + location.getBlockX());
                        temp.setZ(z + location.getBlockZ());
                        Block block = temp.getBlock();
                        temp.getWorld().playEffect(temp, Effect.STEP_SOUND, block.getType());
                    }
                }
                List<Entity> near = getNearbyEntities(PowerRumble.this, location, player,1.5);
                boolean hit = false;
                Random random = new Random();
                for (Entity e : near) {
                    if (e != player) {
                        hit = true;
                        break;
                    }
                }
                if (hit) {
                    near = getNearbyEntities(PowerRumble.this, location, player,power * 2 + 1);
                    for (Entity e : near) {
                        if (e != player) {
                            if (e instanceof ItemFrame || e instanceof Painting) {
                                e.setMetadata("RPGItems.Rumble", new FixedMetadataValue(RPGItems.plugin, null)); // Add metadata to protect hanging entities from the explosion
                                continue;
                            }
                            if (e.getLocation().distance(location) <= 2.5)
                                e.setVelocity(new Vector(random.nextGaussian() / 4d, 1d + random.nextDouble() * (double) power, random.nextGaussian() / 4d));
                        }
                    }
                    location.getWorld().createExplosion(location.getX(), location.getY(), location.getZ(), power, false, false); // Trigger the explosion after all hanging entities have been protected
                    cancel();
                    return;
                }
                location.add(direction);
                if (count >= distance) {
                    cancel();
                }
                count++;
            }
        };
        task.runTaskTimer(RPGItems.plugin, 0, 3);
    }

    @Override
    public String getName() {
        return "rumble";
    }

    @Override
    public String displayText() {
        return I18n.format("power.rumble", (double) cooldown / 20d);
    }

}
