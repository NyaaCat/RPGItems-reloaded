package think.rpgitems;

import cat.nyaa.nyaacore.LanguageRepository;
import org.bukkit.plugin.java.JavaPlugin;
public class I18n extends LanguageRepository {
    private static I18n instance = null;
    private final RPGItems plugin;
    private String lang;

    public I18n(RPGItems plugin, String lang) {
        instance = this;
        this.plugin = plugin;
        this.lang = lang;
        load();
    }

    public static String format(String key, Object... args) {
        return instance.getFormatted(key, args);
    }

    public static I18n getInstance() {
        return instance;
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    @Override
    protected String getLanguage() {
        return lang;
    }
}
