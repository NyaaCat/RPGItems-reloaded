package think.rpgitems.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import think.rpgitems.data.Locale;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

import java.util.ArrayList;
import java.util.List;

public class RPGItemUpdateCommandHandler implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length <= 0) {
            updateItemInHand(sender);
        } else if (args[0].equalsIgnoreCase("inspect")) {
            inspectItemInHand(sender);
        } else if (args[0].equalsIgnoreCase("upgrade")) {
            upgradeItemInHand(sender);
        } else if (args[0].equalsIgnoreCase("downgrade")) {
            downgradeItemInHand(sender);
        } else {
            return false;
        }
        return true;
    }

    public void updateItemInHand(CommandSender sender) {
        if (!sender.hasPermission("rpgitemupdate.command")) {
            sender.sendMessage("No permission");
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be player to do this");
            return;
        }
        Player p = (Player) sender;
        p.sendMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + Locale.get("message.update.head"));
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            p.sendMessage(ChatColor.RED + Locale.get("message.update.noitem"));
            p.sendMessage(ChatColor.RED + ChatColor.BOLD.toString() + Locale.get("message.update.fail"));
            return;
        }
        if (ItemManager.toRPGItem(item) != null) {
            p.sendMessage(ChatColor.GREEN + Locale.get("message.update.noneed"));
            p.sendMessage(ChatColor.GREEN + ChatColor.BOLD.toString() + Locale.get("message.update.success"));
            return;
        }

        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            p.sendMessage(ChatColor.RED + "Not a valid RPGItem");
            p.sendMessage(ChatColor.RED + ChatColor.BOLD.toString() + Locale.get("message.update.fail"));
            return;
        }
        String name = item.getItemMeta().getDisplayName();
        int id; RPGItem ritem = null;
        try {
            id = ItemManager.decodeId(name);
            ritem = ItemManager.getItemById(id);
        } catch (Exception ex) {
            p.sendMessage(ChatColor.RED + Locale.get("message.update.notvalid"));
            p.sendMessage(ChatColor.RED + ChatColor.BOLD.toString() + Locale.get("message.update.fail"));
            return;
        }
        if (ritem == null) {
            p.sendMessage(ChatColor.RED + Locale.get("message.update.notvalid"));
            p.sendMessage(ChatColor.RED + ChatColor.BOLD.toString() + Locale.get("message.update.fail"));
            return;
        }
        p.sendMessage(ChatColor.GREEN + Locale.get("message.update.updating"));
        ItemMeta meta = item.getItemMeta();
        List<String> lore;
        if (meta.hasLore()) {
            lore = meta.getLore();
        }else{
            lore = new ArrayList<>();
        }
        if (lore.size() <= 0) lore.add("");
        lore.set(0, ritem.getMCEncodedID() + lore.get(0));
        meta.setLore(lore);
        item.setItemMeta(meta);
        RPGItem.updateItem(item);
        p.sendMessage(ChatColor.GREEN + ChatColor.BOLD.toString() + Locale.get("message.update.success"));
    }

    public void inspectItemInHand(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage("No permission");
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be player to do this");
            return;
        }
        Player p = (Player) sender;
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            p.sendMessage("You must take item in hand");
        } else if (!item.hasItemMeta()) {
            p.sendMessage("Item has no Metadata");
        } else {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                try {
                    p.sendMessage("DisplayName ID: " + ItemManager.decodeId(meta.getDisplayName()));
                } catch (Exception ex) {
                    p.sendMessage("DisplayName contains no valid ID.");
                }
            } else {
                p.sendMessage("Item has no displayName");
            }
            if (meta.hasLore() && meta.getLore().size() > 0) {
                try {
                    p.sendMessage("Lore ID: " + ItemManager.decodeId(meta.getLore().get(0)));
                } catch (Exception ex) {
                    p.sendMessage("DisplayName contains no valid ID.");
                }
            } else {
                p.sendMessage("Item has no lore ID");
            }
        }
        p.sendMessage(">>> Inspect Finish <<<");
    }

    public void upgradeItemInHand(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage("No permission");
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be player to do this");
            return;
        }
        Player p = (Player) sender;
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            p.sendMessage("You must take item in hand");
        } else if (!item.hasItemMeta()) {
            p.sendMessage("Item has no Metadata");
        } else {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                try {
                    int id = ItemManager.decodeId(meta.getDisplayName());
                    p.sendMessage("DisplayName ID: " + id);
                    RPGItem ritem = ItemManager.getItemById(id);
                    List<String> lore;
                    if (meta.hasLore()) {
                        lore = meta.getLore();
                    }else{
                        lore = new ArrayList<>();
                    }
                    if (lore.size() <= 0) lore.add("");
                    lore.set(0, ritem.getMCEncodedID() + lore.get(0));
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    RPGItem.updateItem(item);
                    p.sendMessage("Updated.");
                } catch (Exception ex) {
                    p.sendMessage("DisplayName contains no valid ID.");
                }
            } else {
                p.sendMessage("Item has no displayName");
            }
        }
        p.sendMessage(">>> Update Finish <<<");
    }

    public void downgradeItemInHand(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage("No permission");
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be player to do this");
            return;
        }
        Player p = (Player) sender;
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            p.sendMessage("You must take item in hand");
        } else if (!item.hasItemMeta()) {
            p.sendMessage("Item has no Metadata");
        } else {
            ItemMeta meta = item.getItemMeta();
            if (!meta.hasLore() || meta.getLore().size() <= 0) {
                p.sendMessage("No Lore");
            } else {
                String lore = meta.getLore().get(0);
                RPGItem ritem = ItemManager.toRPGItem(item);
                if (ritem == null) {
                    p.sendMessage("Invalid Lore");
                } else {
                    List<String> l = meta.getLore();
                    l.set(0,l.get(0).substring(RPGItem.MC_ENCODED_ID_LENGTH));
                    meta.setLore(l);
                    meta.setDisplayName(ritem.getTooltipLines().get(0));
                    item.setItemMeta(meta);
                    p.sendMessage("Downgraded.");
                }
            }
        }
        p.sendMessage(">>> Downgrade Finish <<<");
    }

    public static boolean isOldRPGItem(ItemStack item) {
        if (item == null) return false;
        if (!item.hasItemMeta()) return false;
        String name = item.getItemMeta().getDisplayName();
        try {
            ItemManager.decodeId(name);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
