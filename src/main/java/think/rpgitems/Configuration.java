package think.rpgitems;

import cat.nyaa.nyaacore.configuration.PluginConfigure;
import org.bukkit.plugin.java.JavaPlugin;
import think.rpgitems.item.RPGItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Configuration extends PluginConfigure {
    private final RPGItems plugin;
    @Serializable
    public String language = "en_US";
    @Serializable
    public String version = "1.1";
    @Serializable(name = "general.enabled_languages")
    public List<String> enabledLanguages = Stream.of("en_US", "zh_CN").collect(Collectors.toList());
    @Serializable(name = "general.spu_endpoint")
    public String spuEndpoint = null;
    @Serializable(name = "command.list.item_per_page", alias = "itemperpage")
    public int itemPerPage = 9;
    @Serializable(name = "command.list.power_per_page")
    public int powerPerPage = 5;
    @Serializable(name = "support.MythicMobs.enable", alias = "support.mythicmobs")
    public boolean useMythicMobs = true;
    @Serializable(name = "support.Residence.enable", alias = "support.residence")
    public boolean useResidence = true;
    @Serializable(name = "support.PlaceholderAPI.enable", alias = "support.placeholderapi")
    public boolean usePlaceholderAPI = true;
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
    @Serializable(name = "item.defaults.allow_anvil_enchant")
    public boolean allowAnvilEnchant = true;
    @Serializable(name = "item.defaults.note")
    public String defaultNote;
    @Serializable(name = "item.defaults.author")
    public String defaultAuthor;
    @Serializable(name = "general.item.fs_lock")
    public boolean itemFsLock = true;
    @Serializable(name = "general.item.show_loaded")
    public boolean itemShowLoaded = false;
    // enable for better performance
    // note: all new given items will not stack
    // and can not be used in trades!
    @Serializable(name = "general.item.item_stack_uuid")
    public boolean itemStackUuid = true;
    @SuppressWarnings("unused")
    @Serializable(name = "unused.locale_inv", alias = {"general.locale_inv", "localeInv"})
    public boolean oldLocaleInv = false;
    @Serializable(name = "item.quality")
    public Map<String, String> qualityPrefixes = new HashMap<>();

    {
        qualityPrefixes.put("trash", "§7");
        qualityPrefixes.put("normal", "§f");
        qualityPrefixes.put("rare", "§b");
        qualityPrefixes.put("epic", "§3");
        qualityPrefixes.put("legendary", "§e");
    }
    @Serializable(name = "general.item.show_cooldown_warning_to_actionbar")
    public boolean showCooldownActionbar = false;

    public Configuration(RPGItems plugin) {
        this.plugin = plugin;
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }
}
