package think.rpgitems;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import think.rpgitems.commands.CommandDocumentation;
import think.rpgitems.commands.CommandGroup;
import think.rpgitems.commands.CommandHandler;
import think.rpgitems.commands.CommandString;
import think.rpgitems.data.Locale;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.Quality;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;
import think.rpgitems.support.WorldGuard;

import java.lang.reflect.Field;
import java.util.*;

public class Handler implements CommandHandler {

    
    @CommandString("rpgitem reload")
    @CommandDocumentation("$command.rpgitem.reload")
    @CommandGroup("reload")
    public void reload(CommandSender sender) {
        Plugin.plugin.reloadConfig();
        Plugin.plugin.updateConfig();
        Locale.reload();
        WorldGuard.reload();
        ItemManager.reload();
        if (Plugin.plugin.getConfig().getBoolean("autoupdate", true)) {
            Plugin.plugin.startUpdater();
        }
        if (Plugin.plugin.getConfig().getBoolean("localeInv", false)) {
            Events.useLocaleInv = true;
        }
        sender.sendMessage(ChatColor.GREEN + "[RPGItems] Reloaded RPGItems.");
    }
    
    @CommandString("rpgitem list")
    @CommandDocumentation("$command.rpgitem.list")
    @CommandGroup("list")
    public void listItems(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "RPGItems:");
        for (RPGItem item : ItemManager.itemByName.values()) {
            if (sender instanceof Player) {
                Player player = ((Player) sender).getPlayer();
                BaseComponent msg = new TextComponent();
                msg.addExtra(ChatColor.GREEN + item.getName() + " - " + ChatColor.RESET);
                msg.addExtra(item.getComponent());
                player.spigot().sendMessage(msg);
            } else {
                sender.sendMessage(ChatColor.GREEN + item.getName() + " - " + item.getDisplay());
            }
        }
    }

    @CommandString("rpgitem list $page:i[]")
    @CommandDocumentation("$command.rpgitem.list.paged")
    @CommandGroup("list")
    public void listItems(CommandSender sender, int page) {
        int index = 0;
        Collection<RPGItem> items = ItemManager.itemByName.values();
        int perPage = Plugin.plugin.getConfig().getInt("itemperpage", 9);
        sender.sendMessage(ChatColor.AQUA + "RPGItems: " + page + " / " + (int)Math.ceil(items.size()/(double)perPage) );
        for (RPGItem item : ItemManager.itemByName.values()) {
            ++index;
            if(index > (page - 1)*perPage && index <= page*perPage){
                if (sender instanceof Player) {
                    Player player = ((Player) sender).getPlayer();
                    BaseComponent msg = new TextComponent();
                    msg.addExtra(ChatColor.GREEN + item.getName() + " - " + ChatColor.RESET);
                    msg.addExtra(item.getComponent());
                    player.spigot().sendMessage(msg);
                } else {
                    sender.sendMessage(ChatColor.GREEN + item.getName() + " - " + item.getDisplay());
                }
            }
            if(index == page*perPage )return;
        }
    }

    @CommandString("rpgitem option worldguard")
    @CommandDocumentation("$command.rpgitem.worldguard")
    @CommandGroup("option_worldguard")
    public void toggleWorldGuard(CommandSender sender) {
        if (!WorldGuard.isEnabled()) {
            sender.sendMessage(ChatColor.RED + Locale.get("message.worldguard.error"));
            return;
        }
        if (WorldGuard.useWorldGuard) {
            sender.sendMessage(ChatColor.AQUA + Locale.get("message.worldguard.disable"));
        } else {
            sender.sendMessage(ChatColor.AQUA + Locale.get("message.worldguard.enable"));
        }
        WorldGuard.useWorldGuard = !WorldGuard.useWorldGuard;
        Plugin.plugin.getConfig().set("support.worldguard", WorldGuard.useWorldGuard);
        Plugin.plugin.saveConfig();
    }

    @CommandString("rpgitem option wgcustomflag")
    @CommandDocumentation("$command.rpgitem.wgcustomflag")
    @CommandGroup("option_wgcustomflag")
    public void toggleCustomFlag(CommandSender sender) {
        if (!WorldGuard.isEnabled()) {
            sender.sendMessage(ChatColor.RED + Locale.get("message.worldguard.error"));
            return;
        }
        if (WorldGuard.useCustomFlag) {
            sender.sendMessage(ChatColor.AQUA + Locale.get("message.wgcustomflag.disable"));
        } else {
            sender.sendMessage(ChatColor.AQUA + Locale.get("message.wgcustomflag.enable"));
        }
        WorldGuard.useCustomFlag = !WorldGuard.useCustomFlag;
        Plugin.plugin.getConfig().set("support.wgcustomflag", WorldGuard.useCustomFlag);
        Plugin.plugin.saveConfig();
    }

    @CommandString("rpgitem option wgforcerefreash")
    @CommandDocumentation("$command.rpgitem.wgforcerefreash")
    @CommandGroup("option_wgforcerefreash")
    public void toggleForceRefreash(CommandSender sender) {
        if (!WorldGuard.isEnabled()) {
            sender.sendMessage(ChatColor.RED + Locale.get("message.worldguard.error"));
            return;
        }
        if (WorldGuard.forceRefresh) {
            sender.sendMessage(ChatColor.AQUA + Locale.get("message.wgforcerefresh.disable"));
        } else {
            sender.sendMessage(ChatColor.AQUA + Locale.get("message.wgforcerefresh.enable"));
        }
        WorldGuard.forceRefresh = !WorldGuard.forceRefresh;
        Plugin.plugin.getConfig().set("support.wgforcerefresh", WorldGuard.forceRefresh);
        Plugin.plugin.saveConfig();
    }

    @CommandString("rpgitem $n[]")
    @CommandDocumentation("$command.rpgitem.print")
    @CommandGroup("item")
    public void printItem(CommandSender sender, RPGItem item) {
        item.print(sender);
    }

    @CommandString("rpgitem $name:s[] create")
    @CommandDocumentation("$command.rpgitem.create")
    @CommandGroup("item")
    public void createItem(CommandSender sender, String itemName) {
        if (ItemManager.newItem(itemName.toLowerCase()) != null) {
            sender.sendMessage(String.format(ChatColor.GREEN + Locale.get("message.create.ok"), itemName));
            ItemManager.save(Plugin.plugin);
        } else {
            sender.sendMessage(ChatColor.RED + Locale.get("message.create.fail"));
        }
    }

    @CommandString("rpgitem option giveperms")
    @CommandDocumentation("$command.rpgitem.giveperms")
    @CommandGroup("option_giveperms")
    public void givePerms(CommandSender sender) {
        Plugin.plugin.getConfig().set("give-perms", !Plugin.plugin.getConfig().getBoolean("give-perms", false));
        if (Plugin.plugin.getConfig().getBoolean("give-perms", false)) {
            sender.sendMessage(ChatColor.AQUA + Locale.get("message.giveperms.true"));
        } else {
            sender.sendMessage(ChatColor.AQUA + Locale.get("message.giveperms.false"));
        }
        Plugin.plugin.saveConfig();
    }

    @CommandString(value = "rpgitem $n[] give", handlePermissions = true)
    @CommandDocumentation("$command.rpgitem.give")
    @CommandGroup("item_give")
    public void giveItem(CommandSender sender, RPGItem item) {
        if (sender instanceof Player) {
            if ((!Plugin.plugin.getConfig().getBoolean("give-perms", false) && sender.hasPermission("rpgitem")) || (Plugin.plugin.getConfig().getBoolean("give-perms", false) && sender.hasPermission("rpgitem.give." + item.getName()))) {
                item.give((Player) sender);
                sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.give.ok"), item.getDisplay()));
            } else {
                sender.sendMessage(ChatColor.RED + Locale.get("message.error.permission"));
            }
        } else {
            sender.sendMessage(ChatColor.RED + Locale.get("message.give.console"));
        }
    }

    @CommandString("rpgitem $n[] give $p[]")
    @CommandDocumentation("$command.rpgitem.give.player")
    @CommandGroup("item_give")
    public void giveItemPlayer(CommandSender sender, RPGItem item, Player player) {
        item.give(player);
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.give.to"), item.getDisplay() + ChatColor.AQUA, player.getName()));
        player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.give.ok"), item.getDisplay()));
    }

    @CommandString("rpgitem $n[] give $p[] $count:i[]")
    @CommandDocumentation("$command.rpgitem.give.player.count")
    @CommandGroup("item_give")
    public void giveItemPlayerCount(CommandSender sender, RPGItem item, Player player, int count) {
        for (int i = 0; i < count; i++) {
            item.give(player);
        }
    }

    @CommandString("rpgitem $n[] remove")
    @CommandDocumentation("$command.rpgitem.remove")
    @CommandGroup("item_remove")
    public void removeItem(CommandSender sender, RPGItem item) {
        ItemManager.remove(item);
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.remove.ok"), item.getName()));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] display")
    @CommandDocumentation("$command.rpgitem.display")
    @CommandGroup("item_display")
    public void getItemDisplay(CommandSender sender, RPGItem item) {
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.display.get"), item.getName(), item.getDisplay()));
    }

    @CommandString("rpgitem $n[] display $display:s[]")
    @CommandDocumentation("$command.rpgitem.display.set")
    @CommandGroup("item_display")
    public void setItemDisplay(CommandSender sender, RPGItem item, String display) {
        item.setDisplay(display);
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.display.set"), item.getName(), item.getDisplay()));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] quality")
    @CommandDocumentation("$command.rpgitem.quality")
    @CommandGroup("item_quality")
    public void getItemQuality(CommandSender sender, RPGItem item) {
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.quality.get"), item.getName(), item.getQuality().toString().toLowerCase()));
    }

    @CommandString("rpgitem $n[] quality $e[think.rpgitems.item.Quality]")
    @CommandDocumentation("$command.rpgitem.quality.set")
    @CommandGroup("item_quality")
    public void setItemQuality(CommandSender sender, RPGItem item, Quality quality) {
        item.setQuality(quality);
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.quality.set"), item.getName(), item.getQuality().toString().toLowerCase()));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] damage")
    @CommandDocumentation("$command.rpgitem.damage")
    @CommandGroup("item_damage")
    public void getItemDamage(CommandSender sender, RPGItem item) {
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.damage.get"), item.getName(), item.getDamageMin(), item.getDamageMax()));
    }

    @CommandString("rpgitem $n[] damage $damage:i[]")
    @CommandDocumentation("$command.rpgitem.damage.set")
    @CommandGroup("item_damage")
    public void setItemDamage(CommandSender sender, RPGItem item, int damage) {
        if (damage > 32767) {
            sender.sendMessage(ChatColor.RED + Locale.get("message.error.damagetolarge"));
            return;
        }
        item.setDamage(damage, damage);
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.damage.set"), item.getName(), item.getDamageMin()));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] damage $min:i[] $max:i[]")
    @CommandDocumentation("$command.rpgitem.damage.set.range")
    @CommandGroup("item_damage")
    public void setItemDamage(CommandSender sender, RPGItem item, int min, int max) {
        if (min > 32767 || max > 32767) {
            sender.sendMessage(ChatColor.RED + Locale.get("message.error.damagetolarge"));
            return;
        }
        item.setDamage(min, max);
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.damage.set.range"), item.getName(), item.getDamageMin(), item.getDamageMax()));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] armour")
    @CommandDocumentation("$command.rpgitem.armour")
    @CommandGroup("item_armour")
    public void getItemArmour(CommandSender sender, RPGItem item) {
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.armour.get"), item.getName(), item.getArmour()));
    }

    @CommandString("rpgitem $n[] armour $armour:i[0,100]")
    @CommandDocumentation("$command.rpgitem.armour.set")
    @CommandGroup("item_armour")
    public void setItemArmour(CommandSender sender, RPGItem item, int armour) {
        item.setArmour(armour);
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.armour.set"), item.getName(), item.getArmour()));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] type")
    @CommandDocumentation("$command.rpgitem.type")
    @CommandGroup("item_type")
    public void getItemType(CommandSender sender, RPGItem item) {
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.type.get"), item.getName(), item.getType()));
    }

    @CommandString("rpgitem $n[] type $type:s[]")
    @CommandDocumentation("$command.rpgitem.type.set")
    @CommandGroup("item_type")
    public void setItemType(CommandSender sender, RPGItem item, String type) {
        item.setType(type);
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.type.set"), item.getName(), item.getType()));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] hand")
    @CommandDocumentation("$command.rpgitem.hand")
    @CommandGroup("item_hand")
    public void getItemHand(CommandSender sender, RPGItem item) {
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.hand.get"), item.getName(), item.getHand()));
    }

    @CommandString("rpgitem $n[] hand $hand:s[]")
    @CommandDocumentation("$command.rpgitem.hand.set")
    @CommandGroup("item_hand")
    public void setItemHand(CommandSender sender, RPGItem item, String hand) {
        item.setHand(hand);
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.hand.set"), item.getName(), item.getHand()));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] lore")
    @CommandDocumentation("$command.rpgitem.lore")
    @CommandGroup("item_lore")
    public void getItemLore(CommandSender sender, RPGItem item) {
        sender.sendMessage(ChatColor.RED + Locale.get("message.deprecation.lore"));
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.lore.get"), item.getName(), item.getLore()));
    }

    @CommandString("rpgitem $n[] lore $lore:s[]")
    @CommandDocumentation("$command.rpgitem.lore.set")
    @CommandGroup("item_lore")
    public void setItemLore(CommandSender sender, RPGItem item, String lore) {
        sender.sendMessage(ChatColor.RED + Locale.get("message.deprecation.lore"));
        item.setLore(lore);
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.lore.set"), item.getName(), item.getLore()));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] item")
    @CommandDocumentation("$command.rpgitem.item")
    @CommandGroup("item_item")
    public void getItemItem(CommandSender sender, RPGItem item) {
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.item.get"), item.getName(), item.getItem().toString()));
    }

    @CommandString("rpgitem $n[] item $m[]")
    @CommandDocumentation("$command.rpgitem.item.set")
    @CommandGroup("item_item")
    public void setItemItem(CommandSender sender, RPGItem item, Material material) {
        item.setItem(material);
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.item.set"), item.getName(), item.getItem(), item.getDataValue()));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] item $m[] $data:i[]")
    @CommandDocumentation("$command.rpgitem.item.set.data")
    @CommandGroup("item_item")
    public void setItemItem(CommandSender sender, RPGItem item, Material material, int data) {
        item.setItem(material, false);
        ItemMeta meta = item.getLocaleMeta();
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(Color.fromRGB(data));
        } else {
            item.setDataValue((short) data);
        }
        item.updateLocaleMeta(meta);
        item.rebuild();
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.item.set"), item.getName(), item.getItem(), item.getDataValue()));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] item $m[] hex $hexcolour:s[]")
    @CommandDocumentation("$command.rpgitem.item.set.data.hex")
    @CommandGroup("item_item")
    public void setItemItem(CommandSender sender, RPGItem item, Material material, String hexColour) {
        int dam;
        try {
            dam = Integer.parseInt((String) hexColour, 16);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Failed to parse " + hexColour);
            return;
        }
        item.setItem(material, true);
        ItemMeta meta = item.getLocaleMeta();
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(Color.fromRGB(dam));
        } else {
            item.setDataValue((short) dam);
        }
        item.updateLocaleMeta(meta);
        item.rebuild();
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.item.set"), item.getName(), item.getItem(), item.getDataValue()));
        ItemManager.save(Plugin.plugin);
    }

    @SuppressWarnings("deprecation")
    @CommandString("rpgitem $n[] item $itemid:i[]")
    @CommandDocumentation("$command.rpgitem.item.set.id")
    @CommandGroup("item_item")
    public void setItemItem(CommandSender sender, RPGItem item, int id) {
        Material mat = Material.getMaterial(id);
        if (mat == null) {
            sender.sendMessage(ChatColor.RED + "Cannot find item");
            return;
        }
        item.setItem(mat);
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.item.set"), item.getName(), item.getItem(), item.getDataValue()));
        ItemManager.save(Plugin.plugin);
    }

    @SuppressWarnings("deprecation")
    @CommandString("rpgitem $n[] item $itemid:i[] $data:i[]")
    @CommandDocumentation("$command.rpgitem.item.set.id.data")
    @CommandGroup("item_item")
    public void setItemItem(CommandSender sender, RPGItem item, int id, int data) {
        Material mat = Material.getMaterial(id);
        if (mat == null) {
            sender.sendMessage(ChatColor.RED + Locale.get("message.item.cant.find"));
            return;
        }
        item.setItem(mat, true);
        ItemMeta meta = item.toItemStack().getItemMeta();
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(Color.fromRGB(data));
        } else {
            item.setDataValue((short) data);
        }
        item.updateLocaleMeta(meta);
        item.rebuild();
        sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.item.set"), item.getName(), item.getItem(), item.getDataValue()));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] enchantment")
    @CommandDocumentation("$command.rpgitem.enchentment.list")
    @CommandGroup("item_enchantment")
    public void itemListEnchant(CommandSender sender, RPGItem item) {
        if (item.enchantMap != null) {
            sender.sendMessage(ChatColor.GREEN + String.format(Locale.get("message.enchantment.listing"), item.getName()));
            if (item.enchantMap.size() == 0) {
                sender.sendMessage(ChatColor.GREEN + Locale.get("message.enchantment.empty_ench"));
            } else {
                for (Enchantment ench : item.enchantMap.keySet()) {
                    sender.sendMessage(ChatColor.GREEN + String.format(Locale.get("message.enchantment.item"),
                            ench.getName(), item.enchantMap.get(ench)));
                }
            }
        } else {
            sender.sendMessage(ChatColor.RED + Locale.get("message.enchantment.no_ench"));
        }
    }

    @CommandString("rpgitem $n[] enchantment clone")
    @CommandDocumentation("$command.rpgitem.enchentment.clone")
    @CommandGroup("item_enchantment")
    public void itemCloneEnchant(CommandSender sender, RPGItem item) {
        if (sender instanceof Player) {
            ItemStack hand = ((Player) sender).getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                sender.sendMessage(ChatColor.RED + Locale.get("message.enchantment.fail"));
            } else {
                if (hand.hasItemMeta()) {
                    item.enchantMap = new HashMap<>(hand.getItemMeta().getEnchants());
                } else {
                    item.enchantMap = Collections.emptyMap();
                }
                item.rebuild();
                ItemManager.save(Plugin.plugin);
                sender.sendMessage(ChatColor.GREEN + Locale.get("message.enchantment.success"));
            }
        } else {
            sender.sendMessage(ChatColor.RED + Locale.get("message.enchantment.fail"));
        }
    }

    @CommandString("rpgitem $n[] enchantment clear")
    @CommandDocumentation("$command.rpgitem.enchentment.clear")
    @CommandGroup("item_enchantment")
    public void itemClearEnchant(CommandSender sender, RPGItem item) {
        item.enchantMap = null;
        item.rebuild();
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.GREEN + Locale.get("message.enchantment.removed"));
    }

    @CommandString("rpgitem $n[] removepower $power:s[]")
    @CommandDocumentation("$command.rpgitem.removepower")
    @CommandGroup("item_removepower")
    public void itemRemovePower(CommandSender sender, RPGItem item, String power) {
        if (item.removePower(power)) {
            Power.powerUsage.put(power, Power.powerUsage.get(power) - 1);
            sender.sendMessage(ChatColor.GREEN + String.format(Locale.get("message.power.removed"), power));
            ItemManager.save(Plugin.plugin);
        } else {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.power.unknown"), power));
        }
    }

    @CommandString("rpgitem $n[] description add $descriptionline:s[]")
    @CommandDocumentation("$command.rpgitem.description.add")
    @CommandGroup("item_description")
    public void itemAddDescription(CommandSender sender, RPGItem item, String line) {
        item.addDescription(ChatColor.WHITE + line);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.description.ok"));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] description set $lineno:i[] $descriptionline:s[]")
    @CommandDocumentation("$command.rpgitem.description.set")
    @CommandGroup("item_description")
    public void itemSetDescription(CommandSender sender, RPGItem item, int lineNo, String line) {
        if (lineNo < 0 || lineNo >= item.description.size()) {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.description.out.of.range"), lineNo));
            return;
        }
        item.description.set(lineNo, ChatColor.translateAlternateColorCodes('&', ChatColor.WHITE + line));
        item.rebuild();
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.description.change"));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] description remove $lineno:i[]")
    @CommandDocumentation("$command.rpgitem.description.remove")
    @CommandGroup("item_description")
    public void itemRemoveDescription(CommandSender sender, RPGItem item, int lineNo) {
        if (lineNo < 0 || lineNo >= item.description.size()) {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.description.out.of.range"), lineNo));
            return;
        }
        item.description.remove(lineNo);
        item.rebuild();
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.description.remove"));
        ItemManager.save(Plugin.plugin);
    }

    @CommandString("rpgitem $n[] worldguard")
    @CommandDocumentation("$command.rpgitem.item.worldguard")
    @CommandGroup("item_worldguard")
    public void itemToggleWorldGuard(CommandSender sender, RPGItem item) {
        if (!WorldGuard.isEnabled()) {
            sender.sendMessage(ChatColor.AQUA + Locale.get("message.worldguard.error"));
            return;
        }
        item.ignoreWorldGuard = !item.ignoreWorldGuard;
        if (item.ignoreWorldGuard) {
            sender.sendMessage(ChatColor.AQUA + Locale.get("message.worldguard.override.active"));
        } else {
            sender.sendMessage(ChatColor.AQUA + Locale.get("message.worldguard.override.disabled"));
        }
    }

    @CommandString("rpgitem $n[] removerecipe")
    @CommandDocumentation("$command.rpgitem.removerecipe")
    @CommandGroup("item_recipe")
    public void itemRemoveRecipe(CommandSender sender, RPGItem item) {
        item.hasRecipe = false;
        item.resetRecipe(true);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.recipe.removed"));
    }

    @CommandString("rpgitem $n[] recipe $chance:i[]")
    @CommandDocumentation("$command.rpgitem.recipe")
    @CommandGroup("item_recipe")
    public void itemSetRecipe(CommandSender sender, RPGItem item, int chance) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String title = "RPGItems - " + item.getDisplay();
            if (title.length() > 32) {
                title = title.substring(0, 32);
            }
            Inventory recipeInventory = Bukkit.createInventory(player, 27, title);
            if (item.hasRecipe) {
                ItemStack blank = new ItemStack(Material.THIN_GLASS);
                ItemMeta meta = blank.getItemMeta();
                meta.setDisplayName(ChatColor.RED + Locale.get("message.recipe.1"));
                ArrayList<String> lore = new ArrayList<String>();
                lore.add(ChatColor.WHITE + Locale.get("message.recipe.2"));
                lore.add(ChatColor.WHITE + Locale.get("message.recipe.3"));
                lore.add(ChatColor.WHITE + Locale.get("message.recipe.4"));
                lore.add(ChatColor.WHITE + Locale.get("message.recipe.5"));
                meta.setLore(lore);
                blank.setItemMeta(meta);
                for (int i = 0; i < 27; i++) {
                    recipeInventory.setItem(i, blank);
                }
                for (int x = 0; x < 3; x++) {
                    for (int y = 0; y < 3; y++) {
                        int i = x + y * 9;
                        ItemStack it = item.recipe.get(x + y * 3);
                        if (it != null)
                            recipeInventory.setItem(i, it);
                        else
                            recipeInventory.setItem(i, null);
                    }
                }
            }
            item.setRecipeChance(chance);
            player.openInventory(recipeInventory);
            Events.recipeWindows.put(player.getName(), item.getID());
        } else {
            sender.sendMessage(ChatColor.RED + Locale.get("message.error.only.player"));
        }
    }

    @CommandString("rpgitem $n[] drop $e[org.bukkit.entity.EntityType]")
    @CommandDocumentation("$command.rpgitem.drop")
    @CommandGroup("item_drop")
    public void getItemDropChance(CommandSender sender, RPGItem item, EntityType type) {
        sender.sendMessage(String.format(ChatColor.AQUA + Locale.get("message.drop.get"), item.getDisplay() + ChatColor.AQUA, type.toString().toLowerCase(), item.dropChances.get(type.toString())));
    }

    @CommandString("rpgitem $n[] drop $e[org.bukkit.entity.EntityType] $chance:f[]")
    @CommandDocumentation("$command.rpgitem.drop.set")
    @CommandGroup("item_drop")
    public void setItemDropChance(CommandSender sender, RPGItem item, EntityType type, double chance) {
        chance = Math.min(chance, 100.0);
        String typeS = type.toString();
        if (chance > 0) {
            item.dropChances.put(typeS, chance);
            if (!Events.drops.containsKey(typeS)) {
                Events.drops.put(typeS, new HashSet<Integer>());
            }
            Set<Integer> set = Events.drops.get(typeS);
            set.add(item.getID());
        } else {
            item.dropChances.remove(typeS);
            if (Events.drops.containsKey(typeS)) {
                Set<Integer> set = Events.drops.get(typeS);
                set.remove(item.getID());
            }
        }
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(String.format(ChatColor.AQUA + Locale.get("message.drop.set"), item.getDisplay() + ChatColor.AQUA, typeS.toLowerCase(), item.dropChances.get(typeS)));
    }

    @CommandString("rpgitem $n[] get $power:s[] $nth:i[] $property:s[]")
    @CommandDocumentation("$command.rpgitem.power_property_set")
    @CommandGroup("item_power_property_g")
    public void getItemPowerProperty(CommandSender sender, RPGItem item, String power, int nth, String property) {
        int i = nth;
        Class p = Power.powers.get(power);
        if(p == null){
            sender.sendMessage(String.format(Locale.get("message.power.unknown"), power));
            return;
        }
        for (Power pow:item.powers) {
            if(p.isInstance(pow) && --i == 0){
                try{
                    Field pro = p.getField(property);
                    String val = pro.get(pow).toString();
                    sender.sendMessage(String.format(Locale.get("message.power_property.get"), nth, power, property, val));
                }catch (Exception e){
                    sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.power_property.property_notfound"), property));
                    return;
                }
                return;
            }
        }
        sender.sendMessage(ChatColor.RED + Locale.get("message.power_property.power_notfound"));
    }

    @CommandString("rpgitem $n[] set $power:s[] $nth:i[] $property:s[] $val:s[]")
    @CommandDocumentation("$command.rpgitem.power_property_get")
    @CommandGroup("item_power_property_s")
    public void setItemPowerProperty(CommandSender sender, RPGItem item, String power, int nth, String property, String val) {
        Class p = Power.powers.get(power);
        if(p == null){
            sender.sendMessage(String.format(Locale.get("message.power.unknown"), power));
            return;
        }
        for (Power pow:item.powers) {
            if(p.isInstance(pow) && --nth == 0){
                try {
                    Field pro = p.getField(property);
                    if(pro.getType() == int.class || pro.getType() == long.class){
                        try {
                            pro.set(pow, Integer.parseInt(val));
                        }catch (NumberFormatException e){
                            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.power_property.not_vaild_int"), val));
                            return;
                        }
                    }else if(pro.getType() == double.class) {
                        try {
                            pro.set(pow, Double.parseDouble(val));
                        }catch (NumberFormatException e){
                            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.power_property.not_vaild_int"), val));
                            return;
                        }
                    }else if(pro.getType() == String.class){
                        pro.set(pow, val);
                    }else if(pro.getType() == boolean.class){
                        if(val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")){
                            pro.set(pow, val.equalsIgnoreCase("true"));
                        } else {
                            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.power_property.not_vaild_bool"), val));
                            return;
                        }
                    }else {
                        sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.power_property.property_unsupporttype"), property));
                    }
                }catch (Exception e){
                    sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.power_property.property_notfound"), property));
                    return;
                }
                ItemManager.save(Plugin.plugin);
                sender.sendMessage(Locale.get("message.power_property.change"));
                return;
            }
        }
        sender.sendMessage(ChatColor.RED + Locale.get("message.power_property.power_notfound"));
    }

    @CommandString("rpgitem $n[] cost breaking")
    @CommandDocumentation("$command.rpgitem.cost.break.get")
    @CommandGroup("item_cost_break")
    public void getItemBlockBreakingCost(CommandSender sender, RPGItem item) {
        sender.sendMessage(String.format(Locale.get("message.cost.get"),item.blockBreakingCost));
    }

    @CommandString("rpgitem $n[] cost hitting")
    @CommandDocumentation("$command.rpgitem.cost.hitting.get")
    @CommandGroup("item_cost_hitting")
    public void getItemHittingConst(CommandSender sender, RPGItem item) {
        sender.sendMessage(String.format(Locale.get("message.cost.get"),item.hittingCost));
    }

    @CommandString("rpgitem $n[] cost hit")
    @CommandDocumentation("$command.rpgitem.cost.hit.get")
    @CommandGroup("item_cost_hit")
    public void getItemHitConst(CommandSender sender, RPGItem item) {
        sender.sendMessage(String.format(Locale.get("message.cost.get"),item.hitCost));
    }

    @CommandString("rpgitem $n[] cost breaking $durability:i[]")
    @CommandDocumentation("$command.rpgitem.cost.breaking")
    @CommandGroup("item_cost_hitting")
    public void setItemBreakingConst(CommandSender sender, RPGItem item, int newValue) {
        item.blockBreakingCost = newValue;
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(Locale.get("message.cost.change"));
    }

    @CommandString("rpgitem $n[] cost hitting $durability:i[]")
    @CommandDocumentation("$command.rpgitem.cost.hitting")
    @CommandGroup("item_cost_hitting")
    public void setItemHittingConst(CommandSender sender, RPGItem item, int newValue) {
        item.hittingCost = newValue;
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(Locale.get("message.cost.change"));
    }

    @CommandString("rpgitem $n[] cost hit $durability:i[]")
    @CommandDocumentation("$command.rpgitem.cost.hit")
    @CommandGroup("item_cost_hit")
    public void setItemHitConst(CommandSender sender, RPGItem item, int newValue) {
        item.hitCost = newValue;
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(Locale.get("message.cost.change"));
    }

    @CommandString("rpgitem $n[] cost hit toggle")
    @CommandDocumentation("$command.rpgitem.cost.hit_toggle")
    @CommandGroup("item_cost_hit_toggle")
    public void toggleHitCost(CommandSender sender, RPGItem item) {
        item.hitCostByDamage = !item.hitCostByDamage;
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(Locale.get("message.cost.hit_toggle." + (item.hitCostByDamage ? "enable" : "disable")));
    }

    @CommandString("rpgitem $n[] durability $durability:i[]")
    @CommandDocumentation("$command.rpgitem.durability")
    @CommandGroup("item_durability")
    public void setItemDurability(CommandSender sender, RPGItem item, int newValue) {
        item.setMaxDurability(newValue);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(Locale.get("message.durability.change"));
    }

    @CommandString("rpgitem $n[] durability infinite")
    @CommandDocumentation("$command.rpgitem.durability.infinite")
    @CommandGroup("item_durability")
    public void setItemDurabilityInfinite(CommandSender sender, RPGItem item) {
        item.setMaxDurability(-1);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(Locale.get("message.durability.change"));
    }

    @CommandString("rpgitem $n[] durability togglebar")
    @CommandDocumentation("$command.rpgitem.durability.togglebar")
    @CommandGroup("item_durability")
    public void toggleItemDurabilityBar(CommandSender sender, RPGItem item) {
        item.toggleBar();
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(Locale.get("message.durability.toggle"));
    }

    @CommandString("rpgitem $n[] defaultdurability $durability:i[]")
    @CommandDocumentation("$command.rpgitem.durability.default")
    @CommandGroup("item_defaultdurability")
    public void setItemDefaultDurability(CommandSender sender, RPGItem item, int newValue) {
        item.setDefaultDurability(newValue);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(Locale.get("message.durability.change"));
    }

    @CommandString("rpgitem $n[] durabilitybound $min:i[] $max:i[]")
    @CommandDocumentation("$command.rpgitem.durability.bound")
    @CommandGroup("item_durabilitybound")
    public void setItemDurabilityBound(CommandSender sender, RPGItem item, int min, int max) {
        item.setDurabilityBound(min, max);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(Locale.get("message.durability.change"));
    }

    @CommandString("rpgitem $n[] durabilityinfo")
    @CommandDocumentation("$command.rpgitem.durability.info")
    @CommandGroup("item_durability")
    public void getItemDurability(CommandSender sender, RPGItem item) {
        sender.sendMessage(String.format(Locale.get("message.durability.info"), item.getMaxDurability(), item.defaultDurability, item.durabilityLowerBound, item.durabilityUpperBound));
    }

    @CommandString("rpgitem $n[] permission $permission:s[] $haspermission:s[]")
    @CommandDocumentation("$command.rpgitem.permission")
    @CommandGroup("item_permission")
    public void setPermission(CommandSender sender, RPGItem item, String permission, String haspermission) {
        boolean enabled = false;
        if (haspermission.equalsIgnoreCase("true")) {
            enabled = true;
        } else if (haspermission.equalsIgnoreCase("false")) {
            enabled = false;
        } else {
            sender.sendMessage(Locale.get("message.permission.booleanerror"));
        }
        item.setPermission(permission);
        item.setHaspermission(enabled);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(Locale.get("message.permission.success"));
    }

    @CommandString("rpgitem $n[] togglePowerLore")
    @CommandDocumentation("$command.rpgitem.togglePowerLore")
    @CommandGroup("item_togglePowerLore")
    public void togglePowerLore(CommandSender sender, RPGItem item) {
        item.showPowerLore = !item.showPowerLore;
        item.rebuild();
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(Locale.get("message.toggleLore."+(item.showPowerLore?"show":"hide")));
    }

    @CommandString("rpgitem $n[] toggleArmorLore")
    @CommandDocumentation("$command.rpgitem.toggleArmorLore")
    @CommandGroup("item_toggleArmorLore")
    public void toggleArmorLore(CommandSender sender, RPGItem item) {
        item.showArmourLore = !item.showArmourLore;
        item.rebuild();
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(Locale.get("message.toggleLore."+(item.showArmourLore?"show":"hide")));
    }

    @CommandString("rpgitem $n[] additemflag $e[think.rpgitems.item.ItemFlag]")
    @CommandDocumentation("$command.rpgitem.itemflag.add+ItemFlag")
    @CommandGroup("item_itemflag")
    public void addItemFlag(CommandSender sender, RPGItem item, think.rpgitems.item.ItemFlag flag) {
        item.itemFlags.add(ItemFlag.valueOf(flag.name()));
        item.rebuild();
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(String.format(ChatColor.GREEN + Locale.get("message.itemflag.add"), flag.name()));
    }

    @CommandString("rpgitem $n[] removeitemflag $e[think.rpgitems.item.ItemFlag]")
    @CommandDocumentation("$command.rpgitem.itemflag.remove+ItemFlag")
    @CommandGroup("item_itemflag")
    public void removeItemFlag(CommandSender sender, RPGItem item, think.rpgitems.item.ItemFlag flag) {
        ItemFlag itemFlag = ItemFlag.valueOf(flag.name());
        if (item.itemFlags.contains(itemFlag)) {
            item.itemFlags.remove(itemFlag);
            item.rebuild();
            ItemManager.save(Plugin.plugin);
            sender.sendMessage(String.format(ChatColor.AQUA + Locale.get("message.itemflag.remove"), flag.name()));
        } else {
            sender.sendMessage(String.format(ChatColor.RED + Locale.get("message.itemflag.notfound"), flag.name()));
        }
    }

    @CommandString("rpgitem $n[] customItemModel")
    @CommandDocumentation("$command.rpgitem.customitemmodel")
    @CommandGroup("item_customitemmodel")
    public void toggleCustomItemModel(CommandSender sender, RPGItem item) {
        item.customItemModel = !item.customItemModel;
        item.rebuild();
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(Locale.get("message.customitemmodel." + (item.customItemModel ? "enable" : "disable")));
    }
    
    @CommandString("rpgitem version")
    @CommandDocumentation("$command.rpgitem.version")
    @CommandGroup("version")
    public void printVersion(CommandSender sender) {
        sender.sendMessage(String.format(Locale.get("message.version").replace("\\n","\n"), Plugin.plugin.getDescription().getVersion()));
    }

    @CommandString("rpgitem debug")
    @CommandDocumentation("$command.rpgitem.debug")
    @CommandGroup("debug")
    public void debug(CommandSender sender) {
        sender.sendMessage("Not available in releases");
    }
}
