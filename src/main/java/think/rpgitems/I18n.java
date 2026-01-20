package think.rpgitems;

import cat.nyaa.nyaacore.LanguageRepository;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.HexColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.chat.BaseComponent;
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
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IllegalFormatConversionException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class I18n extends LanguageRepository {
    // Pre-compiled patterns for replaceLegacyColorCodes() optimization
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("[§&]x([§&][0-9a-fA-F]){6}");
    private static final Pattern HEX_STRIP_PATTERN = Pattern.compile("[§&]x|[§&]");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("[§&]([0-9a-fA-Fk-orK-OR])");
    private static final Map<String, String> FORMAT_MAP = Map.ofEntries(
            Map.entry("0", "black"), Map.entry("1", "dark_blue"), Map.entry("2", "dark_green"), Map.entry("3", "dark_aqua"),
            Map.entry("4", "dark_red"), Map.entry("5", "dark_purple"), Map.entry("6", "gold"), Map.entry("7", "gray"),
            Map.entry("8", "dark_gray"), Map.entry("9", "blue"), Map.entry("a", "green"), Map.entry("b", "aqua"),
            Map.entry("c", "red"), Map.entry("d", "light_purple"), Map.entry("e", "yellow"), Map.entry("f", "white"),
            Map.entry("k", "obf"), Map.entry("l", "b"), Map.entry("m", "st"),
            Map.entry("n", "u"), Map.entry("o", "i"), Map.entry("r", "reset")
    );
    private static final Map<String, I18n> instances = new HashMap<>();
    private final RPGItems plugin;
    protected Map<String, String> map = new HashMap<>();
    private final String lang;
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public I18n(RPGItems plugin, String lang) {
        instances.put(lang.toLowerCase(), this);
        this.plugin = plugin;
        this.lang = lang;
        loadResourceLanguage(lang);
        save(lang + ".template");
        loadLocalLanguage(lang + ".custom");
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
                map.put(path, HexColorUtils.hexColored(section.getString(key)));
            } else if (section.isConfigurationSection(key)) {
                loadLanguageSection(map, section.getConfigurationSection(key), path + ".", ignoreInternal, ignoreNormal);
            }
        }
    }

    private static Component parse(String string){
        return mm.deserialize(replaceLegacyColorCodes(string)).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    // helper function to load language map
    private static void loadResourceMap(Plugin plugin, String codeName,
                                        Map<String, String> targetMap, boolean ignoreInternal, boolean ignoreNormal) {
        if (plugin == null || codeName == null || targetMap == null) throw new IllegalArgumentException();
        InputStream stream = plugin.getResource("lang/" + codeName + ".yml");
        if (stream != null) {
            YamlConfiguration section = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
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

    public static I18n getInstance(CommandSender sender) {
        return getInstance((sender instanceof Player) ? ((Player) sender).getLocale() : RPGItems.plugin.cfg.language);
    }

    public static I18n getInstance(String lang) {
        return instances.getOrDefault(lang.toLowerCase(), instances.get(RPGItems.plugin.cfg.language.toLowerCase()));
    }

    public static String formatDefault(String key, Object... args) {
        return getInstance(RPGItems.plugin.cfg.language).getFormatted(key, args);
    }

    public static void sendMessage(CommandSender target, String template, Object... args) {
        I18n i18n = I18n.getInstance(target);
        target.sendMessage(parse(i18n.getFormatted(template, args)));
    }

    public static void sendMessage(CommandSender target, String template, Map<String, Component> map, Object... args) {
        new Message("").append(I18n.getInstance(target).getFormatted(template, args), map).send(target);
    }

    public static String replaceLegacyColorCodes(String text) {
        // Single-pass hex color replacement using pre-compiled pattern
        Matcher hexMatcher = HEX_COLOR_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        while (hexMatcher.find()) {
            String hex = HEX_STRIP_PATTERN.matcher(hexMatcher.group()).replaceAll("");
            hexMatcher.appendReplacement(result, "<#" + hex + ">");
        }
        hexMatcher.appendTail(result);
        text = result.toString();

        // Single-pass legacy color code replacement using pre-compiled pattern
        return LEGACY_COLOR_PATTERN.matcher(text).replaceAll(m ->
                "<" + FORMAT_MAP.getOrDefault(m.group(1).toLowerCase(), m.group(1)) + ">");
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
    public boolean hasKey(String key) {
        if (map.containsKey(key)) return true;
        return super.hasKey(key);
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
