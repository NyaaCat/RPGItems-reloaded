package think.rpgitems.power.impl;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Context;

import java.util.List;
import java.util.Random;

import static think.rpgitems.Events.DAMAGE_SOURCE;
import static think.rpgitems.Events.OVERRIDING_DAMAGE;
import static think.rpgitems.power.Utils.getNearbyEntities;

/**
 * Represents an active rumble effect managed by {@link RumbleManager}.
 */
public class ActiveRumble implements Tickable {
    private final Player player;
    private final Location location;
    private final Vector direction;
    private final Rumble power;
    private final Random random;
    private int count;

    public ActiveRumble(Player player, Location location, Vector direction, Rumble power) {
        this.player = player;
        this.location = location.clone();
        this.direction = direction.clone();
        this.direction.setY(0);
        this.direction.normalize();
        this.power = power;
        this.random = new Random();
        this.count = 0;
    }

    @Override
    public boolean tick() {
        Location above = location.clone().add(0, 1, 0);
        if (above.getBlock().getType().isSolid() || !location.getBlock().getType().isSolid()) {
            return false;
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

        List<Entity> near = getNearbyEntities(power, location, player, 1.5);
        boolean hit = false;
        for (Entity e : near) {
            if (e != player) {
                hit = true;
                break;
            }
        }

        if (hit) {
            near = getNearbyEntities(power, location, player, power.getPower() * 2 + 1);
            for (Entity e : near) {
                if (e != player) {
                    if (e instanceof ItemFrame || e instanceof Painting || e.hasMetadata("NPC")) {
                        e.setMetadata("RPGItems.Rumble", new FixedMetadataValue(RPGItems.plugin, null));
                        continue;
                    }
                    if (e.getLocation().distance(location) <= 2.5) {
                        e.setVelocity(new Vector(
                                random.nextGaussian() / 4d,
                                1d + random.nextDouble() * (double) power.getPower(),
                                random.nextGaussian() / 4d
                        ));
                    }

                    if (!(e instanceof LivingEntity)) {
                        continue;
                    }
                    if (power.getDamage() > 0) {
                        Context.instance().putTemp(player.getUniqueId(), DAMAGE_SOURCE, power.getNamespacedKey().toString());
                        Context.instance().putTemp(player.getUniqueId(), OVERRIDING_DAMAGE, power.getDamage());
                        ((LivingEntity) e).damage(power.getDamage(), player);
                        Context.instance().putTemp(player.getUniqueId(), OVERRIDING_DAMAGE, null);
                        Context.instance().putTemp(player.getUniqueId(), DAMAGE_SOURCE, null);
                    }
                }
            }
            location.getWorld().createExplosion(location.getX(), location.getY(), location.getZ(), power.getPower(), false, false);
            return false;
        }

        location.add(direction);
        count++;

        return count < power.getDistance();
    }
}
