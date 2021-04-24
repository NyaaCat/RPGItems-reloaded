package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Pair;
import java.util.function.Supplier;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;

/**
 * Power teleport.
 *
 * <p>The teleport power will teleport you in the direction you're looking in or to the place where
 * the projectile hit with maximum distance of {@link #distance} blocks
 */
@SuppressWarnings("WeakerAccess")
@Meta(
        defaultTrigger = {"RIGHT_CLICK", "PROJECTILE_HIT"},
        generalInterface = PowerPlain.class,
        implClass = Teleport.Impl.class)
public class Teleport extends BasePower {

    @Property(order = 1)
    public int distance = 5;

    @Property public TargetMode targetMode = TargetMode.DEFAULT;

    @Override
    public String getName() {
        return "teleport";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.teleport", getDistance(), (double) 0 / 20d);
    }

    /** Maximum distance. */
    public int getDistance() {
        return distance;
    }

    public TargetMode getTargetMode() {
        return targetMode;
    }

    public enum TargetMode {
        DEFAULT,
        RAY_TRACING_SWEEP,
        RAY_TRACING_EXACT,
        RAY_TRACING_EXACT_SWEEP,
        RAY_TRACING
    }

    public static class Impl
            implements PowerSneak<Teleport>,
                    PowerLeftClick<Teleport>,
                    PowerSprint<Teleport>,
                    PowerRightClick<Teleport>,
                    PowerProjectileHit<Teleport>,
                    PowerPlain<Teleport>,
                    PowerBowShoot<Teleport>,
                    PowerBeamHit<Teleport> {

        @Override
        public PowerResult<Void> rightClick(
                Teleport power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        public PowerResult<Void> fire(
                Teleport power, Player player, ItemStack stack, Supplier<Location> supplier) {
            Location newLoc = supplier.get();
            World world = player.getWorld();
            Vector velocity = player.getVelocity();
            boolean gliding = player.isGliding();
            player.teleport(newLoc);
            if (gliding) {
                player.setVelocity(velocity);
            }
            world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
            world.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
            return PowerResult.ok();
        }

        private Location getNewLoc(Teleport power, Player player, World world) {
            Location location = player.getLocation();
            Location start = location.clone().add(new Vector(0, 1.6, 0));
            Location eyeLocation = player.getEyeLocation();
            Vector direction = eyeLocation.getDirection();
            Block lastSafe = world.getBlockAt(start);
            Location newLoc = lastSafe.getLocation();

            boolean ignorePassable = true;
            switch (power.getTargetMode()) {
                case RAY_TRACING_EXACT:
                case RAY_TRACING_EXACT_SWEEP:
                    ignorePassable = false;
                case RAY_TRACING:
                case RAY_TRACING_SWEEP:
                    {
                        RayTraceResult result =
                                player.getWorld()
                                        .rayTraceBlocks(
                                                eyeLocation,
                                                direction,
                                                power.getDistance(),
                                                FluidCollisionMode.NEVER,
                                                ignorePassable);
                        Block firstUnsafe = result == null ? null : result.getHitBlock();
                        if (firstUnsafe == null) {
                            newLoc = location.add(direction.clone().multiply(power.getDistance()));
                            break;
                        } else {
                            newLoc = result.getHitPosition().toLocation(world);
                        }
                        if (power.getTargetMode() == TargetMode.RAY_TRACING
                                || power.getTargetMode() == TargetMode.RAY_TRACING_EXACT) {
                            break;
                        }
                        Vector move = newLoc.toVector().subtract(location.toVector());
                        Pair<Vector, Vector> sweep =
                                Utils.sweep(
                                        player.getBoundingBox(), BoundingBox.of(firstUnsafe), move);
                        if (sweep != null) {
                            newLoc = location.clone().add(sweep.getKey());
                        }
                        break;
                    }
                case DEFAULT:
                    {
                        try {
                            BlockIterator bi = new BlockIterator(player, power.getDistance());
                            while (bi.hasNext()) {
                                Block block = bi.next();
                                if (!block.getType().isSolid()
                                        || (block.getType() == Material.AIR)) {
                                    lastSafe = block;
                                } else {
                                    break;
                                }
                            }
                        } catch (IllegalStateException ex) {
                            ex.printStackTrace();
                            RPGItems.logger.info("This exception may be harmless");
                        }
                        newLoc = lastSafe.getLocation();
                        break;
                    }
            }
            newLoc.setPitch(eyeLocation.getPitch());
            newLoc.setYaw(eyeLocation.getYaw());

            return newLoc;
        }

        @Override
        public Class<? extends Teleport> getPowerClass() {
            return Teleport.class;
        }

        @Override
        public PowerResult<Void> leftClick(
                Teleport power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sneak(
                Teleport power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sprint(
                Teleport power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(
                Teleport power, Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(power, player, itemStack).with(e.getForce());
        }

        @Override
        public PowerResult<Void> projectileHit(
                Teleport power, Player player, ItemStack stack, ProjectileHitEvent event) {
            World world = player.getWorld();
            Location start = player.getLocation();
            Location newLoc = getEntityLocation(power, player, event.getEntity());
            if (start.distanceSquared(newLoc) >= power.getDistance() * power.getDistance()) {
                player.sendMessage(I18n.formatDefault("message.too.far"));
                return PowerResult.noop();
            }
            player.teleport(newLoc);
            world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
            world.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
            return PowerResult.ok();
        }

        private Location getEntityLocation(Teleport power, Player player, Projectile entity) {
            Location start = player.getLocation();
            Location newLoc = entity.getLocation();
            newLoc.setPitch(start.getPitch());
            newLoc.setYaw(start.getYaw());
            return newLoc;
        }

        @Override
        public PowerResult<Double> hitEntity(
                Teleport power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                BeamHitEntityEvent event) {
            return fire(power, player, stack, entity::getLocation).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(
                Teleport power,
                Player player,
                ItemStack stack,
                Location location,
                BeamHitBlockEvent event) {
            return fire(power, player, stack, () -> location);
        }

        @Override
        public PowerResult<Void> beamEnd(
                Teleport power,
                Player player,
                ItemStack stack,
                Location location,
                BeamEndEvent event) {
            return fire(power, player, stack, () -> location);
        }

        @Override
        public PowerResult<Void> fire(Teleport power, Player player, ItemStack stack) {
            World world = player.getWorld();
            return fire(power, player, stack, () -> getNewLoc(power, player, world));
        }
    }
}
