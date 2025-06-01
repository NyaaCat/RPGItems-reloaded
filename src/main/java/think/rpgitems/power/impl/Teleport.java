package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Pair;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
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
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.function.Supplier;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power teleport.
 * <p>
 * The teleport power will teleport you
 * in the direction you're looking in
 * or to the place where the projectile hit
 * with maximum distance of {@link #distance} blocks
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = {"RIGHT_CLICK", "PROJECTILE_HIT"}, generalInterface = PowerPlain.class, implClass = Teleport.Impl.class)
public class Teleport extends BasePower {

    @Property(order = 1)
    public int distance = 5;
    @Property(order = 0)
    public int cooldown = 0;
    @Property
    public int cost = 0;
    @Property
    public String particle = "PORTAL";
    @Property
    public String sound = "entity.enderman.teleport";

    @Property
    public TargetMode targetMode = TargetMode.DEFAULT;

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    @Override
    public String getName() {
        return "teleport";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.teleport", getDistance(), (double) getCooldown() / 20d);
    }

    /**
     * Maximum distance.
     */
    public int getDistance() {
        return distance;
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    public String getParticle() { return particle; }

    public String getSound() {return sound;}

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

    public class Impl implements PowerSneak, PowerLeftClick, PowerSprint, PowerRightClick, PowerProjectileHit, PowerPlain, PowerBowShoot, PowerBeamHit, PowerConsume, PowerJump, PowerSwim {

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        public PowerResult<Void> fire(Player player, ItemStack stack, Supplier<Location> supplier) {
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("location",supplier);
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            Location newLoc = supplier.get();
            World world = player.getWorld();
            Vector velocity = player.getVelocity();
            boolean gliding = player.isGliding();
            player.teleport(newLoc);
            if (gliding) {
                player.setVelocity(velocity);
            }
            if(!getParticle().toString().isEmpty()){
                world.spawnParticle(Particle.valueOf(getParticle().toUpperCase(Locale.ROOT)),newLoc,100);
            }
            if(!getSound().isEmpty()){
                world.playSound(newLoc, Registry.SOUNDS.get(Key.key(getSound().toLowerCase(Locale.ROOT))), 1.0f, 0.3f);
            }
            return PowerResult.ok();
        }

        private Location getNewLoc(Player player, World world) {
            Location location = player.getLocation();
            Location start = location.clone().add(new Vector(0, 1.6, 0));
            Location eyeLocation = player.getEyeLocation();
            Vector direction = eyeLocation.getDirection();
            Block lastSafe = world.getBlockAt(start);
            Location newLoc = lastSafe.getLocation();

            boolean ignorePassable = true;
            switch (getTargetMode()) {
                case RAY_TRACING_EXACT:
                case RAY_TRACING_EXACT_SWEEP:
                    ignorePassable = false;
                case RAY_TRACING:
                case RAY_TRACING_SWEEP: {
                    RayTraceResult result = player.getWorld().rayTraceBlocks(eyeLocation, direction, getDistance(), FluidCollisionMode.NEVER, ignorePassable);
                    Block firstUnsafe = result == null ? null : result.getHitBlock();
                    if (firstUnsafe == null) {
                        newLoc = location.add(direction.clone().multiply(getDistance()));
                        break;
                    } else {
                        newLoc = result.getHitPosition().toLocation(world);
                    }
                    if (getTargetMode() == TargetMode.RAY_TRACING || getTargetMode() == TargetMode.RAY_TRACING_EXACT) {
                        break;
                    }
                    Vector move = newLoc.toVector().subtract(location.toVector());
                    Pair<Vector, Vector> sweep = Utils.sweep(player.getBoundingBox(), BoundingBox.of(firstUnsafe), move);
                    if (sweep != null) {
                        newLoc = location.clone().add(sweep.getKey());
                    }
                    break;

                }
                case DEFAULT: {
                    try {
                        BlockIterator bi = new BlockIterator(player, getDistance());
                        while (bi.hasNext()) {
                            Block block = bi.next();
                            if (!block.getType().isSolid() || (block.getType() == Material.AIR)) {
                                lastSafe = block;
                            } else {
                                break;
                            }
                        }
                    } catch (IllegalStateException ex) {
//                        ex.printStackTrace();
//                        RPGItems.logger.info("This exception may be harmless");
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
        public Power getPower() {
            return Teleport.this;
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
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
        public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(player, itemStack).with(e.getForce());
        }

        @Override
        public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            World world = player.getWorld();
            Location start = player.getLocation();
            Location newLoc = getEntityLocation(player, event.getEntity());
            if (start.distanceSquared(newLoc) >= getDistance() * getDistance()) {
                player.sendMessage(I18n.formatDefault("message.too.far"));
                return PowerResult.noop();
            }
            player.teleport(newLoc);
            world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
            world.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
            return PowerResult.ok();
        }

        private Location getEntityLocation(Player player, Projectile entity) {
            Location start = player.getLocation();
            Location newLoc = entity.getLocation();
            newLoc.setPitch(start.getPitch());
            newLoc.setYaw(start.getYaw());
            return newLoc;
        }

        @Override
        public PowerResult<Double> hitEntity(Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            return fire(player, stack, () -> entity.getLocation()).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            return fire(player, stack, () -> location);
        }

        @Override
        public PowerResult<Void> beamEnd(Player player, ItemStack stack, Location location, BeamEndEvent event) {
            return fire(player, stack, () -> location);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            World world = player.getWorld();
            return fire(player, stack, () -> getNewLoc(player, world));
        }

        @Override
        public PowerResult<Void> consume(Player player, ItemStack stack, PlayerItemConsumeEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> jump(Player player, ItemStack stack, PlayerJumpEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> swim(Player player, ItemStack stack, EntityToggleSwimEvent event) {
            return fire(player, stack);
        }
    }
}
