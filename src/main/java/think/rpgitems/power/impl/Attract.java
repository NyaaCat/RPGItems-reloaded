package think.rpgitems.power.impl;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;
import think.rpgitems.utils.cast.CastUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

import static think.rpgitems.power.Utils.getNearbyEntities;

/**
 * Power attract.
 * <p>
 * Pull around mobs in {@link #radius radius} to player.
 * Moving the mobs with max speed of {@link #maxSpeed maxSpeed}
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = {
        PowerLeftClick.class,
        PowerRightClick.class,
        PowerPlain.class,
        PowerSneak.class,
        PowerLivingEntity.class,
        PowerSprint.class,
        PowerHurt.class,
        PowerHit.class,
        PowerHitTaken.class,
        PowerBowShoot.class,
        PowerBeamHit.class,
        PowerLocation.class
}, implClass = Attract.Impl.class)
public class Attract extends BasePower {
    @Property(order = 0)
    public int radius = 5;
    @Property(order = 1, required = true)
    public double maxSpeed = 0.4D;
    @Property
    public int duration = 5;
    @Property
    public int attractingTickCost = 0;
    @Property
    public int attractingEntityTickCost = 0;

    @Property
    public boolean attractPlayer;

    @Property
    public boolean requireHurtByEntity = true;

    @Property
    public FiringLocation firingLocation = FiringLocation.SELF;

    @Property
    public double firingRange = 64;

    public double getFiringRange() {
        return firingRange;
    }

    public FiringLocation getFiringLocation() {
        return firingLocation;
    }

    public enum FiringLocation {
        SELF, TARGET
    }


    /**
     * Hooking Cost Pre-Entity-Tick
     */
    public int getAttractingEntityTickCost() {
        return attractingEntityTickCost;
    }

    /**
     * Hooking Cost Pre-Tick
     */
    public int getAttractingTickCost() {
        return attractingTickCost;
    }

    /**
     * Duration of this power when triggered by click in tick
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Maximum speed.
     */
    public double getMaxSpeed() {
        return maxSpeed;
    }

    @Override
    public String getName() {
        return "attract";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.attract");
    }

    /**
     * Maximum radius
     */
    public int getRadius() {
        return radius;
    }

    /**
     * Whether allow attracting player
     */
    public boolean isAttractPlayer() {
        return attractPlayer;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public static class Impl implements PowerTick<Attract>, PowerLeftClick<Attract>, PowerRightClick<Attract>, PowerPlain<Attract>, PowerSneaking<Attract>, PowerHurt<Attract>, PowerHitTaken<Attract>, PowerBowShoot<Attract>, PowerBeamHit<Attract>, PowerProjectileHit<Attract>, PowerLivingEntity<Attract>, PowerLocation<Attract> {

        @Override
        public PowerResult<Double> takeHit(Attract power, Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Attract power, Player player, ItemStack stack) {
            Location location = player.getLocation();
            if (power.getFiringLocation().equals(FiringLocation.TARGET)) {
                CastUtils.CastLocation result = CastUtils.rayTrace(player, player.getEyeLocation(), player.getEyeLocation().getDirection(), power.getFiringRange());
                location = result.getTargetLocation();
            }
            Location finalLocation = location;
            return fire(power, player, location, stack, () -> getNearbyEntities(power, finalLocation, player, power.getRadius()));
        }

        private PowerResult<Void> fire(Attract power, Player player, Location location, ItemStack stack, Supplier<List<Entity>> supplier) {
            new BukkitRunnable() {
                int dur = power.getDuration();

                @Override
                public void run() {
                    if (--dur <= 0) {
                        this.cancel();
                        return;
                    }
                    attract(power, player, location, stack, supplier);
                }
            }.runTaskTimer(RPGItems.plugin, 0, 1);
            return PowerResult.ok();
        }

        @Override
        public Class<? extends Attract> getPowerClass() {
            return Attract.class;
        }

        private PowerResult<Void> attract(Attract power, Player player, Location location, ItemStack stack, Supplier<List<Entity>> supplier) {
            if (!player.isOnline() || player.isDead()) {
                return PowerResult.noop();
            }
            double factor = Math.sqrt(power.getRadius() - 1.0) / power.getMaxSpeed();
            List<Entity> entities = supplier.get();
            if (entities.isEmpty()) return PowerResult.ok();
            if (!power.getItem().consumeDurability(stack, power.getAttractingTickCost())) return PowerResult.ok();
            for (Entity e : entities) {
                if (e instanceof LivingEntity
                            && (power.isAttractPlayer() || !(e instanceof Player))) {
                    if (!power.getItem().consumeDurability(stack, power.getAttractingEntityTickCost())) break;
                    Location locTarget = e.getLocation();
                    Location locPlayer = location;
                    double d = locTarget.distance(locPlayer);
                    if (d < 1 || d > power.getRadius()) continue;
                    double newVelocity = Math.sqrt(d - 1) / factor;
                    if (Double.isInfinite(newVelocity)) {
                        newVelocity = 0;
                    }
                    Vector direction = locPlayer.clone().subtract(locTarget).toVector().normalize();
                    e.setVelocity(direction.multiply(newVelocity));
                }
            }
            return PowerResult.ok();
        }

        @Override
        public PowerResult<Void> hurt(Attract power, Player target, ItemStack stack, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> tick(Attract power, Player player, ItemStack stack) {
            return attract(power, player, stack);
        }

        private PowerResult<Void> attract(Attract power, Player player, ItemStack stack) {
            Location location = player.getLocation();
            return attract(power, player, location, stack, () -> getNearbyEntities(power, player.getLocation(), player, power.getRadius()));
        }

        @Override
        public PowerResult<Void> sneaking(Attract power, Player player, ItemStack stack) {
            return attract(power, player, stack);
        }

        @Override
        public PowerResult<Void> leftClick(Attract power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> rightClick(Attract power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Attract power, Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(power, player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Double> hitEntity(Attract power, Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            Location location = event.getLoc();
            return fire(power, player, location, stack, () -> getNearbyEntities(power, location, player, power.getRadius())).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(Attract power, Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            return fire(power, player, location, stack, () -> getNearbyEntities(power, location, player, power.getRadius()));
        }

        @Override
        public PowerResult<Void> beamEnd(Attract power, Player player, ItemStack stack, Location location, BeamEndEvent event) {
            return fire(power, player, location, stack, () -> getNearbyEntities(power, location, player, power.getRadius()));
        }

        @Override
        public PowerResult<Void> projectileHit(Attract power, Player player, ItemStack stack, ProjectileHitEvent event) {
            Location location = event.getEntity().getLocation();
            return fire(power, player, location, stack, () -> getNearbyEntities(power, location, player, power.getRadius()));
        }

        @Override
        public PowerResult<Void> fire(Attract power, Player player, ItemStack stack, LivingEntity entity, @Nullable Double value) {
            Location location = entity.getLocation();
            if (power.getFiringLocation().equals(FiringLocation.TARGET)) {
                CastUtils.CastLocation result = CastUtils.rayTrace(entity, entity.getEyeLocation(), entity.getEyeLocation().getDirection(), power.getFiringRange());
                location = result.getTargetLocation();
            }
            Location finalLocation = location;
            return fire(power, player, location, stack, () -> getNearbyEntities(power, finalLocation, player, power.getRadius()));
        }

        @Override
        public PowerResult<Void> fire(Attract power, Player player, ItemStack stack, Location location) {
            return fire(power, player, location, stack, () -> getNearbyEntities(power, location, player, power.getRadius()));
        }
    }
}
