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

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.PowerMeta;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerRightClick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static think.rpgitems.utils.PowerUtils.checkCooldown;

/**
 * Power torch.
 * <p>
 * The torch power will shoots torches to light up an area.
 * </p>
 */
@PowerMeta(immutableTrigger = true)
public class PowerTorch extends BasePower implements PowerRightClick {
    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    @Override
    public PowerResult<Void> rightClick(final Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1.0f, 0.8f);
        final FallingBlock block = player.getWorld().spawnFallingBlock(player.getLocation().add(0, 1.8, 0), Material.TORCH.createBlockData());
        block.setVelocity(player.getLocation().getDirection().multiply(2d));
        block.setDropItem(false);
        BukkitRunnable run = new BukkitRunnable() {

            public void run() {
                World world = block.getWorld();
                final Random random = new Random();
                if (block.isDead()) {
                    block.remove();
                    if (block.getLocation().getBlock().getType().equals(Material.TORCH))
                        block.setMetadata("RPGItems.Torch", new FixedMetadataValue(RPGItems.plugin, null));
                    cancel();
                    final HashMap<Location, Material> changedBlocks = new HashMap<>();
                    final HashMap<Location, BlockData> changedBlockData = new HashMap<>();
                    for (int x = -2; x <= 2; x++) {
                        for (int y = -2; y <= 3; y++) {
                            for (int z = -2; z <= 2; z++) {
                                Location loc = block.getLocation().add(x, y, z);
                                Block b = world.getBlockAt(loc);
                                if (b.getType().equals(Material.AIR) && random.nextInt(100) < 20) {
                                    List<BlockFace> faces = getPossibleFaces(loc);
                                    if (faces.size() > 0) {
                                        changedBlocks.put(b.getLocation(), b.getType());
                                        changedBlockData.put(b.getLocation(), b.getBlockData());
                                        BlockFace o = faces.get(random.nextInt(faces.size()));
                                        b.setMetadata("RPGItems.Torch", new FixedMetadataValue(RPGItems.plugin, null));
                                        b.setType(o == BlockFace.DOWN ? Material.TORCH : Material.WALL_TORCH, false);
                                        if (o != BlockFace.DOWN) {
                                            Directional f = ((Directional) b.getBlockData());
                                            f.setFacing(o);
                                            b.setBlockData(f, false);
                                        }
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
                                block.removeMetadata("RPGItems.Torch", RPGItems.plugin);
                                if (block.getLocation().getBlock().getType() == Material.TORCH) {
                                    block.getLocation().getBlock().setType(Material.AIR);
                                }
                                return;
                            }
                            int index = random.nextInt(changedBlocks.size());
                            Location loc = (Location) changedBlocks.keySet().toArray()[index];
                            Material material = changedBlocks.get(loc);
                            BlockData data = changedBlockData.get(loc);
                            Location position = changedBlocks.keySet().toArray(new Location[0])[index];
                            changedBlocks.remove(position);
                            Block c = position.getBlock();
                            position.getWorld().playEffect(position, Effect.STEP_SOUND, c.getType());
                            c.removeMetadata("RPGItems.Torch", RPGItems.plugin);
                            c.setType(material, false);
                            c.setBlockData(data, false);

                        }
                    }).runTaskTimer(RPGItems.plugin, 4 * 20 + new Random().nextInt(40), 3);
                }

            }
        };
        run.runTaskTimer(RPGItems.plugin, 0, 1);
        return PowerResult.ok();
    }

    @Override
    public String displayText() {
        return I18n.format("power.torch", (double) cooldown / 20d);
    }

    @Override
    public String getName() {
        return "torch";
    }

    private List<BlockFace> getPossibleFaces(Location loc) {
        List<BlockFace> faces = new ArrayList<>();
        Block relative = loc.getBlock().getRelative(BlockFace.DOWN);
        if (relative.getType().isSolid() && relative.getType().isOccluding()) {
            faces.add(BlockFace.DOWN);
        }
        for (BlockFace f : ((Directional) Material.WALL_TORCH.createBlockData()).getFaces()) {
            Block block = loc.getBlock().getRelative(f);
            if (block.getType().isSolid() && block.getType().isOccluding()) {
                faces.add(f.getOppositeFace());
            }
        }
        return faces;
    }
}
