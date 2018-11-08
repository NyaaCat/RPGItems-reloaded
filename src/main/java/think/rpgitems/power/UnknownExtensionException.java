package think.rpgitems.power;

import org.bukkit.plugin.UnknownDependencyException;

public class UnknownExtensionException extends UnknownDependencyException {
    private String name;

    UnknownExtensionException(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
