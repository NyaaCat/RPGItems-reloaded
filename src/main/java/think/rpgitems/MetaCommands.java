package think.rpgitems;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.command.CommandSender;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.RPGCommandReceiver;

import java.util.ArrayList;
import java.util.List;

import static think.rpgitems.AdminCommands.filtered;
import static think.rpgitems.AdminCommands.getItem;

public class MetaCommands extends RPGCommandReceiver {
    private final RPGItems plugin;

    public MetaCommands(RPGItems plugin, I18n i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return "meta";
    }

    //temporary implementation, will replace with generic function
    //all prop in RPGItem will be able to modify here.
    @SubCommand(value = "quality", tabCompleter = "qualityCompleter")
    public void onQuality(CommandSender sender, Arguments arguments) {
        RPGItem item = getItem(arguments.nextString(), sender);
        String quality = arguments.nextString();
        item.setQuality(quality);
        if (!plugin.cfg.qualityPrefixes.containsKey(quality)) {
            new Message("").append(I18n.formatDefault("command.meta.quality.warn_quality_not_exists", quality));
        }
        ItemManager.save(item);
    }

    private List<String> qualityCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(ItemManager.itemNames());
                break;
            case 2:
                completeStr.addAll(plugin.cfg.qualityPrefixes.keySet());
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "type", tabCompleter = "typeCompleter")
    public void onType(CommandSender sender, Arguments arguments) {
        RPGItem item = getItem(arguments.nextString(), sender);
        String type = arguments.nextString();
        item.setType(type);
        ItemManager.save(item);
    }

    private List<String> typeCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(ItemManager.itemNames());
                break;
        }
        return filtered(arguments, completeStr);
    }
}
