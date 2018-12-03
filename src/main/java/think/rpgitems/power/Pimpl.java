package think.rpgitems.power;

public interface Pimpl {

    Power getPower();

    default <T extends Pimpl> T cast(Class<T> powerClass) {
        if (powerClass.isInstance(this)) {
            return powerClass.cast(this);
        }
        return PowerManager.adaptPower(this, powerClass);
    }
}
