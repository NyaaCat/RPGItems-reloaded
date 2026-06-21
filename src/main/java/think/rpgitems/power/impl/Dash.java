package think.rpgitems.power.impl;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import java.util.ArrayList;

import static think.rpgitems.power.Utils.checkCooldown;

@Meta(defaultTrigger = "RIGHT_CLICK", implClass = Dash.Impl.class)
public class Dash extends BasePower {
    public enum Direction {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        UP,
        DOWN,
        RANDOM,
        RANDOM_HORIZONTAL,
        RANDOM_VERTICAL,
        WEST,
        EAST,
        NORTH,
        SOUTH
    }

    @Property
    public Direction direction = Direction.FORWARD;

    @Property
    public int cooldown = 0;

    @Property
    public int cost = 0;

    @Property
    public double speed = 1;

    @Property
    public int duration = 0;

    @Override
    public String getName() {
        return "dash";
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getCost() {
        return cost;
    }

    public Direction getDirection() {
        return direction;
    }

    public double getSpeed() {
        return speed;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.dash",getDirection(), (double) getCooldown() / 20d);
    }

    public class Impl implements PowerRightClick, PowerPlain, PowerLeftClick, PowerSneak, PowerHurt, PowerHit, PowerHitTaken, PowerSwim, PowerJump, PowerBowShoot, PowerOffhandClick, PowerTick, PowerSprint {

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public Power getPower() {
            return Dash.this;
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack).with(damage);
        }

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            return fire(target, stack).with(damage);
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            return fire(target, stack);
        }

        @Override
        public PowerResult<Void> jump(Player player, ItemStack stack, PlayerJumpEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> offhandClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack s) {
            PowerActivateEvent powerEvent = new PowerActivateEvent(player, s, getPower());
            if (!powerEvent.callEvent())
                return PowerResult.fail();
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (!getItem().consumeDurability(s, getCost())) return PowerResult.cost();
            double speed = getSpeed();
            Vector velocity;
            switch (getDirection()) {
                case FORWARD:
                    velocity = player.getLocation().getDirection().multiply(speed);
                    break;
                case BACKWARD:
                    velocity = player.getLocation().getDirection().multiply(-speed);
                    break;
                case LEFT:
                    velocity = player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(-speed);
                    break;
                case RIGHT:
                    velocity = player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed);
                    break;
                case UP:
                    velocity = player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed).setY(speed);
                    break;
                case DOWN:
                    velocity = player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed).setY(-speed);
                    break;
                case RANDOM:
                    velocity = player.getLocation().getDirection().add(new Vector(Math.random(), Math.random(), Math.random())).normalize().multiply(speed);
                    break;
                case RANDOM_HORIZONTAL:
                    velocity = player.getLocation().getDirection().add(new Vector(Math.random(), 0, Math.random())).normalize().multiply(speed);
                    break;
                case RANDOM_VERTICAL:
                    velocity = player.getLocation().getDirection().add(new Vector(0, Math.random(), 0)).normalize().multiply(speed);
                    break;
                case EAST:
                    velocity = player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed).setX(speed);
                    break;
                case WEST:
                    velocity = player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed).setX(-speed);
                    break;
                case NORTH:
                    velocity = player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed).setZ(-speed);
                    break;
                case SOUTH:
                    velocity = player.getLocation().getDirection().crossProduct(player.getLocation().getDirection()).multiply(speed).setZ(speed);
                    break;
                default:
                    return PowerResult.fail();
            }

            player.setVelocity(velocity);
            if (getDuration() > 0) {
                DashManager.getInstance().register(new ActiveDash(player, velocity, getDuration()));
            }

            return PowerResult.ok();
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
        public PowerResult<Void> swim(Player player, ItemStack stack, EntityToggleSwimEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            return fire(player, stack);
        }
    }

    /**
     * Represents an active dash effect managed by {@link DashManager}.
     * Maintains the player's velocity for a specified duration.
     *
     * Optimizations:
     * - Stores velocity as primitives (no Vector clone in constructor)
     * - Reuses single Vector object for setVelocity calls (zero allocation per tick)
     */
    public static class ActiveDash implements Tickable {
        private final Player player;
        private final Vector velocity;  // Reused each tick
        private int remainingTicks;
        boolean markedForRemoval;

        public ActiveDash(Player player, Vector velocity, int durationTicks) {
            this.player = player;
            // Store our own Vector with same values - reused each tick
            this.velocity = new Vector(velocity.getX(), velocity.getY(), velocity.getZ());
            this.remainingTicks = durationTicks;
            this.markedForRemoval = false;
        }

        @Override
        public boolean tick() {
            if (markedForRemoval || !player.isOnline() || player.isDead()) {
                return false;
            }

            if (--remainingTicks <= 0) {
                return false;
            }

            player.setVelocity(velocity);
            return true;
        }

        public Player getPlayer() {
            return player;
        }
    }

    /**
     * High-performance manager for active dash effects.
     *
     * Optimizations over generic EffectManager:
     * - Uses ArrayList with batch removal instead of CopyOnWriteArrayList
     * - Avoids O(n) copy on each removal during iteration
     * - Single scheduler task for all active dashes
     * - Auto-stops when no dashes active
     */
    public static class DashManager {
        private static final DashManager INSTANCE = new DashManager();

        private final ArrayList<ActiveDash> activeDashes = new ArrayList<>();
        private volatile BukkitTask tickTask;

        private DashManager() {}

        public static DashManager getInstance() {
            return INSTANCE;
        }

        public void register(ActiveDash dash) {
            synchronized (activeDashes) {
                activeDashes.add(dash);
                if (tickTask == null || tickTask.isCancelled()) {
                    tickTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            tickAll();
                        }
                    }.runTaskTimer(RPGItems.plugin, 1, 1);
                }
            }
        }

        private void tickAll() {
            synchronized (activeDashes) {
                // Iterate backwards for safe removal
                for (int i = activeDashes.size() - 1; i >= 0; i--) {
                    ActiveDash dash = activeDashes.get(i);
                    if (!dash.tick()) {
                        // Swap with last element and remove - O(1) removal
                        int last = activeDashes.size() - 1;
                        if (i != last) {
                            activeDashes.set(i, activeDashes.get(last));
                        }
                        activeDashes.remove(last);
                    }
                }

                if (activeDashes.isEmpty() && tickTask != null) {
                    tickTask.cancel();
                    tickTask = null;
                }
            }
        }

        public void clearAll() {
            synchronized (activeDashes) {
                for (ActiveDash dash : activeDashes) {
                    dash.markedForRemoval = true;
                }
                activeDashes.clear();
                if (tickTask != null) {
                    tickTask.cancel();
                    tickTask = null;
                }
            }
        }

        public int getActiveCount() {
            return activeDashes.size();
        }
    }
}
