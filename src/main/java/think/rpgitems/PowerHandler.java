package think.rpgitems;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import think.rpgitems.commands.CommandDocumentation;
import think.rpgitems.commands.CommandGroup;
import think.rpgitems.commands.CommandHandler;
import think.rpgitems.commands.CommandString;
import think.rpgitems.data.Locale;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.PowerArrow;
import think.rpgitems.power.PowerCommand;
import think.rpgitems.power.PowerConsume;
import think.rpgitems.power.PowerFireball;
import think.rpgitems.power.PowerFlame;
import think.rpgitems.power.PowerFood;
import think.rpgitems.power.PowerIce;
import think.rpgitems.power.PowerKnockup;
import think.rpgitems.power.PowerLifeSteal;
import think.rpgitems.power.PowerLightning;
import think.rpgitems.power.PowerPotionHit;
import think.rpgitems.power.PowerPotionSelf;
import think.rpgitems.power.PowerPotionTick;
import think.rpgitems.power.PowerRainbow;
import think.rpgitems.power.PowerRumble;
import think.rpgitems.power.PowerSkyHook;
import think.rpgitems.power.PowerTNTCannon;
import think.rpgitems.power.PowerTeleport;
import think.rpgitems.power.PowerUnbreakable;
import think.rpgitems.power.PowerUnbreaking;

public class PowerHandler implements CommandHandler {

    @CommandString("rpgitem $n[] power arrow")
    @CommandDocumentation("$command.rpgitem.arrow")
    @CommandGroup("item_power_arrow")
    public void arrow(CommandSender sender, RPGItem item) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerArrow pow = new PowerArrow();
        pow.cooldownTime = 20;
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power arrow $cooldown:i[]")
    @CommandDocumentation("$command.rpgitem.arrow.full")
    @CommandGroup("item_power_arrow")
    public void arrow(CommandSender sender, RPGItem item, int cooldown) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerArrow pow = new PowerArrow();
        pow.item = item;
        pow.cooldownTime = cooldown;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power command $cooldown:i[] $o[left,right] $display:s[] $command:s[]")
    @CommandDocumentation("$command.rpgitem.command")
    @CommandGroup("item_power_command_b")
    public void command(CommandSender sender, RPGItem item, int cooldown, String mouse, String displayText, String command) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerCommand com = new PowerCommand();
        com.cooldownTime = cooldown;
        command = command.trim();
        if (command.charAt(0) == '/') {
            command = command.substring(1);
        }
        com.isRight = mouse.equals("right");
        com.display = displayText;
        com.command = command;
        com.item = item;
        item.addPower(com);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power command $cooldown:i[] $o[left,right] $display:s[] $command:s[] $permission:s[]")
    @CommandDocumentation("$command.rpgitem.command.full")
    @CommandGroup("item_power_command_a")
    public void command(CommandSender sender, RPGItem item, int cooldown, String mouse, String displayText, String command, String permission) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerCommand com = new PowerCommand();
        com.cooldownTime = cooldown;
        command = command.trim();
        if (command.charAt(0) == '/') {
            command = command.substring(1);
        }
        com.isRight = mouse.equals("right");
        com.display = displayText;
        com.command = command;
        com.permission = permission;
        com.item = item;
        item.addPower(com);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power command $cooldown:i[] $o[left,right] $details:s[]")
    @CommandDocumentation("$command.rpgitem.command.old")
    @CommandGroup("item_power_command_c")
    public void command(CommandSender sender, RPGItem item, int cooldown, String mouse, String details) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        String[] pArgs = details.split("\\|");
        if (pArgs.length < 2) {
            sender.sendMessage(ChatColor.RED + Locale.get("message.error.command.format", locale));
            return;
        }
        String display = pArgs[0].trim();
        String command = pArgs[1].trim();
        if (command.charAt(0) == '/') {
            command = command.substring(1);
        }
        String permission = "";
        if (pArgs.length > 2) {
            permission = pArgs[2].trim();
        }

        PowerCommand com = new PowerCommand();
        com.cooldownTime = cooldown;

        com.isRight = mouse.equals("right");
        com.item = item;
        com.display = display;
        com.command = command;
        com.permission = permission;

        item.addPower(com);
        item.rebuild();
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power consume")
    @CommandDocumentation("$command.rpgitem.consume")
    @CommandGroup("item_power_consume")
    public void consume(CommandSender sender, RPGItem item) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerConsume pow = new PowerConsume();
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power fireball")
    @CommandDocumentation("$command.rpgitem.fireball")
    @CommandGroup("item_power_fireball")
    public void fireball(CommandSender sender, RPGItem item) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerFireball pow = new PowerFireball();
        pow.cooldownTime = 20;
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power fireball $cooldown:i[]")
    @CommandDocumentation("$command.rpgitem.fireball.full")
    @CommandGroup("item_power_fireball")
    public void fireball(CommandSender sender, RPGItem item, int cooldown) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerFireball pow = new PowerFireball();
        pow.item = item;
        pow.cooldownTime = cooldown;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power flame")
    @CommandDocumentation("$command.rpgitem.flame")
    @CommandGroup("item_power_flame")
    public void flame(CommandSender sender, RPGItem item) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerFlame pow = new PowerFlame();
        pow.burnTime = 20;
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power flame $burntime:i[]")
    @CommandDocumentation("$command.rpgitem.flame.full")
    @CommandGroup("item_power_flame")
    public void flame(CommandSender sender, RPGItem item, int burnTime) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerFlame pow = new PowerFlame();
        pow.item = item;
        pow.burnTime = burnTime;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }
    
    @CommandString("rpgitem $n[] power lifesteal $chance:i[]")
    @CommandDocumentation("$command.rpgitem.lifesteal")
    @CommandGroup("item_power_lifesteal")
    public void lifsteal(CommandSender sender, RPGItem item, int chance) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerLifeSteal pow = new PowerLifeSteal();
        pow.item = item;
        pow.chance = chance;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power ice")
    @CommandDocumentation("$command.rpgitem.ice")
    @CommandGroup("item_power_ice")
    public void ice(CommandSender sender, RPGItem item) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerIce pow = new PowerIce();
        pow.cooldownTime = 20;
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power ice $cooldown:i[]")
    @CommandDocumentation("$command.rpgitem.ice.full")
    @CommandGroup("item_power_ice")
    public void ice(CommandSender sender, RPGItem item, int cooldown) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerIce pow = new PowerIce();
        pow.item = item;
        pow.cooldownTime = cooldown;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power knockup")
    @CommandDocumentation("$command.rpgitem.knockup")
    @CommandGroup("item_power_knockup")
    public void knockup(CommandSender sender, RPGItem item) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerKnockup pow = new PowerKnockup();
        pow.item = item;
        pow.chance = 20;
        pow.power = 2;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power knockup $chance:i[] $power:f[]")
    @CommandDocumentation("$command.rpgitem.knockup.full")
    @CommandGroup("item_power_knockup")
    public void knockup(CommandSender sender, RPGItem item, int chance, double power) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerKnockup pow = new PowerKnockup();
        pow.item = item;
        pow.chance = chance;
        pow.power = power;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power lightning")
    @CommandDocumentation("$command.rpgitem.lightning")
    @CommandGroup("item_power_lightning")
    public void lightning(CommandSender sender, RPGItem item) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerLightning pow = new PowerLightning();
        pow.item = item;
        pow.chance = 20;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power lightning $chance:i[]")
    @CommandDocumentation("$command.rpgitem.lightning.full")
    @CommandGroup("item_power_lightning")
    public void lightning(CommandSender sender, RPGItem item, int chance) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerLightning pow = new PowerLightning();
        pow.item = item;
        pow.chance = chance;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power potionhit $chance:i[] $duration:i[] $amplifier:i[] $effect:s[]")
    @CommandDocumentation("$command.rpgitem.potionhit+PotionEffectType")
    @CommandGroup("item_power_potionhit")
    public void potionhit(CommandSender sender, RPGItem item, int chance, int duration, int amplifier, String effect) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerPotionHit pow = new PowerPotionHit();
        pow.item = item;
        pow.chance = chance;
        pow.duration = duration;
        pow.amplifier = amplifier;
        pow.type = PotionEffectType.getByName(effect);
        if (pow.type == null) {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.effect", locale), effect));
            return;
        }
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power potionself $cooldown:i[] $duration:i[] $amplifier:i[] $effect:s[]")
    @CommandDocumentation("$command.rpgitem.potionself+PotionEffectType")
    @CommandGroup("item_power_potionself")
    public void potionself(CommandSender sender, RPGItem item, int ccoldown, int duration, int amplifier, String effect) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerPotionSelf pow = new PowerPotionSelf();
        pow.item = item;
        pow.cooldownTime = ccoldown;
        pow.time = duration;
        pow.amplifier = amplifier;
        pow.type = PotionEffectType.getByName(effect);
        if (pow.type == null) {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.effect", locale), effect));
            return;
        }
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power rainbow")
    @CommandDocumentation("$command.rpgitem.rainbow")
    @CommandGroup("item_power_rainbow")
    public void rainbow(CommandSender sender, RPGItem item) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerRainbow pow = new PowerRainbow();
        pow.cooldownTime = 20;
        pow.count = 5;
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power rainbow $cooldown:i[] $count:i[]")
    @CommandDocumentation("$command.rpgitem.rainbow.full")
    @CommandGroup("item_power_rainbow")
    public void rainbow(CommandSender sender, RPGItem item, int cooldown, int count) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerRainbow pow = new PowerRainbow();
        pow.cooldownTime = cooldown;
        pow.count = count;
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power rumble $cooldown:i[] $power:i[] $distance:i[]")
    @CommandDocumentation("$command.rpgitem.rumble")
    @CommandGroup("item_power_rumble")
    public void rumble(CommandSender sender, RPGItem item, int cooldown, int power, int distance) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerRumble pow = new PowerRumble();
        pow.item = item;
        pow.cooldownTime = cooldown;
        pow.power = power;
        pow.distance = distance;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power teleport")
    @CommandDocumentation("$command.rpgitem.teleport")
    @CommandGroup("item_power_teleport")
    public void teleport(CommandSender sender, RPGItem item) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerTeleport pow = new PowerTeleport();
        pow.item = item;
        pow.cooldownTime = 20;
        pow.distance = 5;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power teleport $cooldown:i[] $distance:i[]")
    @CommandDocumentation("$command.rpgitem.teleport.full")
    @CommandGroup("item_power_teleport")
    public void teleport(CommandSender sender, RPGItem item, int cooldown, int distance) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerTeleport pow = new PowerTeleport();
        pow.item = item;
        pow.cooldownTime = cooldown;
        pow.distance = distance;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power tntcannon")
    @CommandDocumentation("$command.rpgitem.tntcannon")
    @CommandGroup("item_power_tntcannon")
    public void tntcannon(CommandSender sender, RPGItem item) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerTNTCannon pow = new PowerTNTCannon();
        pow.item = item;
        pow.cooldownTime = 20;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }

    @CommandString("rpgitem $n[] power tntcannon $cooldown:i[]")
    @CommandDocumentation("$command.rpgitem.tntcannon.full")
    @CommandGroup("item_power_tntcannon")
    public void tntcannon(CommandSender sender, RPGItem item, int cooldown) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerTNTCannon pow = new PowerTNTCannon();
        pow.item = item;
        pow.cooldownTime = cooldown;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }
    
    @CommandString("rpgitem $n[] power skyhook $m[] $distance:i[]")
    @CommandDocumentation("$command.rpgitem.skyhook")
    @CommandGroup("item_power_skyhook")
    public void skyHook(CommandSender sender, RPGItem item, Material material, int distance) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerSkyHook pow = new PowerSkyHook();
        pow.item = item;
        pow.railMaterial = material;
        pow.hookDistance = distance;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }
    
    @CommandString("rpgitem $n[] power potiontick $amplifier:i[] $effect:s[]")
    @CommandDocumentation("$command.rpgitem.potiontick")
    @CommandGroup("item_power_potiontick")
    public void potionTick(CommandSender sender, RPGItem item, int amplifier, String effect) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerPotionTick pow = new PowerPotionTick();
        pow.item = item;
        pow.amplifier = amplifier;
        pow.effect = PotionEffectType.getByName(effect);
        if (pow.effect == null) {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.effect", locale), effect));
            return;
        }
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }
    @CommandString("rpgitem $n[] power food $foodpoints:i[]")
    @CommandDocumentation("$command.rpgitem.food")
    @CommandGroup("item_power_food")
    public void food(CommandSender sender, RPGItem item, int foodpoints) {
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        PowerFood pow = new PowerFood();
        pow.item = item;
        pow.foodpoints = foodpoints;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok", locale));
    }
}
