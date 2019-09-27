package think.rpgitems;

import cat.nyaa.nyaacore.LanguageRepository;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class I18n extends LanguageRepository {
    private static Map<String, I18n> instances = new HashMap<>();
    private final RPGItems plugin;
    private String lang;

    public I18n(RPGItems plugin, String lang) {
        instances.put(lang, this);
        this.plugin = plugin;
        this.lang = lang;
        load(false);
    }

    public static I18n getInstance(CommandSender sender) {
        return getInstance((sender instanceof Player) ? ((Player) sender).getLocale() : RPGItems.plugin.cfg.language);
    }

    public String format(String key, Object... args) {
        return getFormatted(key, args);
    }

    public static String formatDefault(String key, Object... args) {
        return getInstance(RPGItems.plugin.cfg.language).getFormatted(key, args);
    }

    public static I18n getInstance(String lang) {
        return instances.getOrDefault(lang, instances.get(RPGItems.plugin.cfg.language));
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
