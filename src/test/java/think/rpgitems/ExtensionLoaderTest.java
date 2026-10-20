package think.rpgitems;

import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extension lifecycle across cold start and {@code /rpgitem reload}.
 * <p>
 * {@link PaperLikeHost} behaves like Paper 26.2: plugins cannot be unloaded, so loading a jar whose plugin
 * name is already installed fails ("attempted to add duplicate plugin identifier"), and disabling a plugin
 * closes its class loader, so a disabled extension cannot be brought back by enabling it again.
 */
class ExtensionLoaderTest {

    static final class Ext {
        final String name;
        boolean enabled;
        boolean classLoaderClosed;
        int onLoad;
        final Plugin plugin;

        Ext(String name) {
            this.name = name;
            this.plugin = TestProxies.of(Plugin.class, name, Map.of(
                    "getName", a -> this.name,
                    "isEnabled", a -> enabled,
                    "getLogger", a -> Logger.getLogger("ext-" + this.name),
                    "onLoad", a -> {
                        onLoad++;
                        return null;
                    }));
        }
    }

    static final class PaperLikeHost implements ExtensionLoader.Host {
        /** jar file name -> plugin name it declares; "null" jars load as null, "boom" jars throw */
        final Map<String, String> jars = new HashMap<>();
        final Map<String, Ext> installed = new LinkedHashMap<>();
        final List<String> loadCalls = new ArrayList<>();
        final List<String> disableCalls = new ArrayList<>();

        File jar(String file, String pluginName) {
            jars.put(file, pluginName);
            return new File("plugins/RPGItems/ext/" + file);
        }

        @Override
        public Plugin load(File jar) throws InvalidPluginException {
            loadCalls.add(jar.getName());
            String name = jars.get(jar.getName());
            if (name.equals("null")) return null;
            if (name.equals("boom")) throw new IllegalStateException("broken extension " + jar.getName());
            if (installed.containsKey(name)) {
                // what Paper does after logging "attempted to add duplicate plugin identifier"
                throw new InvalidPluginException("Plugin didn't load any plugin providers?");
            }
            Ext ext = new Ext(name);
            installed.put(name, ext);
            return ext.plugin;
        }

        @Override
        public Plugin installed(String name) {
            Ext ext = installed.get(name);
            return ext == null ? null : ext.plugin;
        }

        @Override
        public String pluginName(File jar) {
            String name = jars.get(jar.getName());
            return name == null || name.equals("null") || name.equals("boom") ? null : name;
        }

        @Override
        public void disable(Plugin plugin) {
            disableCalls.add(plugin.getName());
            Ext ext = installed.get(plugin.getName());
            ext.enabled = false;
            ext.classLoaderClosed = true;
        }

        /** what the caller does with the plugins handed back for enabling */
        void enable(List<Plugin> plugins) {
            for (Plugin p : plugins) {
                Ext ext = installed.get(p.getName());
                assertFalse(ext.classLoaderClosed, p.getName() + " enabled with a closed class loader: its classes can no longer load");
                ext.enabled = true;
            }
        }

        Ext ext(String name) {
            return installed.get(name);
        }
    }

    private final PaperLikeHost host = new PaperLikeHost();
    private final ExtensionLoader loader = new ExtensionLoader(host);
    private final List<LogRecord> logged = new ArrayList<>();
    private final Handler capture = new Handler() {
        @Override
        public void publish(LogRecord record) {
            logged.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeEach
    void logger() {
        if (RPGItems.logger == null) RPGItems.logger = Logger.getLogger("RPGItems-test");
        RPGItems.logger.addHandler(capture);
    }

    @AfterEach
    void detach() {
        RPGItems.logger.removeHandler(capture);
    }

    private List<String> names(List<Plugin> plugins) {
        return plugins.stream().map(Plugin::getName).toList();
    }

    private List<String> messages(Level level) {
        return logged.stream().filter(r -> r.getLevel() == level).map(LogRecord::getMessage).toList();
    }

    /** cold start: RPGItems#onLoad loads, RPGItems#onEnable enables */
    private void coldStart(File... jars) {
        loader.load(jars);
        host.enable(loader.managed());
    }

    /** /rpgitem reload, after PowerManager.clear() */
    private void reload(File... jars) {
        host.enable(loader.reload(jars));
    }

    @Test
    void coldStartLoadsEveryJarOnce() {
        File minion = host.jar("RPGItems-ext-minion-1.3.2.jar", "RGMinion");
        File other = host.jar("other.jar", "OtherExt");
        coldStart(minion, other);
        assertEquals(List.of("RGMinion", "OtherExt"), names(loader.managed()));
        assertTrue(host.ext("RGMinion").enabled && host.ext("OtherExt").enabled);
        assertEquals(1, host.ext("RGMinion").onLoad);
    }

    @Test
    void reloadKeepsTheLoadedExtensionEnabledAndDoesNotLoadItThroughBukkitAgain() {
        File minion = host.jar("RPGItems-ext-minion-1.3.2.jar", "RGMinion");
        coldStart(minion);
        reload(minion);
        assertEquals(List.of("RPGItems-ext-minion-1.3.2.jar"), host.loadCalls, "a second loadPlugin of the same jar is the duplicate-provider error");
        assertEquals(List.of(), host.disableCalls, "disabling closes the class loader for good");
        assertTrue(host.ext("RGMinion").enabled);
        assertFalse(host.ext("RGMinion").classLoaderClosed);
        assertEquals(List.of("RGMinion"), names(loader.managed()), "still managed, so RPGItems still disables it on shutdown");
        assertTrue(messages(Level.SEVERE).isEmpty(), messages(Level.SEVERE).toString());
    }

    @Test
    void repeatedReloadsNeverTouchTheLoadedExtension() {
        File minion = host.jar("RPGItems-ext-minion-1.3.2.jar", "RGMinion");
        coldStart(minion);
        for (int i = 0; i < 3; i++) reload(minion);
        assertEquals(1, host.loadCalls.size());
        assertEquals(0, host.disableCalls.size());
        assertEquals(1, host.ext("RGMinion").onLoad, "onLoad runs once per plugin instance");
        assertTrue(host.ext("RGMinion").enabled);
        assertEquals(List.of("RGMinion"), names(loader.managed()));
    }

    @Test
    void reloadLoadsOnlyGenuinelyNewJars() {
        File minion = host.jar("RPGItems-ext-minion-1.3.2.jar", "RGMinion");
        coldStart(minion);
        File added = host.jar("new-ext.jar", "NewExt");
        List<Plugin> toEnable = loader.reload(new File[]{minion, added});
        assertEquals(List.of("NewExt"), names(toEnable), "only the new extension is enabled by the reload");
        host.enable(toEnable);
        assertEquals(List.of("RGMinion", "NewExt"), names(loader.managed()));
        assertEquals(1, host.ext("NewExt").onLoad);
        assertTrue(host.ext("RGMinion").enabled && host.ext("NewExt").enabled);
    }

    @Test
    void replacedJarOfALoadedExtensionIsReportedNotLoaded() {
        coldStart(host.jar("RPGItems-ext-minion-1.3.1.jar", "RGMinion"));
        File newer = host.jar("RPGItems-ext-minion-1.3.2.jar", "RGMinion");
        reload(newer);
        assertEquals(1, host.loadCalls.size(), "a jar for an installed plugin name would fail as a duplicate");
        assertTrue(host.ext("RGMinion").enabled);
        List<String> warnings = messages(Level.WARNING);
        assertTrue(warnings.stream().anyMatch(m -> m.contains("RPGItems-ext-minion-1.3.2.jar") && m.contains("RGMinion") && m.contains("restart")), warnings.toString());
        assertTrue(messages(Level.SEVERE).isEmpty(), messages(Level.SEVERE).toString());
    }

    @Test
    void aJarThatLoadsNothingDoesNotStopTheOthers() {
        File nothing = host.jar("a-nothing.jar", "null");
        File minion = host.jar("b-minion.jar", "RGMinion");
        coldStart(nothing, minion);
        assertEquals(List.of("RGMinion"), names(loader.managed()));
    }

    @Test
    void aBrokenJarDoesNotStopTheOthers() {
        File broken = host.jar("a-broken.jar", "boom");
        File minion = host.jar("b-minion.jar", "RGMinion");
        coldStart(broken, minion);
        assertEquals(List.of("RGMinion"), names(loader.managed()));
        assertTrue(messages(Level.SEVERE).stream().anyMatch(m -> m.contains("a-broken.jar")), messages(Level.SEVERE).toString());
    }

    @Test
    void aBrokenJarIsRetriedOnReloadWithoutHarmingTheLoadedOnes() {
        File broken = host.jar("a-broken.jar", "boom");
        File minion = host.jar("b-minion.jar", "RGMinion");
        coldStart(broken, minion);
        reload(broken, minion);
        assertEquals(List.of("a-broken.jar", "b-minion.jar", "a-broken.jar"), host.loadCalls);
        assertTrue(host.ext("RGMinion").enabled);
    }

    @Test
    void jarsLoadInFileNameOrder() {
        File b = host.jar("b.jar", "B");
        File a = host.jar("a.jar", "A");
        coldStart(b, a);
        assertEquals(List.of("A", "B"), names(loader.managed()), "File#listFiles has no defined order");
    }

    @Test
    void disablingRPGItemsDisablesEveryManagedExtension() {
        coldStart(host.jar("a.jar", "A"), host.jar("b.jar", "B"));
        loader.disableAll();
        assertEquals(List.of("A", "B"), host.disableCalls);
    }
}
