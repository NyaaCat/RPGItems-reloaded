package think.rpgitems.power.impl;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Context;
import think.rpgitems.power.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

import static think.rpgitems.Events.DAMAGE_SOURCE;
import static think.rpgitems.Events.OVERRIDING_DAMAGE;
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
@Meta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class, implClass = Rumble.Impl.class)
public class Rumble extends BasePower {

    @Property(order = 1)
    public int power = 2;
    @Property(order = 2, required = true)
    public int distance = 15;

    @Property
    public double damage = 0;

    public double getDamage() {
        return damage;
    }

    /**
     * Maximum distance of rumble
     */
    public int getDistance() {
        return distance;
    }

    @Override
    public String getName() {
        return "rumble";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.rumble", (double) 0 / 20d);
    }

    /**
     * Power of rumble
     */
    public int getPower() {
        return power;
    }

    public static class Impl implements PowerRightClick<Rumble>, PowerLeftClick<Rumble>, PowerSneak<Rumble>, PowerSprint<Rumble>, PowerPlain<Rumble>, PowerBowShoot<Rumble>, PowerLivingEntity<Rumble> {
        @Override
        public PowerResult<Void> leftClick(Rumble power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(Rumble power, final Player player, ItemStack stack) {
            final Location location = player.getLocation().add(0, -0.2, 0);
            final Vector direction = player.getLocation().getDirection();
            return fire(power, player, location, direction);
        }

        private PowerResult<Void> fire(Rumble power, Player player, Location location, Vector direction) {
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
                    List<Entity> near = getNearbyEntities(power, location, player, 1.5);
                    boolean hit = false;
                    Random random = new Random();
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
                                if (e instanceof ItemFrame || e instanceof Painting) {
                                    e.setMetadata("RPGItems.Rumble", new FixedMetadataValue(RPGItems.plugin, null)); // Add metadata to protect hanging entities from the explosion
                                    continue;
                                }
                                if (e.getLocation().distance(location) <= 2.5)
                                    e.setVelocity(new Vector(random.nextGaussian() / 4d, 1d + random.nextDouble() * (double) power.getPower(), random.nextGaussian() / 4d));

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
                        location.getWorld().createExplosion(location.getX(), location.getY(), location.getZ(), power.getPower(), false, false); // Trigger the explosion after all hanging entities have been protected
                        cancel();
                        return;
                    }
                    location.add(direction);
                    if (getCount() >= power.getDistance()) {
                        cancel();
                    }
                    count = getCount() + 1;
                }

                public int getCount() {
                    return count;
                }
            };
            task.runTaskTimer(RPGItems.plugin, 0, 3);
            return PowerResult.ok();
        }

        @Override
        public Class<? extends Rumble> getPowerClass() {
            return Rumble.class;
        }

        @Override
        public PowerResult<Void> rightClick(Rumble power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Rumble power, Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(power, player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Void> sneak(Rumble power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sprint(Rumble power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(Rumble power, Player player, ItemStack stack, LivingEntity entity, @Nullable Double value) {
            return fire(power, player, entity.getLocation(), entity.getLocation().getDirection());
        }
    }

}
