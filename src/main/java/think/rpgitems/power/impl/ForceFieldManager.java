package think.rpgitems.power.impl;

/**
 * Centralized manager for all active force field effects.
 * This reduces scheduler overhead by ticking all force fields in a single task
 * instead of each force field having its own scheduled task.
 */
public class ForceFieldManager extends EffectManager<ActiveForceField> {
    private static final ForceFieldManager INSTANCE = new ForceFieldManager();

    private ForceFieldManager() {
        super(1); // Tick every tick, same as original
    }

    public static ForceFieldManager getInstance() {
        return INSTANCE;
    }
}
