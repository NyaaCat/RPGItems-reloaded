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
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.BaseTriggers;

import java.util.List;
import java.util.function.Supplier;

import static think.rpgitems.power.Utils.checkCooldown;
import static think.rpgitems.power.Utils.getNearbyEntities;

/**
 * Power attract.
 * <p>
 * Pull around mobs in {@link #radius radius} to player.
 * Moving the mobs with max speed of {@link #maxSpeed maxSpeed}
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class, implClass = Attract.Impl.class)
public class Attract extends BasePower {
    @Property(order = 0)
    public int radius = 5;
    @Property(order = 1, required = true)
    public double maxSpeed = 0.4D;
    @Property
    public int duration = 5;
    @Property
    public int cost = 0;
    @Property
    public int attractingTickCost = 0;
    @Property
    public int attractingEntityTickCost = 0;

    @Property
    public int cooldown = 0;

    @Property
    public boolean attractPlayer;

    @Property
    public boolean requireHurtByEntity = true;

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
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
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

    public class Impl implements PowerTick, PowerLeftClick, PowerRightClick, PowerPlain, PowerSneaking, PowerHurt, PowerHitTaken, PowerBowShoot, PowerBeamHit, PowerProjectileHit{

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            return attract(player, stack);
        }

        private PowerResult<Void> fire(Player player, ItemStack stack, Supplier<List<Entity>> supplier) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            new BukkitRunnable() {
                int dur = getDuration();

                @Override
                public void run() {
                    if (--dur <= 0) {
                        this.cancel();
                        return;
                    }
                    attract(player, stack, supplier);
                }
            }.runTaskTimer(RPGItems.plugin, 0, 1);
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return Attract.this;
        }

        private PowerResult<Void> attract(Player player, ItemStack stack, Supplier<List<Entity>> supplier) {
            if (!player.isOnline() || player.isDead()) {
                return PowerResult.noop();
            }
            if (!triggers.contains(BaseTriggers.TICK) && !stack.equals(player.getInventory().getItemInMainHand())) {
                return PowerResult.noop();
            }
            double factor = Math.sqrt(getRadius() - 1.0) / getMaxSpeed();
            List<Entity> entities = supplier.get();
            if (entities.isEmpty()) return null;
            if (!getItem().consumeDurability(stack, getAttractingTickCost())) return null;
            for (Entity e : entities) {
                if (e instanceof LivingEntity
                            && (isAttractPlayer() || !(e instanceof Player))) {
                    if (!getItem().consumeDurability(stack, getAttractingEntityTickCost())) break;
                    Location locTarget = e.getLocation();
                    Location locPlayer = player.getLocation();
                    double d = locTarget.distance(locPlayer);
                    if (d < 1 || d > getRadius()) continue;
                    double newVelocity = Math.sqrt(d - 1) / factor;
                    if (Double.isInfinite(newVelocity)) {
                        newVelocity = 0;
                    }
                    Vector direction = locPlayer.subtract(locTarget).toVector().normalize();
                    e.setVelocity(direction.multiply(newVelocity));
                }
            }
            return PowerResult.ok();
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            return attract(player, stack);
        }

        private PowerResult<Void> attract(Player player, ItemStack stack) {
            return attract(player, stack, () -> getNearbyEntities(getPower(), player.getLocation(), player, getRadius()));
        }

        @Override
        public PowerResult<Void> sneaking(Player player, ItemStack stack) {
            return attract(player, stack);
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
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
        public PowerResult<Double> hitEntity(Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            Location location = entity.getLocation();
            return fire(player, stack, () -> getNearbyEntities(getPower(), location, player, getRadius())).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            return fire(player, stack, () -> getNearbyEntities(getPower(), location, player, getRadius()));
        }

        @Override
        public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
            return fire(player, stack, () -> getNearbyEntities(getPower(), event.getEntity().getLocation(), player, getRadius()));
        }
    }
}
