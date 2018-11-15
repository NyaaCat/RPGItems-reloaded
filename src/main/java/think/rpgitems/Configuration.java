package think.rpgitems;

import cat.nyaa.nyaacore.configuration.PluginConfigure;
import org.bukkit.plugin.java.JavaPlugin;

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

    @Serializable(name ="general.locale_inv", alias = "localeInv")
    public boolean localeInv = false;

    @Serializable(name ="general.pid_compat", alias = "pidCompat")
    public boolean pidCompat = false;

    @Serializable(name ="command.list.item_per_page", alias = "itemperpage")
    public int itemPerPage = 9;

    @Serializable(name ="command.list.power_per_page")
    public int powerPerPage = 5;

    @Serializable(name = "support.worldguard")
    public boolean useWorldGuard = true;

    @Serializable(name = "support.wgforcerefresh")
    public boolean wgForceRefresh = false;

    @Serializable(name ="general.give_perms", alias = "give-perms")
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

    @Serializable(name = "item.defaults.note")
    public String defaultNote;

    @Serializable(name = "item.defaults.author")
    public String defaultAuthor;

    @Serializable(name = "general.item.fs_lock")
    public boolean itemFsLock = true;
}
