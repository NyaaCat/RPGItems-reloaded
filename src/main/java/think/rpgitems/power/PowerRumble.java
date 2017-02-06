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
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerRightClick;

import java.util.Random;

public class PowerRumble extends Power implements PowerRightClick {

    public long cooldownTime = 20;
    public int power = 2;
    public int distance = 15;
    public int consumption = 0;

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
        consumption = s.getInt("consumption", 0);
        power = s.getInt("power", 2);
        distance = s.getInt("distance", 15);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("power", power);
        s.set("distance", distance);
        s.set("consumption", consumption);
    }

    @Override
    public void rightClick(final Player player, ItemStack i, Block block) {
        long cooldown;
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false) {
        } else {
            RPGValue value = RPGValue.get(player, item, "rumble.cooldown");
            if (value == null) {
                cooldown = System.currentTimeMillis() / 50;
                value = new RPGValue(player, item, "rumble.cooldown", cooldown);
            } else {
                cooldown = value.asLong();
            }
            if (cooldown <= System.currentTimeMillis() / 50) {
                if(!item.consumeDurability(i,consumption))return;
                value.set(System.currentTimeMillis() / 50 + cooldownTime);
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
                        Entity[] near = getNearbyEntities(location, 1.5);
                        boolean hit = false;
                        Random random = new Random();
                        for (Entity e : near) {
                            if (e != player) {
                                hit = true;
                                break;
                            }
                        }
                        if (hit) {
                            near = getNearbyEntities(location, power * 2 + 1);
                            for (Entity e : near) {
                                if (e != player) {
                                    if (e instanceof ItemFrame || e instanceof Painting) {
                                        e.setMetadata("RPGItems.Rumble", new FixedMetadataValue(Plugin.plugin, null)); // Add metadata to protect hanging entities from the explosion
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
                task.runTaskTimer(Plugin.plugin, 0, 3);
            } else {
                player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown"), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
            }
        }
    }

    @Override
    public String getName() {
        return "rumble";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.rumble"), (double) cooldownTime / 20d);
    }

}
