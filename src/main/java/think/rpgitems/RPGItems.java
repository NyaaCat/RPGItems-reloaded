package think.rpgitems;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.data.Font;
import think.rpgitems.item.ItemManager;
import think.rpgitems.power.PowerManager;
import think.rpgitems.power.PowerTicker;
import think.rpgitems.power.impl.BasePower;
import think.rpgitems.support.WGSupport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class RPGItems extends JavaPlugin {

    public static Logger logger;
    public static RPGItems plugin;
    public static Events listener;
    public Handler commandHandler;
    List<Plugin> managedPlugins = new ArrayList<>();
    public I18n i18n;
    public Configuration cfg;

    @Override
    public void onLoad() {
        plugin = this;
        logger = this.getLogger();
        PowerManager.registerPowers(RPGItems.plugin, BasePower.class.getPackage().getName());
        PowerManager.addDescriptionResolver(RPGItems.plugin, (power, property) -> {
            if (property == null) {
                @LangKey(skipCheck = true) String powerKey = "power.properties." + power.getKey() + ".main_description";
                return I18n.format(powerKey);
            }
            @LangKey(skipCheck = true) String key = "power.properties." + power.getKey() + "." + property;
            if (I18n.getInstance().hasKey(key)) {
                return I18n.format(key);
            }
            @LangKey(skipCheck = true) String baseKey = "power.properties.base." + property;
            if (I18n.getInstance().hasKey(baseKey)) {
                return I18n.format(baseKey);
            }
            return null;
        });
        saveDefaultConfig();
        Font.load();
        WGSupport.load();

        loadExtensions();
    }

    public void loadExtensions() {
        File extDir = new File(plugin.getDataFolder(), "ext");
        if (extDir.isDirectory() || extDir.mkdirs()) {
            File[] files = extDir.listFiles((d, n) -> n.endsWith(".jar"));
            if (files == null) return;
            for (File file : files) {
                try {
                    Plugin plugin = Bukkit.getPluginManager().loadPlugin(file);
                    String message = String.format("Loading %s", plugin.getDescription().getFullName());
                    plugin.getLogger().info(message);
                    plugin.onLoad();
                    managedPlugins.add(plugin);
                    logger.info("Loaded extension: " + plugin.getName());
                } catch (InvalidPluginException | InvalidDescriptionException e) {
                    logger.warning("Error loading extension: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onEnable() {
        plugin = this;
        cfg = new Configuration(this);
        cfg.load();

        if (Bukkit.class.getPackage().getImplementationVersion().startsWith("git-Bukkit-")) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "======================================");
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "RPGItems plugin require Spigot API, Please make sure you are using Spigot.");
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "======================================");
        }
        try {
            Bukkit.spigot();
        } catch (NoSuchMethodError e) {
            getCommand("rpgitem").setExecutor((sender, command, label, args) -> {
                sender.sendMessage(ChatColor.RED + "======================================");
                sender.sendMessage(ChatColor.RED + "RPGItems plugin require Spigot API, Please make sure you are using Spigot.");
                sender.sendMessage(ChatColor.RED + "======================================");
                return true;
            });
        }
        WGSupport.init(this);
        if (cfg.localeInv) {
            Events.useLocaleInv = true;
        }
        i18n = new I18n(this, cfg.language);
        commandHandler = new Handler(this, i18n);
        getCommand("rpgitem").setExecutor(commandHandler);
        getCommand("rpgitem").setTabCompleter(commandHandler);
        Bukkit.getScheduler().runTask(plugin, () -> {
            getServer().getPluginManager().registerEvents(listener = new Events(), this);
            ItemManager.load(this);
            new PowerTicker().runTaskTimer(this, 0, 1);
        });

        managedPlugins.forEach(Bukkit.getPluginManager()::enablePlugin);
    }

    @Override
    public void onDisable() {
        WGSupport.unload();
        getCommand("rpgitem").setExecutor(null);
        getCommand("rpgitem").setTabCompleter(null);
        this.getServer().getScheduler().cancelTasks(plugin);

        managedPlugins.forEach(Bukkit.getPluginManager()::disablePlugin);
    }
}
