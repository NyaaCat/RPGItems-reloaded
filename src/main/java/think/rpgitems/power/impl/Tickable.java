package think.rpgitems.power.impl;

/**
 * Interface for effects managed by centralized managers.
 * Implementations should handle their own tick logic and return
 * whether they should continue being ticked.
 */
public interface Tickable {
    /**
     * Performs one tick of effect logic.
     *
     * @return true if the effect should continue, false if it should be removed
     */
    boolean tick();
}
