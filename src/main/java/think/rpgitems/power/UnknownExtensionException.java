package think.rpgitems.power;

import org.bukkit.plugin.UnknownDependencyException;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;

public class UnknownExtensionException extends UnknownDependencyException {
    private String name;

    UnknownExtensionException(String name) {
        super(name);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getLocalizedMessage() {
        return I18n.getInstance(RPGItems.plugin.cfg.language).format("message.error.unknown.extension", name);
    }
}
