package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import think.rpgitems.RPGItems;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an active force field effect managed by {@link ForceFieldManager}.
 */
public class ActiveForceField implements Tickable {
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
