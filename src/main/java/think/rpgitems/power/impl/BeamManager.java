package think.rpgitems.power.impl;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import think.rpgitems.RPGItems;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Centralized manager for all active beams.
 * This reduces scheduler overhead by ticking all beams in a single task
 * instead of each beam having its own BukkitRunnable that self-schedules every tick.
 */
public class BeamManager {
    private static final BeamManager INSTANCE = new BeamManager();
    private final List<ActiveBeam> activeBeams = new ArrayList<>();
    private BukkitTask tickTask;

    private BeamManager() {
    }

    public static BeamManager getInstance() {
        return INSTANCE;
    }

    /**
     * Registers an active beam to be ticked by the manager.
     * Starts the global tick task if not already running.
     */
    public void register(ActiveBeam beam) {
        synchronized (activeBeams) {
            if (tickTask == null || tickTask.isCancelled()) {
                tickTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        tickAllBeams();
                    }
                }.runTaskTimer(RPGItems.plugin, 1, 1);
            }
            activeBeams.add(beam);
        }
    }

    /**
     * Ticks all active beams and removes finished ones.
     */
    private void tickAllBeams() {
        synchronized (activeBeams) {
            Iterator<ActiveBeam> iterator = activeBeams.iterator();
            while (iterator.hasNext()) {
                ActiveBeam beam = iterator.next();
                try {
                    if (!beam.tick()) {
                        iterator.remove();
                    }
                } catch (Exception e) {
                    // Remove beam on exception to prevent infinite error loops
                    iterator.remove();
                    e.printStackTrace();
                }
            }

            // Stop task when no beams are active to save resources
            if (activeBeams.isEmpty() && tickTask != null) {
                tickTask.cancel();
                tickTask = null;
            }
        }
    }

    /**
     * Gets the number of currently active beams.
     */
    public int getActiveBeamCount() {
        synchronized (activeBeams) {
            return activeBeams.size();
        }
    }

    /**
     * Clears all active beams (e.g., on plugin disable).
     */
    public void clearAll() {
        synchronized (activeBeams) {
            activeBeams.clear();
            if (tickTask != null) {
                tickTask.cancel();
                tickTask = null;
            }
        }
    }
}
