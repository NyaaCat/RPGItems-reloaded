package think.rpgitems;

import cat.nyaa.nyaacore.LanguageRepository;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import cat.nyaa.nyaacore.utils.OfflinePlayerUtils;
import com.google.common.base.Strings;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.inventory.meta.tags.ItemTagType;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.item.ItemGroup;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;
import think.rpgitems.power.impl.PowerCommand;
import think.rpgitems.power.impl.PowerThrow;
import think.rpgitems.support.WGSupport;
import think.rpgitems.utils.MaterialUtils;
import think.rpgitems.utils.NetworkUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static think.rpgitems.item.RPGItem.*;
import static think.rpgitems.power.Utils.rethrow;
import static think.rpgitems.utils.ItemTagUtils.*;
import static think.rpgitems.utils.NetworkUtils.Location.GIST;

public class UserHandler extends RPGCommandReceiver {
    private final RPGItems plugin;

    UserHandler(RPGItems plugin, LanguageRepository i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }

    @SubCommand(value = "tomodel", permission = "tomodel")
    @Attribute("command")
    public void toModel(CommandSender sender, Arguments args) {
        Player p = asPlayer(sender);
        RPGItem item = getItem(sender);
        ItemStack itemStack = p.getInventory().getItemInMainHand();
        item.updateItem(itemStack);
        ItemMeta itemMeta = itemStack.getItemMeta();
        CustomItemTagContainer meta = getTag(itemMeta.getCustomTagContainer(), TAG_META);
        meta.removeCustomTag(TAG_OWNER);
        meta.removeCustomTag(TAG_STACK_ID);
        set(meta, TAG_IS_MODEL, true);
        itemMeta.setDisplayName(item.getDisplayName());
        itemStack.setItemMeta(itemMeta);
    }

    private RPGItem getItem(CommandSender sender) {
        Player p = (Player) sender;
        Optional<RPGItem> item = ItemManager.toRPGItem(p.getInventory().getItemInMainHand());
        if (item.isPresent()) {
            return item.get();
        } else {
            throw new BadCommandException("message.error.iteminhand");
        }
    }
}
