package think.rpgitems.api;

import org.bukkit.entity.LivingEntity;
import think.rpgitems.api.power.IPower;

/**
 * Represent a prerequisite needed when triggering a power.
 */
public interface IPowerPrerequisite {
    /**
     * Called before a power is triggered.
     * @param trigger The entity, maybe a {@link org.bukkit.entity.Player}, who triggered the power.
     * @param thePower The power to be triggered.
     * @param itemStack Item the power attached to
     * @return The chance of moving to next stage (may trigger the power or be another Prerequisite check).
     *         Between 0-1. le 0 will halt the check, ge 1 will be always move to next stage.
     */
    Double prePowerTrigger(LivingEntity trigger, IPower thePower, IPowerHolder itemStack);
}
