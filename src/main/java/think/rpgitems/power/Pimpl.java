package think.rpgitems.power;

public interface Pimpl<P extends Power> {
    default Class<P> getPowerClass() {
        return null;
    }

    default <T extends Pimpl> T cast(Class<T> powerClass) {
        if (powerClass.isInstance(this)) {
            return powerClass.cast(this);
        }
        return PowerManager.adaptPower(this, powerClass);
    }
}
