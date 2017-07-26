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

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.Property;
import think.rpgitems.power.types.PowerRightClick;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * Power rainbow.
 * <p>
 * The rainbow power will fire {@link #count} of blocks of coloured wool
 * or {@link #isFire fire} on right click, the wool will remove itself.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public class PowerRainbow extends Power implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldownTime = 20;
    /**
     * Count of blocks
     */
    @Property(order = 1)
    public int count = 5;
    /**
     * Whether launch fire instead of wool
     */
    @Property(order = 2)
    public boolean isFire = false;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;

    private Random random = new Random();

    @SuppressWarnings("deprecation")
    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true)) return;
        if (!checkCooldown(player, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        final ArrayList<FallingBlock> blocks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            FallingBlock block;
            if (!isFire) {
                block = player.getWorld().spawnFallingBlock(player.getLocation().add(0, 1.8, 0), Material.WOOL, (byte) random.nextInt(16));
            } else {
                block = player.getWorld().spawnFallingBlock(player.getLocation().add(0, 1.8, 0), Material.FIRE, (byte) 0);
            }
            block.setVelocity(player.getLocation().getDirection().multiply(new Vector(random.nextDouble() * 2d + 0.5, random.nextDouble() * 2d + 0.5, random.nextDouble() * 2d + 0.5)));
            block.setDropItem(false);
            blocks.add(block);
        }
        (new BukkitRunnable() {

            ArrayList<Location> fallLocs = new ArrayList<>();
            Random random = new Random();

            public void run() {

                Iterator<Location> l = fallLocs.iterator();
                while (l.hasNext()) {
                    Location loc = l.next();
                    if (random.nextBoolean()) {
                        Block b = loc.getBlock();
                        if (b.getType() == (isFire ? Material.FIRE : Material.WOOL)) {
                            loc.getWorld().playEffect(loc, Effect.STEP_SOUND, isFire ? Material.FIRE : Material.WOOL, b.getData());
                            b.setType(Material.AIR);
                        }
                        l.remove();
                    }
                    if (random.nextInt(5) == 0) {
                        break;
                    }
                }

                Iterator<FallingBlock> it = blocks.iterator();
                while (it.hasNext()) {
                    FallingBlock block = it.next();
                    if (block.isDead()) {
                        fallLocs.add(block.getLocation());
                        it.remove();
                    }
                }

                if (fallLocs.isEmpty() && blocks.isEmpty()) {
                    cancel();
                }

            }
        }).runTaskTimer(RPGItems.plugin, 0, 5);
    }

    @Override
    public String displayText() {
        return I18n.format("power.rainbow", count, (double) cooldownTime / 20d);
    }

    @Override
    public String getName() {
        return "rainbow";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
        count = s.getInt("count", 5);
        isFire = s.getBoolean("isFire");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("count", count);
        s.set("isFire", isFire);
        s.set("consumption", consumption);
    }

}
