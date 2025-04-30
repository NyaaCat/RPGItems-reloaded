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

import java.util.HashMap;
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

    private static class buildWallTask implements Runnable {
        final World w;
        final Set<Location> circlePoints;
        final Set<Location> wasWall, wasBarrier;
        final int l, h;
        final int ttl;
        int current;
        int id;
        final Material wallMaterial;
        final Material barrierMaterial;

        buildWallTask(World w, Set<Location> circlePoints, int l, int h, int ttl, Material wallMaterial, Material barrierMaterial) {
            this.w = w;
            this.circlePoints = circlePoints;
            this.l = l;
            this.h = h;
            this.ttl = ttl;
            this.wallMaterial = wallMaterial;
            this.barrierMaterial = barrierMaterial;
            current = -1;
            wasWall = new HashSet<>();
            wasBarrier = new HashSet<>();
        }

        @Override
        public void run() {
            if (current != -1) {
                for (Location l : circlePoints) {
                    if (wasWall.contains(l)) {
                        l.add(0, 1, 0);
                        continue;
                    }
                    if (w.getBlockAt(l).getType() == barrierMaterial) {
                        wasBarrier.add(l.clone());
                        l.add(0, 1, 0);
                        continue;
                    }
                    if (w.getBlockAt(l).getType() == wallMaterial)
                        w.getBlockAt(l).setType(barrierMaterial);
                    l.add(0, 1, 0);
                }
            }
            if (current == -1) {
                current = l;
            } else {
                current++;
            }
            if (current <= h) {
                loop:
                for (Location l : circlePoints) {
                    if (w.getBlockAt(l).getType() == wallMaterial) {
                        wasWall.add(l.clone());
                        continue;
                    }
                    if (w.getBlockAt(l).getType() == Material.AIR) {
                        for (Entity e : w.getNearbyEntities(l, 2, 2, 2)) {
                            if (e instanceof ItemFrame || e instanceof Painting) {
                                if (e.getLocation().distance(l) < 1.5) continue loop;
                            }
                        }
                        w.getBlockAt(l).setType(wallMaterial);
                    }
                }
            } else {
                Bukkit.getScheduler().cancelTask(id);
                Bukkit.getScheduler().scheduleSyncDelayedTask(RPGItems.plugin, () -> {
                    for (int i = h; i >= l; i--) {
                        for (Location l : circlePoints) {
                            l.subtract(0, 1, 0);
                            if (!wasBarrier.contains(l) && w.getBlockAt(l).getType() == barrierMaterial) {
                                w.getBlockAt(l).setType(Material.AIR);
                            }
                        }
                    }
                }, ttl);
            }
        }

        public void setId(int id) {
            this.id = id;
        }
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
            int l = y + getBase();
            if (l < 1) l = 1;
            if (l > 255) return PowerResult.noop();
            int h = y + getBase() + getHeight();
            if (h > 255) h = 255;
            if (h < 1) return PowerResult.noop();

            buildWallTask tsk = new buildWallTask(w, circlePoints(w, x, z, getRadius(), l), l, h, getTtl(), getWallMaterial(), getBarrierMaterial());
            tsk.setId(Bukkit.getScheduler().scheduleSyncRepeatingTask(RPGItems.plugin, tsk, 1, 1));
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
}
