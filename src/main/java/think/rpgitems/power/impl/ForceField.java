package think.rpgitems.power.impl;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import java.util.HashSet;
import java.util.Set;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power forcefield.
 * <p>
 * When right-clicked, creates a force field around the player,
 * with {@link #radius} {@link #height} based {@link #base} blocks above,
 * lasts for {@link #ttl} ticks.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = ForceField.Impl.class)
public class ForceField extends BasePower {
    @Property(order = 0)
    public int cooldown = 200;
    @Property(order = 1)
    public int radius = 5;
    @Property(order = 2)
    public int height = 30;
    @Property(order = 3)
    public int base = -15;
    @Property(order = 4, required = true)
    public int ttl = 100;
    @Property
    public int cost = 0;
    @Property
    public boolean requireHurtByEntity = true;
    @Property
    public Material wallMaterial = Material.WHITE_WOOL;
    @Property
    public Material barrierMaterial = Material.BARRIER;

    /* copied from wikipedia */
    private Set<Location> circlePoints(World w, int x0, int y0, int radius, int l) {
        int x = radius;
        int y = 0;
        int decisionOver2 = 1 - x;   // Decision criterion divided by 2 evaluated at x=r, y=0
        Set<Location> list = new HashSet<>();
        while (y <= x) {
            list.add(new Location(w, x + x0, l, y + y0)); // Octant 1
            list.add(new Location(w, y + x0, l, x + y0)); // Octant 2
            list.add(new Location(w, -x + x0, l, y + y0)); // Octant 4
            list.add(new Location(w, -y + x0, l, x + y0)); // Octant 3
            list.add(new Location(w, -x + x0, l, -y + y0)); // Octant 5
            list.add(new Location(w, -y + x0, l, -x + y0)); // Octant 6
            list.add(new Location(w, x + x0, l, -y + y0)); // Octant 8
            list.add(new Location(w, y + x0, l, -x + y0)); // Octant 7
            y++;
            if (decisionOver2 <= 0) {
                decisionOver2 += 2 * y + 1;   // Change in decision criterion for y -> y+1
            } else {
                x--;
                decisionOver2 += 2 * (y - x) + 1;   // Change for y -> y+1, x -> x-1
            }
        }
        return list;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    @Override
    public String getName() {
        return "forcefield";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.forcefield", getRadius(), getHeight(), getBase(), (double) getTtl() / 20d, (double) getCooldown() / 20d);
    }

    /**
     * Radius of force field
     */
    public int getRadius() {
        return radius;
    }

    /**
     * Height of force field
     */
    public int getHeight() {
        return height;
    }

    /**
     * Base of force field
     */
    public int getBase() {
        return base;
    }

    /**
     * Time to live
     */
    public int getTtl() {
        return ttl;
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    public Material getBarrierMaterial() {
        return barrierMaterial;
    }

    public Material getWallMaterial() {
        return wallMaterial;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public class Impl implements PowerHitTaken, PowerLeftClick, PowerRightClick, PowerSneak, PowerSprint, PowerPlain, PowerHurt, PowerConsume, PowerJump, PowerSwim {

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            PowerActivateEvent powerEvent = new PowerActivateEvent(player, stack, getPower());
            if (!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!getWallMaterial().isBlock()||!getBarrierMaterial().isBlock()) {
                throw new IllegalArgumentException("The wall material or barrier material isn't a block: " + getWallMaterial() + ", " + getBarrierMaterial());
            }
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            World w = player.getWorld();
            int x = player.getLocation().getBlockX();
            int y = player.getLocation().getBlockY();
            int z = player.getLocation().getBlockZ();
            int baseLevel = y + getBase();
            if (baseLevel < 1) baseLevel = 1;
            if (baseLevel > 255) return PowerResult.noop();
            int maxHeight = y + getBase() + getHeight();
            if (maxHeight > 255) maxHeight = 255;
            if (maxHeight < 1) return PowerResult.noop();

            ForceFieldManager.getInstance().register(
                    new ActiveForceField(w, circlePoints(w, x, z, getRadius(), baseLevel),
                            baseLevel, maxHeight, getTtl(), getWallMaterial(), getBarrierMaterial())
            );
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return ForceField.this;
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack);
            }
            return PowerResult.noop();
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

    /**
     * Centralized manager for all active force field effects.
     * This reduces scheduler overhead by ticking all force fields in a single task
     * instead of each force field having its own scheduled task.
     */
    public static class ForceFieldManager extends EffectManager<ActiveForceField> {
        private static final ForceFieldManager INSTANCE = new ForceFieldManager();

        private ForceFieldManager() {
            super(1); // Tick every tick, same as original
        }

        public static ForceFieldManager getInstance() {
            return INSTANCE;
        }
    }

    /**
     * Represents an active force field effect managed by {@link ForceFieldManager}.
     */
    public static class ActiveForceField implements Tickable {
        private final World world;
        private final Set<Location> circlePoints;
        private final Set<Location> wasWall;
        private final Set<Location> wasBarrier;
        private final int baseLevel;
        private final int maxHeight;
        private final int ttl;
        private final Material wallMaterial;
        private final Material barrierMaterial;
        private int currentLevel;
        private boolean buildingComplete;

        public ActiveForceField(World world, Set<Location> circlePoints, int baseLevel, int maxHeight,
                                int ttl, Material wallMaterial, Material barrierMaterial) {
            this.world = world;
            this.circlePoints = circlePoints;
            this.baseLevel = baseLevel;
            this.maxHeight = maxHeight;
            this.ttl = ttl;
            this.wallMaterial = wallMaterial;
            this.barrierMaterial = barrierMaterial;
            this.currentLevel = -1;
            this.wasWall = new HashSet<>();
            this.wasBarrier = new HashSet<>();
            this.buildingComplete = false;
        }

        @Override
        public boolean tick() {
            if (buildingComplete) {
                return false;
            }

            // Convert previous level's wall blocks to barrier blocks
            if (currentLevel != -1) {
                for (Location loc : circlePoints) {
                    if (wasWall.contains(loc)) {
                        loc.add(0, 1, 0);
                        continue;
                    }
                    if (world.getBlockAt(loc).getType() == barrierMaterial) {
                        wasBarrier.add(loc.clone());
                        loc.add(0, 1, 0);
                        continue;
                    }
                    if (world.getBlockAt(loc).getType() == wallMaterial) {
                        world.getBlockAt(loc).setType(barrierMaterial);
                    }
                    loc.add(0, 1, 0);
                }
            }

            // Initialize or increment current level
            if (currentLevel == -1) {
                currentLevel = baseLevel;
            } else {
                currentLevel++;
            }

            // Build wall at current level
            if (currentLevel <= maxHeight) {
                for (Location loc : circlePoints) {
                    if (world.getBlockAt(loc).getType() == wallMaterial) {
                        wasWall.add(loc.clone());
                        continue;
                    }
                    if (world.getBlockAt(loc).getType() == Material.AIR) {
                        boolean nearHangingEntity = false;
                        for (Entity e : world.getNearbyEntities(loc, 2, 2, 2)) {
                            if (e instanceof ItemFrame || e instanceof Painting) {
                                if (e.getLocation().distance(loc) < 1.5) {
                                    nearHangingEntity = true;
                                    break;
                                }
                            }
                        }
                        if (!nearHangingEntity) {
                            world.getBlockAt(loc).setType(wallMaterial);
                        }
                    }
                }
                return true;
            } else {
                // Building complete, schedule cleanup
                buildingComplete = true;
                scheduleCleanup();
                return false;
            }
        }

        private void scheduleCleanup() {
            Bukkit.getScheduler().scheduleSyncDelayedTask(RPGItems.plugin, () -> {
                for (int i = maxHeight; i >= baseLevel; i--) {
                    for (Location loc : circlePoints) {
                        loc.subtract(0, 1, 0);
                        if (!wasBarrier.contains(loc) && world.getBlockAt(loc).getType() == barrierMaterial) {
                            world.getBlockAt(loc).setType(Material.AIR);
                        }
                    }
                }
            }, ttl);
        }
    }
}
