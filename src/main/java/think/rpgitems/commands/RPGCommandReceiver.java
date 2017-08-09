package think.rpgitems.commands;

import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.LanguageRepository;
import javafx.util.Pair;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import think.rpgitems.RPGItems;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class RPGCommandReceiver extends CommandReceiver {
    public RPGCommandReceiver(RPGItems plugin, LanguageRepository i18n) {
        super(plugin, i18n);
    }

    private static Pair<RPGItem, String> resolveItemCommand(String f, String s) {
        RPGItem rpgItem = ItemManager.getItemByName(f);
        if (rpgItem != null) {
            return new Pair<>(rpgItem, s);
        }
        rpgItem = ItemManager.getItemByName(s);
        if (rpgItem != null) {
            return new Pair<>(rpgItem, f);
        }
        return null;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean suggestion = args[args.length - 1].isEmpty();
        CommandReceiver.Arguments cmd = CommandReceiver.Arguments.parse(args);
        if (cmd == null) return null;
        switch (cmd.length()) {
            case 0:
                return subCommandAttribute.entrySet().stream().filter(entry -> entry.getValue().startsWith("command")).map(Map.Entry::getKey).collect(Collectors.toList());
            case 1: {
                String str = cmd.next();
                if (suggestion) {
                    if (ItemManager.getItemByName(str) != null) {
                        return subCommandAttribute.entrySet().stream().filter(entry -> Stream.of("item", "power", "property").anyMatch(entry.getValue()::startsWith)).map(Map.Entry::getKey).collect(Collectors.toList());
                    } else {
                        String attr = subCommandAttribute.get(str);
                        if (attr == null) return null;
                        if (attr.startsWith("command")) {
                            String[] att = attr.split(":");
                            if (att.length > 1) {
                                return Arrays.asList(att[1].split(","));
                            }
                            return null;
                        } else {
                            return new ArrayList<>(ItemManager.itemByName.keySet());
                        }
                    }
                } else {
                    List<String> list = subCommands.keySet().stream().filter(s -> s.startsWith(str)).collect(Collectors.toList());
                    if (!list.isEmpty()) return list;
                    return ItemManager.itemByName.keySet().stream().filter(s -> s.startsWith(str)).collect(Collectors.toList());
                }
            }
            case 2: {
                String first = cmd.next();
                String second = cmd.next();
                if (suggestion) {
                    Pair<RPGItem, String> itemCommand = resolveItemCommand(first, second);
                    if (itemCommand == null) return null;
                    String attr = subCommandAttribute.get(itemCommand.getValue());
                    if (attr == null) return null;
                    String[] att = attr.split(":");
                    switch (att[0]) {
                        case "property":
                        case "power":
                            switch (itemCommand.getValue()) {
                                case "power":
                                    return new ArrayList<>(Power.powers.keySet());
                                case "removepower":
                                    return itemCommand.getKey().powers.stream().map(Power::getName).collect(Collectors.toList());
                                default:
                                    return null;
                            }
                        case "item":
                        case "command": {
                            return att.length > 1 ? Arrays.asList(att[1].split(",")) : null;
                        }
                    }
                } else {
                    if (ItemManager.getItemByName(first) != null) {
                        return subCommandAttribute.entrySet().stream()
                                                  .filter(entry -> Stream.of("item", "power", "property").anyMatch(entry.getValue()::startsWith))
                                                  .map(Map.Entry::getKey)
                                                  .filter(s -> s.startsWith(second))
                                                  .collect(Collectors.toList());
                    } else {
                        String attr = subCommandAttribute.get(first);
                        if (attr == null) return null;
                        String[] att = attr.split(":");
                        switch (att[0]) {
                            case "property":
                            case "power":
                            case "item": {
                                return ItemManager.itemByName.keySet().stream().filter(s -> s.startsWith(second)).collect(Collectors.toList());
                            }
                            case "command": {
                                return att.length > 1 ? Arrays.stream(att[1].split(",")).filter(s -> s.startsWith(second)).collect(Collectors.toList()) : null;
                            }
                        }
                    }
                }
            }
            case 3: {
                String first = cmd.next();
                String second = cmd.next();
                String third = cmd.next();
                Pair<RPGItem, String> itemCommand = resolveItemCommand(first, second);
                if (itemCommand == null) return null;
                String attr = subCommandAttribute.get(itemCommand.getValue());
                if (attr == null) return null;
                String[] att = attr.split(":");
                if (suggestion) {
                    return null; //TODO
                } else {
                    switch (att[0]) {
                        case "property":
                        case "power":
                            switch (itemCommand.getValue()) {
                                case "power":
                                    return Power.powers.keySet().stream().filter(s -> s.startsWith(third)).collect(Collectors.toList());
                                case "set":
                                case "get":
                                case "removepower":
                                    return itemCommand.getKey().powers.stream().map(Power::getName).filter(s -> s.startsWith(third)).collect(Collectors.toList());
                                default:
                                    return null;
                            }
                        case "item": {
                            return att.length > 1 ? Arrays.stream(att[1].split(",")).filter(s -> s.startsWith(third)).collect(Collectors.toList()) : null;
                        }
                        default:
                            return null;
                    }
                }
            }
        }
        return null;
    }
}
