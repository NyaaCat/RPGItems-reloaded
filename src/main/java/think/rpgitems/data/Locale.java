/*
 *  This file is part of RPG Items.
 *
 *  RPG Items is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RPG Items is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RPG Items.  If not, see <http://www.gnu.org/licenses/>.
 */
package think.rpgitems.data;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import think.rpgitems.Plugin;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Locale {

    private static final String DEFAULT_LANGUAGE = "en_GB";

    private static Locale instance = null;
    private static Plugin plugin;

    private static HashMap<String, String> localeStrings = new HashMap<>();
    private String usedLocale;

    public static void init(Plugin pl) {
        if (instance == null) {
            instance = new Locale(pl);
            plugin = pl;
        } else {
            Plugin.logger.warning("Duplicated init of Locale");
        }
    }

    public static void reload() {
        instance = new Locale(plugin);
    }

    private static Locale getInstance() {
        return instance;
    }

    private Locale(Plugin plugin) {
        // load configures
        usedLocale = plugin.getConfig().getString("language");
        if (!plugin.getConfig().contains("language")) {
            plugin.getConfig().set("language", DEFAULT_LANGUAGE);
            plugin.saveConfig();
            usedLocale = DEFAULT_LANGUAGE;
        }

        // load files
        if (plugin.getResource("locale/" + usedLocale + ".lang") != null) {
            plugin.saveResource("locale/" + usedLocale + ".lang", false);
        } else {
            plugin.getLogger().warning("Language: " + usedLocale + "not found. Resetting to:" + DEFAULT_LANGUAGE);
            plugin.getConfig().set("language", DEFAULT_LANGUAGE);
            plugin.saveConfig();
            usedLocale = DEFAULT_LANGUAGE;
        }

        try {
            File localeFolder = new File(plugin.getDataFolder().getAbsolutePath() + File.separatorChar + "locale");
            localeStrings.clear();
            // use English as fallback
            localeStrings.putAll(loadLocaleStream(plugin.getResource("locale/" + DEFAULT_LANGUAGE + ".lang")));
            localeStrings.putAll(loadLocaleStream(new FileInputStream(new File(localeFolder.getAbsolutePath() + File.separatorChar + usedLocale + ".lang"))));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            Plugin.logger.warning("Error when loading language files.");
        }
    }

    private Map<String, String> loadLocaleStream(InputStream in) {
        Map<String, String> map = new HashMap<>();
        Properties prop = new Properties();
        try {
            prop.load(new InputStreamReader(in, "UTF-8"));
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to load language properties");
            return Collections.emptyMap();
        }
        for (String key : prop.stringPropertyNames()) {
            map.put(key, prop.getProperty(key));
        }
        return map;
    }

    public static String getServerLocale() {
        return getInstance().usedLocale;
    }

    public static String get(String key) {
        if (localeStrings == null || !localeStrings.containsKey(key)) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[RPGItems] Unknown translation in " + getServerLocale() + " for " + key + " .");
            return "<" + key + ">";
        }
        return localeStrings.get(key);
    }
}
