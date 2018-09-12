package think.rpgitems.power;

import org.bukkit.NamespacedKey;

public class UnknownPowerException extends Exception {
    private NamespacedKey key;

    public UnknownPowerException(NamespacedKey key) {
        this.key = key;
    }

    public NamespacedKey getKey() {
        return key;
    }
}
