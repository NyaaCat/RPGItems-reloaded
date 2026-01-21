package think.rpgitems.power.impl;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import think.rpgitems.RPGItems;

import java.util.ArrayList;

/**
 * High-performance manager for active dash effects.
 *
 * Optimizations over generic EffectManager:
 * - Uses ArrayList with batch removal instead of CopyOnWriteArrayList
 * - Avoids O(n) copy on each removal during iteration
 * - Single scheduler task for all active dashes
 * - Auto-stops when no dashes active
 */
public class DashManager {
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
