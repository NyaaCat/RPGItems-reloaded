package think.rpgitems;

import cat.nyaa.nyaacore.configuration.PluginConfigure;
import org.bukkit.plugin.java.JavaPlugin;
import think.rpgitems.item.RPGItem;

public class Configuration extends PluginConfigure {
    private final RPGItems plugin;

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    public Configuration(RPGItems plugin) {
        this.plugin = plugin;
    }

    @Serializable
    public String language = "en_US";

    @Serializable
    public String version = "1.0";

    @Serializable(name = "general.spu_endpoint")
    public String spuEndpoint = null;

    @Serializable(name = "general.pid_compat", alias = "pidCompat")
    public boolean pidCompat = false;

    @Serializable(name = "command.list.item_per_page", alias = "itemperpage")
    public int itemPerPage = 9;

    @Serializable(name = "command.list.power_per_page")
    public int powerPerPage = 5;

    @Serializable(name = "support.world_guard.enable", alias = "support.worldguard")
    public boolean useWorldGuard = true;

    @Serializable(name = "support.world_guard.force_refresh", alias = "support.wgforcerefresh")
    public boolean wgForceRefresh = false;

    @Serializable(name = "support.world_guard.disable_in_no_pvp")
    public boolean wgNoPvP = true;

    @Serializable(name = "support.world_guard.show_warning")
    public boolean wgShowWarning = true;

    @Serializable(name = "general.give_perms", alias = "give-perms")
    public boolean givePerms = false;

    @Serializable(name = "gist.token", alias = "githubToken")
    public String githubToken = "";

    @Serializable(name = "gist.publish", alias = "publishGist")
    public boolean publishGist = true;

    @Serializable(name = "item.defaults.numeric_bar", alias = "numericBar")
    public boolean numericBar = false;

    @Serializable(name = "item.defaults.force_bar", alias = "forceBar")
    public boolean forceBar = false;

    @Serializable(name = "item.defaults.license")
    public String defaultLicense = "All Right Reserved";

    @Serializable(name = "item.defaults.enchant_mode")
    public RPGItem.EnchantMode defaultEnchantMode = RPGItem.EnchantMode.DISALLOW;

    @Serializable(name = "item.defaults.note")
    public String defaultNote;

    @Serializable(name = "item.defaults.author")
    public String defaultAuthor;

    @Serializable(name = "general.item.fs_lock")
    public boolean itemFsLock = true;

    @Serializable(name = "general.item.show_loaded")
    public boolean itemShowLoaded = false;

    @SuppressWarnings("unused")
    @Serializable(name = "unused.locale_inv", alias = {"general.locale_inv", "localeInv"})
    public boolean oldLocaleInv = false;
}
