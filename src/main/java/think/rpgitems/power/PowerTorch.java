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

import java.util.*;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import gnu.trove.map.hash.TObjectLongHashMap;
import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerRightClick;

public class PowerTorch extends Power implements PowerRightClick {

    public long cooldownTime = 20;

    @SuppressWarnings("deprecation")
    @Override
    public void rightClick(final Player player, Block clicked) {
        long cooldown;
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false) {
        } else {
            RPGValue value = RPGValue.get(player, item, "torch.cooldown");
            if (value == null) {
                cooldown = System.currentTimeMillis() / 50;
                value = new RPGValue(player, item, "torch.cooldown", cooldown);
            } else {
                cooldown = value.asLong();
            }
            if (cooldown <= System.currentTimeMillis() / 50) {
                value.set(System.currentTimeMillis() / 50 + cooldownTime);
                player.playSound(player.getLocation(), Sound.FIRE_IGNITE, 1.0f, 0.8f);
                final FallingBlock block = player.getWorld().spawnFallingBlock(player.getLocation().add(0, 1.8, 0), Material.TORCH, (byte) 0);
                block.setVelocity(player.getLocation().getDirection().multiply(2d));
                block.setDropItem(false);
                BukkitRunnable run = new BukkitRunnable() {

                    public void run() {
                        World world = block.getWorld();
                        final Random random = new Random();
                        if (block.isDead()) {
                            block.remove();
                            if (block.getLocation().getBlock().getType().equals(Material.TORCH))
                                block.setMetadata("RPGItems.Torch", new FixedMetadataValue(Plugin.plugin, null));
                            cancel();
                            final TObjectLongHashMap<Location> changedBlocks = new gnu.trove.map.hash.TObjectLongHashMap<Location>();
                            for (int x = -2; x <= 2; x++) {
                                for (int y = -2; y <= 3; y++) {
                                    for (int z = -2; z <= 2; z++) {
                                        Location loc = block.getLocation().add(x, y, z);
                                        Block b = world.getBlockAt(loc);
                                        if (b.getType().equals(Material.AIR) && random.nextInt(100) < 20) {
                                            List<Byte> orientations = getPossibleOrientations(loc);
                                            if (orientations.size() > 0) {
                                                changedBlocks.put(b.getLocation(), b.getTypeId() | (b.getData() << 16));
                                                byte o = orientations.get(random.nextInt(orientations.size()));
                                                b.setMetadata("RPGItems.Torch", new FixedMetadataValue(Plugin.plugin, null));
                                                b.setTypeIdAndData(Material.TORCH.getId(), o, false); // Don't apply physics since the check is done beforehand
                                            }
                                        }
                                    }
                                }
                            }
                            (new BukkitRunnable() {

                                @Override
                                public void run() {
                                    if (changedBlocks.isEmpty()) {
                                        cancel();
                                        block.removeMetadata("RPGItems.Torch", Plugin.plugin);
                                        block.getLocation().getBlock().setType(Material.AIR);
                                        return;
                                    }
                                    int index = random.nextInt(changedBlocks.size());
                                    long data = changedBlocks.values()[index];
                                    Location position = (Location) changedBlocks.keys()[index];
                                    changedBlocks.remove(position);
                                    Block c = position.getBlock();
                                    position.getWorld().playEffect(position, Effect.STEP_SOUND, c.getTypeId());
                                    c.removeMetadata("RPGItems.Torch", Plugin.plugin);
                                    c.setTypeId((int) (data & 0xFFFF));
                                    c.setData((byte) (data >> 16));

                                }
                            }).runTaskTimer(Plugin.plugin, 4 * 20 + new Random().nextInt(40), 3);
                        }

                    }
                };
                run.runTaskTimer(Plugin.plugin, 0, 1);

            } else {
                player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown"), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
            }
        }
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.torch"), (double) cooldownTime / 20d);
    }

    @Override
    public String getName() {
        return "torch";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
    }

    private List<Byte> getPossibleOrientations(Location loc) {
        List<Byte> orientations = new ArrayList<Byte>();
        if (loc.subtract(0, 1, 0).getBlock().getType().isSolid())
            orientations.add((byte) 5);
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                if (Math.abs(x) != Math.abs(z)) {
                    Material materialToCheck = loc.add(x, 0, z).getBlock().getType();
                    if (materialToCheck.isSolid() && !materialToCheck.toString().contains("GRASS")) { // Tall grass somehow counts as solid block
                        if (x > 0)
                            orientations.add((byte) 2);
                        else if (x < 0)
                            orientations.add((byte) 1);
                        if (z > 0)
                            orientations.add((byte) 4);
                        else if (z < 0)
                            orientations.add((byte) 3);
                    }
                }

        return orientations;
    }
}
