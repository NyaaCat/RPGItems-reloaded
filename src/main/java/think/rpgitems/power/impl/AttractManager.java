package think.rpgitems.power.impl;

/**
 * Centralized manager for all active attract effects.
 * This reduces scheduler overhead by ticking all attracts in a single task
 * instead of each attract having its own BukkitRunnable.
 */
public class AttractManager extends EffectManager<ActiveAttract> {
    private static final AttractManager INSTANCE = new AttractManager();

    private AttractManager() {
        super(1); // Tick every tick, same as original
    }

    public static AttractManager getInstance() {
        return INSTANCE;
    }
}
