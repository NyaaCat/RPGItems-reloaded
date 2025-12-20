package think.rpgitems.power.impl;

/**
 * Centralized manager for all active rumble effects.
 * This reduces scheduler overhead by ticking all rumbles in a single task
 * instead of each rumble having its own BukkitRunnable.
 */
public class RumbleManager extends EffectManager<ActiveRumble> {
    private static final RumbleManager INSTANCE = new RumbleManager();

    private RumbleManager() {
        super(3); // Tick every 3 ticks, same as original
    }

    public static RumbleManager getInstance() {
        return INSTANCE;
    }
}
