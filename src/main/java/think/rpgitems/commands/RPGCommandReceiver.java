package think.rpgitems.commands;

import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.LanguageRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import think.rpgitems.RPGItems;
import think.rpgitems.item.ItemManager;

public abstract class RPGCommandReceiver extends CommandReceiver<RPGItems> {
    public RPGCommandReceiver(RPGItems plugin, LanguageRepository i18n) {
        super(plugin, i18n);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (ItemManager.getItemByName(args[0]) != null) {
            String item = args[1];
            args[1] = args[0];
            args[0] = item;
        }
        Arguments cmd = Arguments.parse(args);
        if (cmd == null) return false;
        acceptCommand(sender, cmd);
        return true;
    }
}
