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

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import think.rpgitems.Plugin;

public class Locale {

    private static HashMap<String, String> localeStrings = new HashMap<String, String>();
    private static File localeFolder;
    private static File dataFolder;
    private static String usedLocale;
    private String version;

    public Locale(Plugin plugin) {
        version = plugin.getDescription().getVersion();
        if (!plugin.getConfig().getString("pluginVersion", "0.0").equals(version)) {
            plugin.getConfig().set("pluginVersion", version);
            plugin.saveConfig();
        }
        dataFolder = plugin.getDataFolder();
        localeFolder = new File(dataFolder.getAbsolutePath() + File.separatorChar + "locale");
        if (!localeFolder.exists())
            writeLocaleFolder();
        usedLocale = plugin.getConfig().getString("language");
        if (!plugin.getConfig().contains("language")) {
            plugin.getConfig().set("language", "en_GB");
            plugin.saveConfig();
            usedLocale = "en_GB";
        } else {
            testUsedLocale();
        }
        reloadLocales(plugin);
    }

    private static void testUsedLocale() {
        File[] locales = localeFolder.listFiles();

        if (locales != null)
            for (File loc : locales)
                if (usedLocale.equalsIgnoreCase(loc.getName().replace(".lang", "")))
                    return;
        InputStream localeSource = Plugin.plugin.getResource("locale/" + usedLocale + ".lang");
        if (localeSource != null) {
            OutputStream localeDest = null;
            try {
                localeDest = new FileOutputStream(new File(localeFolder, usedLocale + ".lang"));
            } catch (FileNotFoundException e1) {
            }

            int read = 0;
            byte[] bytes = new byte[1024];

            try {
                while ((read = localeSource.read(bytes)) != -1) {
                    localeDest.write(bytes, 0, read);
                }
                Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[RPGItems] Warning: The locale you set (" + usedLocale + ") did not exist and has to be configured!");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (localeSource != null)
                    try {
                        localeSource.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                if (localeDest != null)
                    try {
                        localeDest.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[RPGItems] Error: The language you've set (" + usedLocale + ") isn't supported or doesn't exist!");
    }

    private static void writeLocaleFolder() {
        Bukkit.getConsoleSender().sendMessage("[RPGItems] Started writing locale folder.");
        localeFolder.mkdirs();
        JarFile jar = null;
        try {
            jar = new JarFile(dataFolder.getParentFile().getAbsolutePath() + File.separator + "RPGItems.jar");
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                if (entryName.startsWith("locale/") && entryName.endsWith(".lang")) {
                    InputStream localeSource = Plugin.plugin.getResource(entryName);
                    OutputStream localeDest = new FileOutputStream(new File(localeFolder, entryName.replace("locale/", "")));

                    int read = 0;
                    byte[] bytes = new byte[1024];

                    try {
                        while ((read = localeSource.read(bytes)) != -1) {
                            localeDest.write(bytes, 0, read);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (localeSource != null)
                            try {
                                localeSource.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        if (localeDest != null)
                            try {
                                localeDest.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    }
                }
            }
            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[RPGItems] Successfully generated locale folder!");
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (jar != null)
                try {
                    jar.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

    }

    public static void reloadLocales(Plugin plugin) {
        localeStrings.clear();
        try {
            localeStrings = loadLocaleStream(new FileInputStream(new File(localeFolder.getAbsolutePath() + File.separatorChar + usedLocale + ".lang")));
        } catch (FileNotFoundException e) {
        }
    }

    private static HashMap<String, String> loadLocaleStream(InputStream in, HashMap<String, String> map) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;
                String[] args = line.split("=");
                map.put(args[0].trim(), args[1].trim());
            }
            return map;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static HashMap<String, String> loadLocaleStream(InputStream in) {
        return loadLocaleStream(in, new HashMap<String, String>());
    }

    public static String getServerLocale() {
        return usedLocale;
    }

    public static String get(String key) {
        if (localeStrings == null || !localeStrings.containsKey(key)) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[RPGItems] Unknown translation in " + usedLocale + " for " + key + ".");
            return ""; // TODO: Add fallback to known language?
        }
        return localeStrings.get(key);
    }
}
