package think.rpgitems.power;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;

import org.bukkit.inventory.ItemStack;
import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerConsuming;
import think.rpgitems.power.types.PowerRightClick;

public class PowerForceField extends Power implements PowerRightClick, PowerConsuming {
    public static final String name = "forcefield";
    public int cooldown = 200;
    public int radius = 5;
    public int height = 30;
    public int base = -15;
    public int ttl = 100;
    public int consumption = 0;

    @Override
    public void init(ConfigurationSection s) {
        cooldown = s.getInt("cooldown");
        radius = s.getInt("radius");
        height = s.getInt("height");
        base = s.getInt("base");
        ttl = s.getInt("ttl");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldown);
        s.set("radius", radius);
        s.set("height", height);
        s.set("base", base);
        s.set("ttl", ttl);
        s.set("consumption", consumption);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.forcefield"), radius, height, base, (double) ttl / 20d, (double) cooldown / 20d);
    }

    @Override
    public void rightClick(Player player, ItemStack i, Block clicked) {
        if (item.getHasPermission() && !player.hasPermission(item.getPermission())) return;
        RPGValue value = RPGValue.get(player, item, "projectile.cooldown");
        long cd;
        if (value == null) {
            cd = System.currentTimeMillis() / 50;
            value = new RPGValue(player, item, "projectile.cooldown", cooldown);
        } else {
            cd = value.asLong();
        }
        if (cd > System.currentTimeMillis() / 50) {
            player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown"), ((double) (cd - System.currentTimeMillis() / 50)) / 20d));
            return;
        }
        if(!item.consumeDurability(i,consumption))return;
        value.set(System.currentTimeMillis() / 50 + cooldown);
        World w = player.getWorld();
        int x = player.getLocation().getBlockX();
        int y = player.getLocation().getBlockY();
        int z = player.getLocation().getBlockZ();
        int l = y + base; if (l<1) l=1; if (l > 255) return;
        int h = y + base + height; if (h > 255) h=255; if (h<1) return;

        buildWallTask tsk = new buildWallTask(w, circlePoints(w, x, z, radius, l), l, h, ttl);
        tsk.setId(Bukkit.getScheduler().scheduleSyncRepeatingTask(Plugin.plugin, tsk, 1, 1));
    }

    private static class buildWallTask implements Runnable {
        final World w;
        final Set<Location> circlePoints;
        final Set<Location> wasWool, wasBarrier;
        final int l, h;
        int current;
        int id;
        final int ttl;

        public void setId(int id) {
            this.id = id;
        }

        public buildWallTask(World w, Set<Location> circlePoints, int l, int h, int ttl) {
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
                        l.add(0,1,0);
                        continue;
                    }
                    if (w.getBlockAt(l).getType() == Material.BARRIER) {
                        wasBarrier.add(l.clone());
                        l.add(0,1,0);
                        continue;
                    }
                    if (w.getBlockAt(l).getType() == Material.WOOL)
                        w.getBlockAt(l).setType(Material.BARRIER);
                    l.add(0,1,0);
                }
            }
            if (current == -1) {
                current = l;
            } else {
                current ++;
            }
            if (current <= h) {
                loop: for (Location l : circlePoints) {
                    if (w.getBlockAt(l).getType() == Material.WOOL) {
                        wasWool.add(l.clone());
                        continue;
                    }
                    if (w.getBlockAt(l).getType() == Material.AIR) {
                        for (Entity e : w.getNearbyEntities(l,2,2,2)) {
                            if (e instanceof ItemFrame || e instanceof Painting) {
                                if (e.getLocation().distance(l)<1.5) continue loop;
                            }
                        }
                        w.getBlockAt(l).setType(Material.WOOL);
                    }
                }
            } else {
                Bukkit.getScheduler().cancelTask(id);
                Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin.plugin, new Runnable() {
                    @Override
                    public void run() {
                        for (int i = h; i >= l; i--) {
                            for (Location l : circlePoints) {
                                l.subtract(0,1,0);
                                if (!wasBarrier.contains(l) && w.getBlockAt(l).getType() == Material.BARRIER) {
                                    w.getBlockAt(l).setType(Material.AIR);
                                }
                            }
                        }
                    }
                }, ttl);
            }
        }
    }

    /* copied from wikipedia */
    private Set<Location> circlePoints(World w, int x0, int y0, int radius, int l)
    {
        int x = radius;
        int y = 0;
        int decisionOver2 = 1 - x;   // Decision criterion divided by 2 evaluated at x=r, y=0
        Set<Location> list = new HashSet<>();
        while( y <= x )
        {
            list.add(new Location(w, x + x0, l, y + y0)); // Octant 1
            list.add(new Location(w, y + x0, l, x + y0)); // Octant 2
            list.add(new Location(w,-x + x0, l, y + y0)); // Octant 4
            list.add(new Location(w,-y + x0, l, x + y0)); // Octant 3
            list.add(new Location(w,-x + x0, l,-y + y0)); // Octant 5
            list.add(new Location(w,-y + x0, l,-x + y0)); // Octant 6
            list.add(new Location(w, x + x0, l,-y + y0)); // Octant 8
            list.add(new Location(w, y + x0, l,-x + y0)); // Octant 7
            y++;
            if (decisionOver2<=0)
            {
                decisionOver2 += 2 * y + 1;   // Change in decision criterion for y -> y+1
            }
            else
            {
                x--;
                decisionOver2 += 2 * (y - x) + 1;   // Change for y -> y+1, x -> x-1
            }
        }
        return list;
    }

    public int getConsumption(){
        return consumption;
    }

    public void setConsumption(int cost){
        consumption = cost;
    }
}
