package think.rpgitems.power.impl;

import java.util.HashSet;
import java.util.Set;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

/**
 * Power forcefield.
 *
 * <p>When right clicked, creates a force field around the player, with {@link #radius} {@link
 * #height} based {@link #base} blocks above, lasts for {@link #ttl} ticks.
 */
@SuppressWarnings("WeakerAccess")
@Meta(
        defaultTrigger = "RIGHT_CLICK",
        generalInterface = PowerPlain.class,
        implClass = ForceField.Impl.class)
public class ForceField extends BasePower {
    @Property(order = 1)
    public int radius = 5;

    @Property(order = 2)
    public int height = 30;

    @Property(order = 3)
    public int base = -15;

    @Property(order = 4, required = true)
    public int ttl = 100;

    @Property public boolean requireHurtByEntity = true;

    /* copied from wikipedia */
    private static Set<Location> circlePoints(World w, int x0, int y0, int radius, int l) {
        int x = radius;
        int y = 0;
        int decisionOver2 = 1 - x; // Decision criterion divided by 2 evaluated at x=r, y=0
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
                decisionOver2 += 2 * y + 1; // Change in decision criterion for y -> y+1
            } else {
                x--;
                decisionOver2 += 2 * (y - x) + 1; // Change for y -> y+1, x -> x-1
            }
        }
        return list;
    }

    @Override
    public String getName() {
        return "forcefield";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault(
                "power.forcefield",
                getRadius(),
                getHeight(),
                getBase(),
                (double) getTtl() / 20d,
                (0) / 20d);
    }

    /** Radius of force field */
    public int getRadius() {
        return radius;
    }

    /** Height of force field */
    public int getHeight() {
        return height;
    }

    /** Base of force field */
    public int getBase() {
        return base;
    }

    /** Time to live */
    public int getTtl() {
        return ttl;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    private static class buildWallTask implements Runnable {
        /** The W. */
        final World w;
        /** The Circle points. */
        final Set<Location> circlePoints;
        /** The Was wool. */
        final Set<Location> wasWool,
                /** The Was barrier. */
                wasBarrier;
        /** The L. */
        final int l,
                /** The H. */
                h;
        /** The Ttl. */
        final int ttl;
        /** The Current. */
        int current;
        /** The Id. */
        int id;

        /**
         * Instantiates a new Build wall task.
         *
         * @param w the w
         * @param circlePoints the circle points
         * @param l the l
         * @param h the h
         * @param ttl the ttl
         */
        buildWallTask(World w, Set<Location> circlePoints, int l, int h, int ttl) {
            this.w = w;
            this.circlePoints = circlePoints;
            this.l = l;
            this.h = h;
            current = -1;
            wasWool = new HashSet<>();
            wasBarrier = new HashSet<>();
            this.ttl = ttl;
        }

        @Override
        public void run() {
            if (current != -1) {
                for (Location l : circlePoints) {
                    if (wasWool.contains(l)) {
                        l.add(0, 1, 0);
                        continue;
                    }
                    if (w.getBlockAt(l).getType() == Material.BARRIER) {
                        wasBarrier.add(l.clone());
                        l.add(0, 1, 0);
                        continue;
                    }
                    if (w.getBlockAt(l).getType() == Material.WHITE_WOOL)
                        w.getBlockAt(l).setType(Material.BARRIER);
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
                    if (w.getBlockAt(l).getType() == Material.WHITE_WOOL) {
                        wasWool.add(l.clone());
                        continue;
                    }
                    if (w.getBlockAt(l).getType() == Material.AIR) {
                        for (Entity e : w.getNearbyEntities(l, 2, 2, 2)) {
                            if (e instanceof ItemFrame || e instanceof Painting) {
                                if (e.getLocation().distance(l) < 1.5) continue loop;
                            }
                        }
                        w.getBlockAt(l).setType(Material.WHITE_WOOL);
                    }
                }
            } else {
                Bukkit.getScheduler().cancelTask(id);
                Bukkit.getScheduler()
                        .scheduleSyncDelayedTask(
                                RPGItems.plugin,
                                () -> {
                                    for (int i = h; i >= l; i--) {
                                        for (Location l : circlePoints) {
                                            l.subtract(0, 1, 0);
                                            if (!wasBarrier.contains(l)
                                                    && w.getBlockAt(l).getType()
                                                            == Material.BARRIER) {
                                                w.getBlockAt(l).setType(Material.AIR);
                                            }
                                        }
                                    }
                                },
                                ttl);
            }
        }

        /**
         * Sets id.
         *
         * @param id the id
         */
        public void setId(int id) {
            this.id = id;
        }
    }

    public static class Impl
            implements PowerHitTaken<ForceField>,
                    PowerLeftClick<ForceField>,
                    PowerRightClick<ForceField>,
                    PowerSneak<ForceField>,
                    PowerSprint<ForceField>,
                    PowerPlain<ForceField>,
                    PowerHurt<ForceField> {

        @Override
        public PowerResult<Void> leftClick(
                ForceField power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(ForceField power, Player player, ItemStack stack) {
            World w = player.getWorld();
            int x = player.getLocation().getBlockX();
            int y = player.getLocation().getBlockY();
            int z = player.getLocation().getBlockZ();
            int l = y + power.getBase();
            if (l < 1) l = 1;
            if (l > 255) return PowerResult.noop();
            int h = y + power.getBase() + power.getHeight();
            if (h > 255) h = 255;
            if (h < 1) return PowerResult.noop();

            buildWallTask tsk =
                    new buildWallTask(
                            w, circlePoints(w, x, z, power.getRadius(), l), l, h, power.getTtl());
            tsk.setId(Bukkit.getScheduler().scheduleSyncRepeatingTask(RPGItems.plugin, tsk, 1, 1));
            return PowerResult.ok();
        }

        @Override
        public Class<? extends ForceField> getPowerClass() {
            return ForceField.class;
        }

        @Override
        public PowerResult<Void> rightClick(
                ForceField power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Double> takeHit(
                ForceField power,
                Player target,
                ItemStack stack,
                double damage,
                EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> hurt(
                ForceField power, Player target, ItemStack stack, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> sneak(
                ForceField power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sprint(
                ForceField power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(power, player, stack);
        }
    }
}
