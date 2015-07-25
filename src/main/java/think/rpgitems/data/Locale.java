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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import think.rpgitems.Plugin;

public class Locale {

    private static final String DEFAULT_LANGUAGE = "en_GB";

    private static Locale instance = null;
    private Map<String, String> languageList = new HashMap<>();

    private static HashMap<String, String> localeStrings = new HashMap<>();
    private String usedLocale;

    public static void init(Plugin plugin) {
        if (instance == null) {
            instance = new Locale(plugin);
        } else {
            Plugin.logger.warning("Duplicated init of Locale");
        }
    }

    private static Locale getInstance() {
        return instance;
    }

    private Locale(Plugin plugin) {
        // load configures
        final InputStream langStream = plugin.getResource("languages.txt");
        languageList = loadLocaleStream(langStream);

        usedLocale = plugin.getConfig().getString("language");
        if (!plugin.getConfig().contains("language") || !languageList.containsKey(usedLocale)) {
            plugin.getConfig().set("language", DEFAULT_LANGUAGE);
            plugin.saveConfig();
            usedLocale = DEFAULT_LANGUAGE;
        }

        // load files
        plugin.saveResource("locale/" + DEFAULT_LANGUAGE + ".lang", true);
        plugin.saveResource("locale/" + usedLocale + ".lang", true);

        try {
            File localeFolder = new File(plugin.getDataFolder().getAbsolutePath() + File.separatorChar + "locale");
            localeStrings.clear();
            // use English as fallback
            localeStrings.putAll(loadLocaleStream(new FileInputStream(new File(localeFolder.getAbsolutePath() + File.separatorChar + DEFAULT_LANGUAGE + ".lang"))));
            localeStrings.putAll(loadLocaleStream(new FileInputStream(new File(localeFolder.getAbsolutePath() + File.separatorChar + usedLocale + ".lang"))));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            Plugin.logger.warning("Error when loading language files.");
        }
    }

    private HashMap<String, String> loadLocaleStream(InputStream in, HashMap<String, String> map) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.trim();
                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                }
                String[] args = line.split("=", 2);
                if (args.length == 2) {
                    map.put(args[0].trim(), args[1].trim());
                } else {
                    Plugin.logger.warning("Unknown lang line: " + line);
                }
            }
            return map;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private HashMap<String, String> loadLocaleStream(InputStream in) {
        return loadLocaleStream(in, new HashMap<String, String>());
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
