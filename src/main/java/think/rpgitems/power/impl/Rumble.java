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
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Context;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Random;

import static think.rpgitems.Events.DAMAGE_SOURCE;
import static think.rpgitems.Events.OVERRIDING_DAMAGE;
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
@Meta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class, implClass = Rumble.Impl.class)
public class Rumble extends BasePower {

    @Property(order = 0)
    public int cooldown = 0;
    @Property(order = 1)
    public int power = 2;
    @Property(order = 2, required = true)
    public int distance = 15;
    @Property
    public int cost = 0;

    @Property
    public double damage = 0;

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

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
        return I18n.formatDefault("power.rumble", (double) getCooldown() / 20d);
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Power of rumble
     */
    public int getPower() {
        return power;
    }

    public class Impl implements PowerRightClick, PowerLeftClick, PowerSneak, PowerSprint, PowerPlain, PowerBowShoot, PowerLivingEntity {
        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(final Player player, ItemStack stack) {
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower());
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            final Location location = player.getLocation().add(0, -0.2, 0);
            final Vector direction = player.getLocation().getDirection();
            return fire(player, location, direction);
        }

        private PowerResult<Void> fire(Player player, Location location, Vector direction) {
            RumbleManager.getInstance().register(
                    new ActiveRumble(player, location, direction, Rumble.this)
            );
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return Rumble.this;
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, @Nullable Double value) {
            return fire(player, entity.getLocation(), entity.getLocation().getDirection());
        }
    }

    /**
     * Represents an active rumble effect managed by {@link RumbleManager}.
     */
    public static class ActiveRumble implements Tickable {
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
}
