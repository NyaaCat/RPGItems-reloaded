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
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.Property;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.types.PowerRightClick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Power torch.
 * <p>
 * The torch power will shoots torches to light up an area.
 * </p>
 */
public class PowerTorch extends Power implements PowerRightClick {
    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldownTime = 20;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;
    /**
     * delay before power activate.
     */
    @Property(order = 1)
    public int delay = 0;


    @SuppressWarnings("deprecation")
    @Override
    public void rightClick(final Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true)) return;
        if (!checkCooldown(player, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        new BukkitRunnable() {
            @Override
            public void run() {

                player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1.0f, 0.8f);
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
                                block.setMetadata("RPGItems.Torch", new FixedMetadataValue(RPGItems.plugin, null));
                            cancel();
                            final HashMap<Location, Long> changedBlocks = new HashMap<>();
                            for (int x = -2; x <= 2; x++) {
                                for (int y = -2; y <= 3; y++) {
                                    for (int z = -2; z <= 2; z++) {
                                        Location loc = block.getLocation().add(x, y, z);
                                        Block b = world.getBlockAt(loc);
                                        if (b.getType().equals(Material.AIR) && random.nextInt(100) < 20) {
                                            List<Byte> orientations = getPossibleOrientations(loc);
                                            if (orientations.size() > 0) {
                                                changedBlocks.put(b.getLocation(), b.getTypeId() | ((long) b.getData() << 16));
                                                byte o = orientations.get(random.nextInt(orientations.size()));
                                                b.setMetadata("RPGItems.Torch", new FixedMetadataValue(RPGItems.plugin, null));
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
                                        block.removeMetadata("RPGItems.Torch", RPGItems.plugin);
                                        block.getLocation().getBlock().setType(Material.AIR);
                                        return;
                                    }
                                    int index = random.nextInt(changedBlocks.size());
                                    long data = changedBlocks.values().toArray(new Long[0])[index];
                                    Location position = changedBlocks.keySet().toArray(new Location[0])[index];
                                    changedBlocks.remove(position);
                                    Block c = position.getBlock();
                                    position.getWorld().playEffect(position, Effect.STEP_SOUND, c.getTypeId());
                                    c.removeMetadata("RPGItems.Torch", RPGItems.plugin);
                                    c.setTypeId((int) (data & 0xFFFF));
                                    c.setData((byte) (data >> 16));

                                }
                            }).runTaskTimer(RPGItems.plugin, 4 * 20 + new Random().nextInt(40), 3);
                        }

                    }
                };
                run.runTaskTimer(RPGItems.plugin, 0, 1);
            }
        }.runTaskLater(RPGItems.plugin,delay);
    }

    @Override
    public String displayText() {
        return I18n.format("power.torch", (double) cooldownTime / 20d);
    }

    @Override
    public String getName() {
        return "torch";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
        consumption = s.getInt("consumption", 0);
        delay = s.getInt("delay",0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("consumption", consumption);
        s.set("delay",delay);
    }

    private List<Byte> getPossibleOrientations(Location loc) {
        List<Byte> orientations = new ArrayList<>();
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
