package think.rpgitems;

import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.Trigger;

import java.util.logging.Level;
import java.util.stream.Collectors;

import static think.rpgitems.AdminCommands.*;

public class PowerCommands extends RPGCommandReceiver {
    private final RPGItems plugin;

    public PowerCommands(RPGItems plugin, I18n i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }

    private Pair<NamespacedKey, Class<? extends Power>> getPowerClass(CommandSender sender, String powerStr) {
        try {
            NamespacedKey key = PowerManager.parseKey(powerStr);
            Class<? extends Power> cls = PowerManager.getPower(key);
            if (cls == null) {
                msgs(sender, "message.power.unknown", powerStr);
            }
            return Pair.of(key, cls);
        } catch (UnknownExtensionException e) {
            msgs(sender, "message.error.unknown.extension", e.getName());
            return null;
        }
    }

    @SubCommand("add")
    public void add(CommandSender sender, Arguments args) {
        String itemStr = args.next();
        String powerStr = args.next();
        if (itemStr == null || itemStr.equals("help")) {
            msgs(sender, "manual.power.add.description");
            msgs(sender, "manual.power.add.usage");
            return;
        }
        if (powerStr == null || powerStr.equals("list")) {
            RPGItem item = getItem(itemStr, sender);
            for (Power power : item.getPowers()) {
                msgs(sender, "message.item.power", power.getLocalizedName(plugin.cfg.language), power.getNamespacedKey().toString(), power.displayText() == null ? I18n.getInstance(sender).format("message.power.no_display") : power.displayText(), power.getTriggers().stream().map(Trigger::name).collect(Collectors.joining(",")));
                if ("list".equals(powerStr)) {
                    PowerManager.getProperties(power.getNamespacedKey()).forEach(
                            (name, prop) -> showProp(sender, power.getNamespacedKey(), prop.getValue(), power)
                    );
                }
            }
            return;
        }
        RPGItem item = getItem(itemStr, sender);
        Pair<NamespacedKey, Class<? extends Power>> keyClass = getPowerClass(sender, powerStr);
        if (keyClass == null || keyClass.getValue() == null) return;
        Power power;
        Class<? extends Power> cls = keyClass.getValue();
        NamespacedKey key = keyClass.getKey();
        try {
            power = initPropertyHolder(sender, args, item, cls);
            item.addPower(key, power);
            ItemManager.refreshItem();
            ItemManager.save(item);
            msg(sender, "message.power.ok");
        } catch (Exception e) {
            if (e instanceof AdminCommands.CommandException) {
                throw (AdminCommands.CommandException) e;
            }
            plugin.getLogger().log(Level.WARNING, "Error adding power " + powerStr + " to item " + itemStr + " " + item, e);
            msgs(sender, "internal.error.command_exception");
        }
    }

    @SubCommand("set")
    @Completion("property")
    public void set(CommandSender sender, Arguments args) throws IllegalAccessException {
        RPGItem item = getItem(args.nextString(), sender);
        int nth = args.nextInt();
        try {
            Power power = item.getPowers().get(nth);
            if (power == null) {
                msgs(sender, "message.power.unknown", nth);
                return;
            }
            if (args.top() == null) {
                PowerManager.getProperties(power.getNamespacedKey()).forEach(
                        (name, prop) -> showProp(sender, power.getNamespacedKey(), prop.getValue(), power)
                );
                return;
            }
            setPropertyHolder(sender, args, power.getClass(), power, false);
            item.rebuild();
            ItemManager.refreshItem();
            ItemManager.save(item);
            msgs(sender, "message.power.change");
        } catch (UnknownExtensionException e) {
            msgs(sender, "message.error.unknown.extension", e.getName());
        }
    }

    @SubCommand("remove")
    @Completion("property")
    public void remove(CommandSender sender, Arguments args) {
        RPGItem item = getItem(args.nextString(), sender);
        int nth = args.nextInt();
        try {
            Power power = item.getPowers().get(nth);
            if (power == null) {
                msgs(sender, "message.power.unknown", nth);
                return;
            }
            power.deinit();
            item.getPowers().remove(nth);
            msgs(sender, "message.power.removed");
        } catch (UnknownExtensionException e) {
            msgs(sender, "message.error.unknown.extension", e.getName());
        }
    }
}
