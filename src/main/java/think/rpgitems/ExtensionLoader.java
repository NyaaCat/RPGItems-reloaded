package think.rpgitems;

import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads the extension plugins in {@code plugins/RPGItems/ext} and keeps them across {@code /rpgitem reload}.
 * <p>
 * Bukkit cannot unload a plugin. A disabled plugin stays installed under its name, so loading its jar again
 * fails as a duplicate, and Paper closes the class loader of a disabled plugin, so enabling it again leaves it
 * unable to load any class it had not loaded yet. Therefore an extension is loaded once per server run and stays
 * enabled through reloads; after RPGItems clears its registries it re-registers on
 * {@link think.rpgitems.event.RPGItemsReloadEvent}. A reload only loads jars whose plugin is not installed yet.
 */
public final class ExtensionLoader {
    /** The server side of extension loading; Bukkit in production, a stub in tests. */
    public interface Host {
        @Nullable
        Plugin load(File jar) throws InvalidPluginException, InvalidDescriptionException;

        /** the plugin already installed under {@code name}, enabled or not */
        @Nullable
        Plugin installed(String name);

        /** the plugin name declared by {@code jar}, or null if it cannot be read */
        @Nullable
        String pluginName(File jar);

        void disable(Plugin plugin);
    }

    private final Host host;
    private final List<Plugin> managed = new ArrayList<>();
    /** jar file -> the plugin loaded from it */
    private final Map<File, Plugin> loadedFrom = new HashMap<>();

    public ExtensionLoader(Host host) {
        this.host = host;
    }

    /** every extension loaded by RPGItems in this server run, in load order */
    public List<Plugin> managed() {
        return Collections.unmodifiableList(managed);
    }

    /**
     * Loads the jars whose plugin is not installed yet, in file name order, and runs their {@code onLoad}.
     * A jar that fails or loads nothing is reported and skipped; the others still load.
     *
     * @return the plugins loaded by this call; the caller enables them
     */
    public List<Plugin> load(File[] jars) {
        File[] sorted = jars.clone();
        Arrays.sort(sorted, Comparator.comparing(File::getName));
        List<Plugin> loaded = new ArrayList<>();
        for (File file : sorted) {
            File key = file.getAbsoluteFile();
            if (loadedFrom.containsKey(key)) continue;
            String name = host.pluginName(file);
            Plugin existing = name == null ? null : host.installed(name);
            if (existing != null) {
                RPGItems.logger.warning("Extension " + file.getName() + " is not loaded: plugin " + name + " is already installed"
                        + (managed.contains(existing) ? "" : " (not by RPGItems)") + ". Plugins cannot be replaced while the server runs; restart to use this jar.");
                continue;
            }
            try {
                Plugin plugin = host.load(file);
                if (plugin == null) {
                    RPGItems.logger.warning("Extension " + file.getName() + " did not load a plugin");
                    continue;
                }
                plugin.getLogger().info("Loading " + plugin.getName());
                plugin.onLoad();
                managed.add(plugin);
                loadedFrom.put(key, plugin);
                loaded.add(plugin);
                RPGItems.logger.info("Loaded extension: " + plugin.getName());
            } catch (Exception | LinkageError e) {
                RPGItems.logger.log(Level.SEVERE, "Error loading extension: " + file.getName(), e);
            }
        }
        return loaded;
    }

    /**
     * Extension part of {@code /rpgitem reload}, run after the power registry was cleared. Loaded extensions are
     * left alone (they stay enabled and re-register on RPGItemsReloadEvent); only new jars are loaded.
     *
     * @return the newly loaded plugins; the caller enables them
     */
    public List<Plugin> reload(File[] jars) {
        for (Plugin plugin : managed) {
            if (!plugin.isEnabled()) {
                RPGItems.logger.warning("Extension " + plugin.getName() + " is disabled and cannot be enabled again while the server runs; restart to use it.");
            }
        }
        return load(jars);
    }

    /** RPGItems is being disabled. */
    public void disableAll() {
        managed.forEach(host::disable);
    }
}
