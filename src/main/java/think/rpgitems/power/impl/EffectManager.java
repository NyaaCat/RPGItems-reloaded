package think.rpgitems.power.impl;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import think.rpgitems.RPGItems;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Generic centralized manager for tickable effects.
 * This reduces scheduler overhead by ticking all effects in a single task
 * instead of each effect having its own BukkitRunnable.
 *
 * @param <T> The type of tickable effect this manager handles
 */
public abstract class EffectManager<T extends Tickable> {
    // CopyOnWriteArrayList allows concurrent iteration and modification without synchronization
    private final CopyOnWriteArrayList<T> activeEffects = new CopyOnWriteArrayList<>();
    private volatile BukkitTask tickTask;
    private final int tickInterval;

    /**
     * Creates a new EffectManager with the specified tick interval.
     *
     * @param tickInterval The number of ticks between each update (1 = every tick, 3 = every 3 ticks)
     */
    protected EffectManager(int tickInterval) {
        this.tickInterval = tickInterval;
    }

    /**
     * Creates a new EffectManager with a default tick interval of 1 (every tick).
     */
    protected EffectManager() {
        this(1);
    }

    /**
     * Registers an active effect to be ticked by the manager.
     * Starts the global tick task if not already running.
     */
    public void register(T effect) {
        activeEffects.add(effect);
        if (tickTask == null || tickTask.isCancelled()) {
            synchronized (this) {
                if (tickTask == null || tickTask.isCancelled()) {
                    tickTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            tickAllEffects();
                        }
                    }.runTaskTimer(RPGItems.plugin, tickInterval, tickInterval);
                }
            }
        }
    }

    /**
     * Ticks all active effects and removes finished ones.
     */
    private void tickAllEffects() {
        for (T effect : activeEffects) {
            try {
                if (!effect.tick()) {
                    activeEffects.remove(effect);
                }
            } catch (Exception e) {
                // Remove effect on exception to prevent infinite error loops
                activeEffects.remove(effect);
                e.printStackTrace();
            }
        }

        // Stop task when no effects are active to save resources
        if (activeEffects.isEmpty() && tickTask != null) {
            synchronized (this) {
                if (activeEffects.isEmpty() && tickTask != null) {
                    tickTask.cancel();
                    tickTask = null;
                }
            }
        }
    }

    /**
     * Gets the number of currently active effects.
     */
    public int getActiveCount() {
        return activeEffects.size();
    }

    /**
     * Clears all active effects (e.g., on plugin disable).
     */
    public void clearAll() {
        activeEffects.clear();
        synchronized (this) {
            if (tickTask != null) {
                tickTask.cancel();
                tickTask = null;
            }
        }
    }
}
