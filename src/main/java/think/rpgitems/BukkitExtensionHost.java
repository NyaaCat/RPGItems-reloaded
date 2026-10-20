package think.rpgitems;

import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.File;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/** {@link ExtensionLoader.Host} on the running server. */
final class BukkitExtensionHost implements ExtensionLoader.Host {
    @Override
    public Plugin load(File jar) throws InvalidPluginException, InvalidDescriptionException {
        return Bukkit.getPluginManager().loadPlugin(jar);
    }

    @Override
    public Plugin installed(String name) {
        return Bukkit.getPluginManager().getPlugin(name);
    }

    @Override
    public String pluginName(File jar) {
        try (JarFile file = new JarFile(jar)) {
            ZipEntry entry = file.getEntry("plugin.yml");
            if (entry == null) return null;
            try (InputStream in = file.getInputStream(entry)) {
                return new PluginDescriptionFile(in).getName();
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void disable(Plugin plugin) {
        Bukkit.getPluginManager().disablePlugin(plugin);
    }
}
