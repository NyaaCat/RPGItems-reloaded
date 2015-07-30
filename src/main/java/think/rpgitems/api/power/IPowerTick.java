package think.rpgitems.api.power;

/**
 * Powers triggered at every tick
 */
public interface IPowerTick extends IPower {
    /**
     * Invoked on ticks
     * may not be invoked at every tick depends on configure
     *
     * @param sinceLast ticks passed since last invoke
     * @return power triggered or not
     */
    Boolean onTick(Long sinceLast);
}
