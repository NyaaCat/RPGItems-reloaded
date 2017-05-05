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

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerRightClick;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Power fire.
 * <p>
 * The fire power will fire an fireblock on right click
 * that burns entities hit for {@link #burnduration duration} in ticks.
 * Furthermore it sends out a burning trail into the aimed direction for {@link #distance} blocks.
 * </p>
 */
public class PowerFire extends Power implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    public long cooldownTime = 20;
    /**
     * Maximum distance
     */
    public int distance = 15;
    /**
     * Duration of the fire, in ticks
     */
    public int burnduration = 40;
    /**
     * Cost of this power
     */
    public int consumption = 0;

    @Override
    public void rightClick(final Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true))return;
        if (!checkCooldown(player, cooldownTime, true))return;
        if (!item.consumeDurability(stack, consumption)) return;
        player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1.0f, 1.2f);
        final List<Block> fireblocks = new ArrayList<>();
        final FallingBlock block = player.getWorld().spawnFallingBlock(player.getLocation().add(0, 1.8, 0), new MaterialData(Material.FIRE));
        block.setVelocity(player.getLocation().getDirection().multiply(2d));
        block.setDropItem(false);

        final Location location = player.getLocation().add(0, 0.5, 0).getBlock().getLocation();
        final Vector direction = player.getLocation().getDirection();
        direction.setY(0);
        direction.normalize();
        location.add(direction.multiply(4));

        BukkitRunnable run = new BukkitRunnable() {
            private boolean finishedFire = false, blockDead = false;
            private int count = 0;

            public void run() {
                if (!finishedFire) {
                    if (!location.getBlock().getType().equals(Material.AIR)) {
                        finishedFire = true;
                    }

                    Location temp = location.clone();
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            temp.setX(x + location.getBlockX());
                            temp.setZ(z + location.getBlockZ());
                            Block block = temp.getBlock();
                            if (block.getType().equals(Material.AIR) && !block.getRelative(0, -1, 0).getType().isBurnable()) {
                                block.setType(Material.FIRE);
                                fireblocks.add(block);
                            }
                        }
                    }
                    location.add(direction);
                    if (count >= distance) {
                        finishedFire = true;
                    }
                    count++;

                    if (finishedFire) {
                        (new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (fireblocks.isEmpty()) {
                                    cancel();
                                    return;
                                }
                                Block fb = fireblocks.get(0);
                                fb.getWorld().playEffect(fb.getLocation(), Effect.EXTINGUISH, 1);
                                fb.setType(Material.AIR);
                                fireblocks.remove(fb);
                            }
                        }).runTaskTimer(RPGItems.plugin, 4 * 20 + new Random().nextInt(40), 3);
                    }
                }

                if (!blockDead) {
                    if (block.isDead()) {
                        block.remove();
                        if (block.getLocation().getBlock().getType().equals(Material.FIRE))
                            block.getLocation().getBlock().setType(Material.AIR);
                        blockDead = true;
                    }
                } else {
                    List<Entity> ents = block.getNearbyEntities(0, 1, 0);
                    for (Entity ent : ents)
                        if (ent instanceof Damageable)
                            ent.setFireTicks(burnduration);
                }

                if (finishedFire && blockDead)
                    cancel();
            }
        };
        run.runTaskTimer(RPGItems.plugin, 0, 1);
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.fire"), (double) cooldownTime / 20d);
    }

    @Override
    public String getName() {
        return "fire";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
        distance = s.getInt("distance");
        burnduration = s.getInt("burnduration");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("distance", distance);
        s.set("burnduration", burnduration);
        s.set("consumption", consumption);
    }

}
