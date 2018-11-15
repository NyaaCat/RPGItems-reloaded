package think.rpgitems.power;

import org.bukkit.NamespacedKey;
import think.rpgitems.I18n;

public class UnknownPowerException extends Exception {
    private NamespacedKey key;

    public UnknownPowerException(NamespacedKey key) {
        super(key.toString());
        this.key = key;
    }

    public NamespacedKey getKey() {
        return key;
    }

    @Override
    public String getLocalizedMessage() {
        return I18n.format("message.power.unknown", key.toString());
    }
}
