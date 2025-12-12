package think.rpgitems.power.impl;

/**
 * Interface for beams managed by {@link BeamManager}.
 * Implementations should handle their own tick logic and return
 * whether they should continue being ticked.
 */
public interface ActiveBeam {
    /**
     * Performs one tick of beam logic.
     *
     * @return true if the beam should continue, false if it should be removed
     */
    boolean tick();
}
