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
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerRightClick;
import think.rpgitems.power.Property;

import java.util.List;
import java.util.Random;

import static think.rpgitems.power.Utils.checkCooldown;
import static think.rpgitems.power.Utils.getNearbyEntities;

/**
 * Power rumble.
 * <p>
 * The rumble power sends a shockwave through the ground
 * and sends any hit entities flying with power {@link #power}.
 * The wave will travel {@link #distance} blocks.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(immutableTrigger = true, withSelectors = true)
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
    public int cost = 0;

    @Override
    public PowerResult<Void> rightClick(final Player player, ItemStack stack, Block block, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
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
        return PowerResult.ok();
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
