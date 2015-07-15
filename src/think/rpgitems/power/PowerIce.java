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

import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerRightClick;

public class PowerIce extends Power implements PowerRightClick {

    public long cooldownTime = 20;

    @Override
    public void rightClick(final Player player) {
        long cooldown;
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false){
        }else{  
        RPGValue value = RPGValue.get(player, item, "ice.cooldown");
        if (value == null) {
            cooldown = System.currentTimeMillis() / 50;
            value = new RPGValue(player, item, "ice.cooldown", cooldown);
        } else {
            cooldown = value.asLong();
        }
        if (cooldown <= System.currentTimeMillis() / 50) {
            value.set(System.currentTimeMillis() / 50 + cooldownTime);
            player.playSound(player.getLocation(), Sound.FIZZ, 1.0f, 0.1f);
            final FallingBlock block = player.getWorld().spawnFallingBlock(player.getLocation().add(0, 1.8, 0), Material.ICE, (byte) 0);
            block.setVelocity(player.getLocation().getDirection().multiply(2d));
            block.setDropItem(false);
            BukkitRunnable run = new BukkitRunnable() {

                @SuppressWarnings("deprecation")
				public void run() {
                    boolean hit = false;
                    World world = block.getWorld();
                    Location bLoc = block.getLocation();
                    loop: for (int x = -1; x < 2; x++) {
                        for (int y = -1; y < 2; y++) {
                            for (int z = -1; z < 2; z++) {
                                Location loc = block.getLocation().add(x, y, z);
                                if (world.getBlockTypeIdAt(loc) != Material.AIR.getId()) {
                                    Block b = world.getBlockAt(loc);
                                    if (b.getType().isSolid()) {
                                        if (checkBlock(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 1, 1, 1, bLoc.getX() - 0.5d, bLoc.getY() - 0.5d, bLoc.getZ() - 0.5d, 1, 1, 1)) {
                                            hit = true;
                                            break loop;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!hit) {
                        List<Entity> entities = block.getNearbyEntities(1, 1, 1);
                        for (Entity e : entities) {
                            if (e != player) {
                                hit = true;
                                break;
                            }
                        }
                    }
                    if (block.isDead() || hit) {
                        block.remove();
                        block.getLocation().getBlock().setType(Material.AIR);
                        cancel();
                        final TObjectLongHashMap<Location> changedBlocks = new gnu.trove.map.hash.TObjectLongHashMap<Location>();
                        for (int x = -1; x < 2; x++) {
                            for (int y = -1; y < 3; y++) {
                                for (int z = -1; z < 2; z++) {
                                    Location loc = block.getLocation().add(x, y, z);
                                    Block b = world.getBlockAt(loc);
                                    if (!b.getType().isSolid()) {
                                        changedBlocks.put(b.getLocation(), b.getTypeId() | (b.getData() << 16));
                                        b.setType(Material.ICE);
                                    }
                                }
                            }
                        }
                        (new BukkitRunnable() {
                            Random random = new Random();

                            @SuppressWarnings("deprecation")
							public void run() {
                                for (int i = 0; i < 4; i++) {
                                    if (changedBlocks.isEmpty()) {
                                        cancel();
                                        return;
                                    }
                                    int index = random.nextInt(changedBlocks.size());
                                    long data = changedBlocks.values()[index];
                                    Location position = (Location) changedBlocks.keys()[index];
                                    changedBlocks.remove(position);
                                    Block c = position.getBlock();
                                    position.getWorld().playEffect(position, Effect.STEP_SOUND, c.getTypeId());
                                    c.setTypeId((int) (data & 0xFFFF));
                                    c.setData((byte) (data >> 16));
                                }

                            }
                        }).runTaskTimer(Plugin.plugin, 4 * 20 + new Random().nextInt(40), 3);
                    }

                }
            };
            run.runTaskTimer(Plugin.plugin, 0, 1);

        } else {
            player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown", Locale.getPlayerLocale(player)), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
        }
        }
    }

    private boolean checkBlock(double x1, double y1, double z1, double w1, double h1, double d1, double x2, double y2, double z2, double w2, double h2, double d2) {
        if (x1 + w1 < x2)
            return false;
        if (x2 + w2 < x1)
            return false;
        if (y1 + h1 < y2)
            return false;
        if (y2 + h2 < y1)
            return false;
        if (z1 + d1 < z2)
            return false;
        if (z2 + d2 < z1)
            return false;

        return true;
    }

    @Override
    public String displayText(String locale) {
        return ChatColor.GREEN + String.format(Locale.get("power.ice", locale), (double) cooldownTime / 20d);
    }

    @Override
    public String getName() {
        return "ice";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
    }
}
