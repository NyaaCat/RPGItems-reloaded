package think.rpgitems.power;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.Property;
import think.rpgitems.power.types.PowerRightClick;

import java.util.HashSet;
import java.util.Set;

/**
 * Power forcefield.
 * <p>
 * When right clicked, creates a force field around the player,
 * with {@link #radius} {@link #height} based {@link #base} blocks above,
 * lasts for {@link #ttl} ticks.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public class PowerForceField extends Power implements PowerRightClick {
    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public int cooldownTime = 200;
    /**
     * Radius of force field
     */
    @Property(order = 1)
    public int radius = 5;
    /**
     * Height of force field
     */
    @Property(order = 2)
    public int height = 30;
    /**
     * Base of force field
     */
    @Property(order = 3)
    public int base = -15;
    /**
     * Time to live
     */
    @Property(order = 4, required = true)
    public int ttl = 100;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getInt("cooldown");
        radius = s.getInt("radius");
        height = s.getInt("height");
        base = s.getInt("base");
        ttl = s.getInt("ttl");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("radius", radius);
        s.set("height", height);
        s.set("base", base);
        s.set("ttl", ttl);
        s.set("consumption", consumption);
    }

    @Override
    public String getName() {
        return "forcefield";
    }

    @Override
    public String displayText() {
        return I18n.format("power.forcefield", radius, height, base, (double) ttl / 20d, (double) cooldownTime / 20d);
    }

    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true)) return;
        if (!checkCooldown(player, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        World w = player.getWorld();
        int x = player.getLocation().getBlockX();
        int y = player.getLocation().getBlockY();
        int z = player.getLocation().getBlockZ();
        int l = y + base;
        if (l < 1) l = 1;
        if (l > 255) return;
        int h = y + base + height;
        if (h > 255) h = 255;
        if (h < 1) return;

        buildWallTask tsk = new buildWallTask(w, circlePoints(w, x, z, radius, l), l, h, ttl);
        tsk.setId(Bukkit.getScheduler().scheduleSyncRepeatingTask(RPGItems.plugin, tsk, 1, 1));
    }

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

    private static class buildWallTask implements Runnable {
        /**
         * The W.
         */
        final World w;
        /**
         * The Circle points.
         */
        final Set<Location> circlePoints;
        /**
         * The Was wool.
         */
        final Set<Location> wasWool, /**
         * The Was barrier.
         */
        wasBarrier;
        /**
         * The L.
         */
        final int l, /**
         * The H.
         */
        h;
        /**
         * The Ttl.
         */
        final int ttl;
        /**
         * The Current.
         */
        int current;
        /**
         * The Id.
         */
        int id;

        /**
         * Instantiates a new Build wall task.
         *
         * @param w            the w
         * @param circlePoints the circle points
         * @param l            the l
         * @param h            the h
         * @param ttl          the ttl
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

        /**
         * Sets id.
         *
         * @param id the id
         */
        public void setId(int id) {
            this.id = id;
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
                Bukkit.getScheduler().scheduleSyncDelayedTask(RPGItems.plugin, () -> {
                    for (int i = h; i >= l; i--) {
                        for (Location l : circlePoints) {
                            l.subtract(0, 1, 0);
                            if (!wasBarrier.contains(l) && w.getBlockAt(l).getType() == Material.BARRIER) {
                                w.getBlockAt(l).setType(Material.AIR);
                            }
                        }
                    }
                }, ttl);
            }
        }
    }

}
