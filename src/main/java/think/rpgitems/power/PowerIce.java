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
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.ArgumentPriority;

import think.rpgitems.power.types.PowerRightClick;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Power ice.
 * <p>
 * The ice power will fire an ice block on right click
 * which will then create a box of ice on impact,
 * the ice will slowly remove itself.
 * </p>
 */
public class PowerIce extends Power implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    @ArgumentPriority
    public long cooldownTime = 20;
    /**
     * Cost of this power
     */
    public int consumption = 0;

    @SuppressWarnings("deprecation")
    @Override
    public void rightClick(final Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true))return;
        if (!checkCooldown(player, cooldownTime, true))return;
        if (!item.consumeDurability(stack, consumption)) return;
        player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1.0f, 0.1f);

        // launch an ice block
        final FallingBlock block = player.getWorld().spawnFallingBlock(player.getLocation().add(0, 1.8, 0), Material.PACKED_ICE, (byte) 0);
        block.setVelocity(player.getLocation().getDirection().multiply(2d));
        block.setDropItem(false);

        BukkitRunnable run = new BukkitRunnable() {
            public void run() {
                boolean hit = false;
                World world = block.getWorld();

                List<Entity> entities = block.getNearbyEntities(1, 1, 1);
                for (Entity e : entities) {
                    if (e != player) {
                        hit = true;
                        break;
                    }
                }

                if (block.isDead() || hit) {
                    Location landingLoc = block.getLocation();
                    boolean hitBlock = block.isDead();
                    // remove entity and (potential) placed block.
                    block.remove();
                    if (hitBlock) {
                        if (landingLoc.getBlock().getType().equals(Material.PACKED_ICE)) {
                            landingLoc.getBlock().setType(Material.AIR);
                        }
                    }
                    cancel();
                    final HashMap<Location, Long> changedBlocks = new HashMap<>();
                    for (int x = -1; x < 2; x++) {
                        for (int y = -1; y < 3; y++) {
                            for (int z = -1; z < 2; z++) {
                                Location loc = landingLoc.clone().add(x, y, z);
                                Block b = world.getBlockAt(loc);
                                if (!b.getType().isSolid() && !b.getType().toString().contains("SIGN")
                                        && !(b.getType() == Material.SKULL || b.getType() == Material.FLOWER_POT)) {
                                    changedBlocks.put(b.getLocation(), b.getTypeId() | ((long) b.getData() << 16));
                                    b.setType(Material.PACKED_ICE);
                                }
                            }
                        }
                    }

                    // ice block remove timer
                    (new BukkitRunnable() {
                        Random random = new Random();

                        @Override
                        public void run() {
                            for (int i = 0; i < 4; i++) {
                                if (changedBlocks.isEmpty()) {
                                    cancel();
                                    return;
                                }
                                int index = random.nextInt(changedBlocks.size());
                                long data = changedBlocks.values().toArray(new Long[0])[index];
                                Location position = changedBlocks.keySet().toArray(new Location[0])[index];
                                changedBlocks.remove(position);
                                Block c = position.getBlock();
                                position.getWorld().playEffect(position, Effect.STEP_SOUND, c.getTypeId());
                                c.setTypeId((int) (data & 0xFFFF));
                                c.setData((byte) (data >> 16));
                            }

                        }
                    }).runTaskTimer(RPGItems.plugin, 4 * 20 + new Random().nextInt(40), 3);
                }

            }
        };
        run.runTaskTimer(RPGItems.plugin, 0, 1);
    }

    @Override
    public String displayText() {
        return I18n.format("power.ice", (double) cooldownTime / 20d);
    }

    @Override
    public String getName() {
        return "ice";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("consumption", consumption);
    }

}
