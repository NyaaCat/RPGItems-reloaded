package think.rpgitems.power;

import think.rpgitems.power.impl.Beam;
import think.rpgitems.power.impl.Tickable;

/**
 * Interface for beams managed by {@link Beam.BeamManager}.
 * Extends {@link Tickable} for consistency with other effect types.
 */
public interface ActiveBeam extends Tickable {
    // tick() method inherited from Tickable
}
