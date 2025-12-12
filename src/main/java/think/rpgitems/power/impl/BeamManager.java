package think.rpgitems.power.impl;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import think.rpgitems.RPGItems;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralized manager for all active beams.
 * This reduces scheduler overhead by ticking all beams in a single task
 * instead of each beam having its own BukkitRunnable that self-schedules every tick.
 */
public class BeamManager {
    private static final BeamManager INSTANCE = new BeamManager();
    // CopyOnWriteArrayList allows concurrent iteration and modification without synchronization
    private final CopyOnWriteArrayList<ActiveBeam> activeBeams = new CopyOnWriteArrayList<>();
    private volatile BukkitTask tickTask;

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
        activeBeams.add(beam);
        if (tickTask == null || tickTask.isCancelled()) {
            synchronized (this) {
                if (tickTask == null || tickTask.isCancelled()) {
                    tickTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            tickAllBeams();
                        }
                    }.runTaskTimer(RPGItems.plugin, 1, 1);
                }
            }
        }
    }

    /**
     * Ticks all active beams and removes finished ones.
     */
    private void tickAllBeams() {
        for (ActiveBeam beam : activeBeams) {
            try {
                if (!beam.tick()) {
                    activeBeams.remove(beam);
                }
            } catch (Exception e) {
                // Remove beam on exception to prevent infinite error loops
                activeBeams.remove(beam);
                e.printStackTrace();
            }
        }

        // Stop task when no beams are active to save resources
        if (activeBeams.isEmpty() && tickTask != null) {
            synchronized (this) {
                if (activeBeams.isEmpty() && tickTask != null) {
                    tickTask.cancel();
                    tickTask = null;
                }
            }
        }
    }

    /**
     * Gets the number of currently active beams.
     */
    public int getActiveBeamCount() {
        return activeBeams.size();
    }

    /**
     * Clears all active beams (e.g., on plugin disable).
     */
    public void clearAll() {
        activeBeams.clear();
        synchronized (this) {
            if (tickTask != null) {
                tickTask.cancel();
                tickTask = null;
            }
        }
    }
}
