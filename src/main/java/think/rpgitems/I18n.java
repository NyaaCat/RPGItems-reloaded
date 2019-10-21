package think.rpgitems;

import cat.nyaa.nyaacore.LanguageRepository;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.IllegalFormatConversionException;
import java.util.Map;

public class I18n extends LanguageRepository {
    private static Map<String, I18n> instances = new HashMap<>();
    private final RPGItems plugin;
    private String lang;

    protected Map<String, String> map = new HashMap<>();

    public I18n(RPGItems plugin, String lang) {
        instances.put(lang, this);
        this.plugin = plugin;
        this.lang = lang;
        loadResourceLanguage(lang);
        save(lang + ".template");
        loadLocalLanguage(lang + ".custom");
    }

    /**
     * Save language file back to disk using given file name
     */
    public void save(String fileName) {
        Plugin plugin = getPlugin();
        File localLangFile = new File(plugin.getDataFolder(), fileName + ".yml");
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            for (String key : map.keySet()) {
                yaml.set(key, map.get(key));
            }
            yaml.save(localLangFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Cannot save language file: " + fileName + ".yml");
        }
    }

    /**
     * add all language items from section into language map recursively
     * overwrite existing items
     * The '&' will be transformed to color code.
     *
     * @param section        source section
     * @param prefix         used in recursion to determine the proper prefix
     * @param ignoreInternal ignore keys prefixed with `internal'
     * @param ignoreNormal   ignore keys not prefixed with `internal'
     */
    private static void loadLanguageSection(Map<String, String> map, ConfigurationSection section, String prefix, boolean ignoreInternal, boolean ignoreNormal) {
        if (map == null || section == null || prefix == null) return;
        for (String key : section.getKeys(false)) {
            String path = prefix + key;
            if (section.isString(key)) {
                if (path.startsWith("internal") && ignoreInternal) continue;
                if (!path.startsWith("internal") && ignoreNormal) continue;
                map.put(path, ChatColor.translateAlternateColorCodes('&', section.getString(key)));
            } else if (section.isConfigurationSection(key)) {
                loadLanguageSection(map, section.getConfigurationSection(key), path + ".", ignoreInternal, ignoreNormal);
            }
        }
    }


    // helper function to load language map
    private static void loadResourceMap(Plugin plugin, String codeName,
                                        Map<String, String> targetMap, boolean ignoreInternal, boolean ignoreNormal) {
        if (plugin == null || codeName == null || targetMap == null) throw new IllegalArgumentException();
        InputStream stream = plugin.getResource("lang/" + codeName + ".yml");
        if (stream != null) {
            YamlConfiguration section = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, Charset.forName("UTF8")));
            loadLanguageSection(targetMap, section, "", ignoreInternal, ignoreNormal);
        }
    }

    // helper function to load language map
    private static void loadLocalMap(Plugin plugin, String codeName,
                                     Map<String, String> targetMap, boolean ignoreInternal, boolean ignoreNormal) {
        if (plugin == null || codeName == null || targetMap == null) throw new IllegalArgumentException();
        if (Boolean.parseBoolean(System.getProperty("nyaautils.i18n.refreshLangFiles", "false"))) return;
        File langFile = new File(plugin.getDataFolder(), codeName + ".yml");
        if (langFile.exists() && langFile.isFile()) {
            YamlConfiguration section = YamlConfiguration.loadConfiguration(langFile);
            loadLanguageSection(targetMap, section, "", ignoreInternal, ignoreNormal);
        }
    }

    /**
     * Load specified resource language map
     *
     * @param fileName
     */
    protected void loadResourceLanguage(String fileName) {
        loadResourceMap(getPlugin(), fileName, map, false, false);
    }

    /**
     * Load specified local language map
     *
     * @param fileName
     */
    protected void loadLocalLanguage(String fileName) {
        loadLocalMap(getPlugin(), fileName, map, false, false);
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

    /**
     * Get the language item then format with `para` by {@link String#format(String, Object...)}
     */
    @Override
    public String getFormatted(String key, Object... para) {
        String val = map.get(key);
        if (val == null) {
            return super.getFormatted(key, para);
        } else {
            try {
                return String.format(val, para);
            } catch (IllegalFormatConversionException e) {
                e.printStackTrace();
                getPlugin().getLogger().warning("Corrupted language key: " + key);
                getPlugin().getLogger().warning("val: " + val);
                StringBuilder keyBuilder = new StringBuilder();
                for (Object obj : para) {
                    keyBuilder.append("#<").append(obj.toString()).append(">");
                }
                String params = keyBuilder.toString();
                getPlugin().getLogger().warning("params: " + params);
                return "CORRUPTED_LANG<" + key + ">" + params;
            }
        }
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
