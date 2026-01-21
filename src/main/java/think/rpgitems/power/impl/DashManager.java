package think.rpgitems.power.impl;

/**
 * Centralized manager for all active dash effects.
 * Ticks every tick to maintain smooth velocity control.
 */
public class DashManager extends EffectManager<ActiveDash> {
    private static final DashManager INSTANCE = new DashManager();

    private DashManager() {
        super(1); // Tick every tick for smooth velocity
    }

    public static DashManager getInstance() {
        return INSTANCE;
    }
}
