package think.rpgitems.api;

import think.rpgitems.api.power.IPower;

import java.util.Collection;

/**
 * Something powers can attach to, e.g. RPGItem
 */
public interface IPowerHolder {
    /**
     * Check if the specified power exists
     *
     * @param classOfPower class of power to check
     * @return whether the power exists or not
     */
    Boolean hasPower(Class<? extends IPower> classOfPower);

    /**
     * Get specified type of power
     *
     * @param classOfPower power type
     * @return all attached power of this type, null if not exists
     */
    Collection<IPower> getPower(Class<? extends IPower> classOfPower);

    /**
     * Remove all of a power type
     *
     * @param classOfPower power type
     * @return removed powers, null if not exists
     */
    Collection<IPower> removePower(Class<? extends IPower> classOfPower);

    /**
     * Remove one specified power
     *
     * @param power the power to be removed
     * @return removed power, null if not exists
     */
    IPower removePower(IPower power);

    /**
     * Attach a power to the holder
     *
     * @param power the power to be added
     * @return if success
     */
    Boolean addPower(IPower power);

    /**
     * Get all attached power types
     *
     * @return power types
     */
    Collection<Class<? extends IPower>> getPowerTypes();
}
