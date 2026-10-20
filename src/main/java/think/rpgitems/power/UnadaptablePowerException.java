package think.rpgitems.power;

/**
 * A power cannot serve a trigger: its impl neither implements the trigger's power interface nor has an
 * adapter to it from one of its general interfaces. Usually the plugin that provides the trigger did not
 * register its adapter (again) after RPGItems cleared the registry.
 * <p>
 * Extends {@link ClassCastException}, which is what {@link PowerManager#adaptPower} threw before.
 */
public class UnadaptablePowerException extends ClassCastException {
    public UnadaptablePowerException(String message) {
        super(message);
    }
}
