package think.rpgitems;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.commands.CommandDocumentation;
import think.rpgitems.commands.CommandGroup;
import think.rpgitems.commands.CommandHandler;
import think.rpgitems.commands.CommandString;
import think.rpgitems.data.Locale;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

public class PowerHandler implements CommandHandler {

    @CommandString("rpgitem $n[] power aoe $cooldown:i[] $range:i[] $effect:s[] $duration:i[] $amplifier:i[]")
    @CommandDocumentation("$command.rpgitem.aoe+PotionEffectType")
    @CommandGroup("item_power_aoe")
    public void potionaoe(CommandSender sender, RPGItem item, int cooldown, int range, String effect, int duration, int amplifier) {
        PowerAOE pow = new PowerAOE();
        pow.item = item;
        pow.cooldownTime = cooldown;
        pow.range = range;
        pow.duration = duration;
        pow.amplifier = amplifier;
        pow.selfapplication = true;
        pow.type = PotionEffectType.getByName(effect);
        if (pow.type == null) {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.effect"), effect));
            return;
        }
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power aoe $cooldown:i[] $range:i[] $effect:s[] $duration:i[] $amplifier:i[] $selfapplication:s[]")
    @CommandDocumentation("$command.rpgitem.aoe+PotionEffectType")
    @CommandGroup("item_power_aoe")
    public void potionaoe(CommandSender sender, RPGItem item, int cooldown, int range, String effect, int duration, int amplifier, String selfapplication) {
        PowerAOE pow = new PowerAOE();
        pow.item = item;
        pow.cooldownTime = cooldown;
        pow.range = range;
        pow.duration = duration;
        pow.amplifier = amplifier;
        pow.selfapplication = Boolean.parseBoolean(selfapplication);
        pow.type = PotionEffectType.getByName(effect);
        if (pow.type == null) {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.effect"), effect));
            return;
        }
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power aoe $displayName:s[] $cooldown:i[] $range:i[] $effect:s[] $duration:i[] $amplifier:i[] $selfapplication:s[]")
    @CommandDocumentation("$command.rpgitem.aoe+PotionEffectType")
    @CommandGroup("item_power_aoe")
    public void potionaoe(CommandSender sender, RPGItem item, String displayName, int cooldown, int range, String effect, int duration, int amplifier, String selfapplication) {
        PowerAOE pow = new PowerAOE();
        pow.item = item;
        pow.cooldownTime = cooldown;
        pow.range = range;
        pow.duration = duration;
        pow.amplifier = amplifier;
        pow.selfapplication = Boolean.parseBoolean(selfapplication);
        pow.type = PotionEffectType.getByName(effect);
        pow.name = displayName.replace("&", "§");
        if (pow.type == null) {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.effect"), effect));
            return;
        }
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power arrow")
    @CommandDocumentation("$command.rpgitem.arrow")
    @CommandGroup("item_power_arrow")
    public void arrow(CommandSender sender, RPGItem item) {
        PowerArrow pow = new PowerArrow();
        pow.cooldownTime = 20;
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power arrow $cooldown:i[]")
    @CommandDocumentation("$command.rpgitem.arrow.full")
    @CommandGroup("item_power_arrow")
    public void arrow(CommandSender sender, RPGItem item, int cooldown) {
        PowerArrow pow = new PowerArrow();
        pow.item = item;
        pow.cooldownTime = cooldown;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power color")
    @CommandDocumentation("$command.rpgitem.color")
    @CommandGroup("item_power_color")
    public void color(CommandSender sender, RPGItem item) {
        PowerColor color = new PowerColor();
        color.cooldownTime = 0;
        color.glass = true;
        color.clay = true;
        color.wool = true;
        color.item = item;
        item.addPower(color);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power color $cooldown:i[] $glass:s[] $clay:s[] $wool:s[]")
    @CommandDocumentation("$command.rpgitem.color.full")
    @CommandGroup("item_power_color")
    public void color(CommandSender sender, RPGItem item, int cooldown, String glass, String clay, String wool) {
        PowerColor color = new PowerColor();
        color.cooldownTime = cooldown;
        color.glass = glass.equalsIgnoreCase("true");
        color.clay = clay.equalsIgnoreCase("true");
        color.wool = wool.equalsIgnoreCase("true");
        color.item = item;
        item.addPower(color);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power command $cooldown:i[] $o[left,right] $display:s[] $command:s[]")
    @CommandDocumentation("$command.rpgitem.command")
    @CommandGroup("item_power_command_b")
    public void command(CommandSender sender, RPGItem item, int cooldown, String mouse, String displayText, String command) {
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
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power command $cooldown:i[] $o[left,right] $display:s[] $command:s[] $permission:s[]")
    @CommandDocumentation("$command.rpgitem.command.full")
    @CommandGroup("item_power_command_a")
    public void command(CommandSender sender, RPGItem item, int cooldown, String mouse, String displayText, String command, String permission) {
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
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power aoecommand $cooldown:i[] $o[left,right] $display:s[] $command:s[] $min:i[] $max:i[] $facing:i[]")
    @CommandDocumentation("$command.rpgitem.aoecommand")
    @CommandGroup("item_power_aoecommand_a")
    public void aoecommand(CommandSender sender, RPGItem item, int cooldown, String mouse, String displayText, String command, int min, int max, int facing) {
        PowerAOECommand com = new PowerAOECommand();
        com.cooldownTime = cooldown;
        command = command.trim();
        if (command.charAt(0) == '/') {
            command = command.substring(1);
        }
        com.isRight = mouse.equals("right");
        com.display = displayText;
        com.command = command;
        com.item = item;
        com.r = max;
        com.rm = min;
        com.facing = facing;
        item.addPower(com);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power aoecommand $cooldown:i[] $o[left,right] $display:s[] $command:s[] $min:i[] $max:i[] $facing:i[] $permission:s[]")
    @CommandDocumentation("$command.rpgitem.aoecommand.full")
    @CommandGroup("item_power_aoecommand_b")
    public void aoecommand(CommandSender sender, RPGItem item, int cooldown, String mouse, String displayText, String command, int min, int max, int facing, String permission) {
        PowerAOECommand com = new PowerAOECommand();
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
        com.r = max;
        com.rm = min;
        com.facing = facing;
        item.addPower(com);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power consume $o[left,right] $cooldown:i[]")
    @CommandDocumentation("$command.rpgitem.consume.full")
    @CommandGroup("item_power_consume_left")
    public void consume(CommandSender sender, RPGItem item, String mouse, int cooldown) {
        PowerConsume pow = new PowerConsume();
        pow.isRight = mouse.equals("right");
        pow.item = item;
        pow.cooldownTime = cooldown;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power command $cooldown:i[] $o[left,right] $details:s[]")
    @CommandDocumentation("$command.rpgitem.command.old")
    @CommandGroup("item_power_command_c")
    public void command(CommandSender sender, RPGItem item, int cooldown, String mouse, String details) {
        String[] pArgs = details.split("\\|");
        if (pArgs.length < 2) {
            sender.sendMessage(ChatColor.RED + Locale.get("message.error.command.format"));
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
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power commandhit $cooldown:i[] $display:s[] $command:s[]")
    @CommandDocumentation("$command.rpgitem.commandhit")
    @CommandGroup("item_power_commandhit_b")
    public void commandhit(CommandSender sender, RPGItem item, int cooldown, String displayText, String command) {
        PowerCommandHit com = new PowerCommandHit();
        command = command.trim();
        if (command.charAt(0) == '/') {
            command = command.substring(1);
        }
        com.cooldownTime = cooldown;
        com.display = displayText;
        com.command = command;
        com.item = item;
        item.addPower(com);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power commandhit $cooldown:i[] $display:s[] $command:s[] $permission:s[]")
    @CommandDocumentation("$command.rpgitem.commandhit.full")
    @CommandGroup("item_power_commandhit_a")
    public void commandhit(CommandSender sender, RPGItem item, int cooldown, String displayText, String command, String permission) {
        PowerCommandHit com = new PowerCommandHit();
        command = command.trim();
        if (command.charAt(0) == '/') {
            command = command.substring(1);
        }
        com.cooldownTime = cooldown;
        com.display = displayText;
        com.command = command;
        com.permission = permission;
        com.item = item;
        item.addPower(com);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power delayedcommand $delay:i[] $cooldown:i[] $o[left,right] $display:s[] $command:s[] $permission:s[]")
    @CommandDocumentation("$command.rpgitem.delayedcommand.full")
    @CommandGroup("item_power_delayedcommand")
    public void delayedCommand(CommandSender sender, RPGItem item, int delay, int cooldown, String mouse, String displayText, String command, String permission) {
        PowerDelayedCommand com = new PowerDelayedCommand();
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
        com.delay = delay;
        item.addPower(com);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power consume")
    @CommandDocumentation("$command.rpgitem.consume")
    @CommandGroup("item_power_consume")
    public void consume(CommandSender sender, RPGItem item) {
        PowerConsume pow = new PowerConsume();
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power consume $cooldown:i[]")
    @CommandDocumentation("$command.rpgitem.consume.cd")
    @CommandGroup("item_power_consume")
    public void consume(CommandSender sender, RPGItem item, int cooldown) {
        PowerConsume pow = new PowerConsume();
        pow.item = item;
        pow.cooldownTime = cooldown;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power consumehit")
    @CommandDocumentation("$command.rpgitem.consumehit")
    @CommandGroup("item_power_consumehit")
    public void consumehit(CommandSender sender, RPGItem item) {
        PowerConsumeHit pow = new PowerConsumeHit();
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power consumehit $cooldown:i[]")
    @CommandDocumentation("$command.rpgitem.consumehit.cd")
    @CommandGroup("item_power_consumehit")
    public void consumehit(CommandSender sender, RPGItem item, int cooldown) {
        PowerConsumeHit pow = new PowerConsumeHit();
        pow.item = item;
        pow.cooldownTime = cooldown;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power fire $cooldown:i[] $distance:i[] $burnduration:i[]")
    @CommandDocumentation("$command.rpgitem.fire")
    @CommandGroup("item_power_fire")
    public void fire(CommandSender sender, RPGItem item, int cooldown, int distance, int burnduration) {
        PowerFire pow = new PowerFire();
        pow.cooldownTime = cooldown;
        pow.distance = distance;
        pow.burnduration = burnduration;
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power fireball")
    @CommandDocumentation("$command.rpgitem.fireball")
    @CommandGroup("item_power_fireball")
    public void fireball(CommandSender sender, RPGItem item) {
        PowerFireball pow = new PowerFireball();
        pow.cooldownTime = 20;
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power fireball $cooldown:i[]")
    @CommandDocumentation("$command.rpgitem.fireball.full")
    @CommandGroup("item_power_fireball")
    public void fireball(CommandSender sender, RPGItem item, int cooldown) {
        PowerFireball pow = new PowerFireball();
        pow.item = item;
        pow.cooldownTime = cooldown;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power flame")
    @CommandDocumentation("$command.rpgitem.flame")
    @CommandGroup("item_power_flame")
    public void flame(CommandSender sender, RPGItem item) {
        PowerFlame pow = new PowerFlame();
        pow.burnTime = 20;
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power flame $burntime:i[]")
    @CommandDocumentation("$command.rpgitem.flame.full")
    @CommandGroup("item_power_flame")
    public void flame(CommandSender sender, RPGItem item, int burnTime) {
        PowerFlame pow = new PowerFlame();
        pow.item = item;
        pow.burnTime = burnTime;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power lifesteal $chance:i[]")
    @CommandDocumentation("$command.rpgitem.lifesteal")
    @CommandGroup("item_power_lifesteal")
    public void lifesteal(CommandSender sender, RPGItem item, int chance) {
        PowerLifeSteal pow = new PowerLifeSteal();
        pow.item = item;
        pow.chance = chance;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power ice")
    @CommandDocumentation("$command.rpgitem.ice")
    @CommandGroup("item_power_ice")
    public void ice(CommandSender sender, RPGItem item) {
        PowerIce pow = new PowerIce();
        pow.cooldownTime = 20;
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power ice $cooldown:i[]")
    @CommandDocumentation("$command.rpgitem.ice.full")
    @CommandGroup("item_power_ice")
    public void ice(CommandSender sender, RPGItem item, int cooldown) {
        PowerIce pow = new PowerIce();
        pow.item = item;
        pow.cooldownTime = cooldown;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power knockup")
    @CommandDocumentation("$command.rpgitem.knockup")
    @CommandGroup("item_power_knockup")
    public void knockup(CommandSender sender, RPGItem item) {
        PowerKnockup pow = new PowerKnockup();
        pow.item = item;
        pow.chance = 20;
        pow.power = 2;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power knockup $chance:i[] $power:f[]")
    @CommandDocumentation("$command.rpgitem.knockup.full")
    @CommandGroup("item_power_knockup")
    public void knockup(CommandSender sender, RPGItem item, int chance, double power) {
        PowerKnockup pow = new PowerKnockup();
        pow.item = item;
        pow.chance = chance;
        pow.power = power;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power lightning")
    @CommandDocumentation("$command.rpgitem.lightning")
    @CommandGroup("item_power_lightning")
    public void lightning(CommandSender sender, RPGItem item) {
        PowerLightning pow = new PowerLightning();
        pow.item = item;
        pow.chance = 20;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power lightning $chance:i[]")
    @CommandDocumentation("$command.rpgitem.lightning.full")
    @CommandGroup("item_power_lightning")
    public void lightning(CommandSender sender, RPGItem item, int chance) {
        PowerLightning pow = new PowerLightning();
        pow.item = item;
        pow.chance = chance;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power potionhit $chance:i[] $duration:i[] $amplifier:i[] $effect:s[]")
    @CommandDocumentation("$command.rpgitem.potionhit+PotionEffectType")
    @CommandGroup("item_power_potionhit")
    public void potionhit(CommandSender sender, RPGItem item, int chance, int duration, int amplifier, String effect) {
        PowerPotionHit pow = new PowerPotionHit();
        pow.item = item;
        pow.chance = chance;
        pow.duration = duration;
        pow.amplifier = amplifier;
        pow.type = PotionEffectType.getByName(effect);
        if (pow.type == null) {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.effect"), effect));
            return;
        }
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power potionself $cooldown:i[] $duration:i[] $amplifier:i[] $effect:s[]")
    @CommandDocumentation("$command.rpgitem.potionself+PotionEffectType")
    @CommandGroup("item_power_potionself")
    public void potionself(CommandSender sender, RPGItem item, int ccoldown, int duration, int amplifier, String effect) {
        PowerPotionSelf pow = new PowerPotionSelf();
        pow.item = item;
        pow.cooldownTime = ccoldown;
        pow.duration = duration;
        pow.amplifier = amplifier;
        pow.type = PotionEffectType.getByName(effect);
        if (pow.type == null) {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.effect"), effect));
            return;
        }
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power rainbow")
    @CommandDocumentation("$command.rpgitem.rainbow")
    @CommandGroup("item_power_rainbow")
    public void rainbow(CommandSender sender, RPGItem item) {
        PowerRainbow pow = new PowerRainbow();
        pow.cooldownTime = 20;
        pow.count = 5;
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power rainbow $cooldown:i[] $count:i[]")
    @CommandDocumentation("$command.rpgitem.rainbow.full")
    @CommandGroup("item_power_rainbow")
    public void rainbow(CommandSender sender, RPGItem item, int cooldown, int count) {
        PowerRainbow pow = new PowerRainbow();
        pow.cooldownTime = cooldown;
        pow.count = count;
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power rainbow $cooldown:i[] $count:i[] $isFire:s[]")
    @CommandDocumentation("$command.rpgitem.rainbow.full")
    @CommandGroup("item_power_rainbow")
    public void rainbow(CommandSender sender, RPGItem item, int cooldown, int count, String isFire) {
        PowerRainbow pow = new PowerRainbow();
        pow.cooldownTime = cooldown;
        pow.count = count;
        pow.item = item;
        pow.isFire = "true".equalsIgnoreCase(isFire);
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power rescue $cooldown:i[] $healthtrigger:i[] $usebed:s[]")
    @CommandDocumentation("$command.rpgitem.rescue")
    @CommandGroup("item_power_rescue")
    public void rescue(CommandSender sender, RPGItem item, int cooldown, int healthTrigger, String useBed) {
        PowerRescue pow = new PowerRescue();
        pow.item = item;
        pow.cooldownTime = cooldown;
        pow.healthTrigger = healthTrigger;
        pow.useBed = Boolean.parseBoolean(useBed);
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power rumble $cooldown:i[] $power:i[] $distance:i[]")
    @CommandDocumentation("$command.rpgitem.rumble")
    @CommandGroup("item_power_rumble")
    public void rumble(CommandSender sender, RPGItem item, int cooldown, int power, int distance) {
        PowerRumble pow = new PowerRumble();
        pow.item = item;
        pow.cooldownTime = cooldown;
        pow.power = power;
        pow.distance = distance;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power teleport")
    @CommandDocumentation("$command.rpgitem.teleport")
    @CommandGroup("item_power_teleport")
    public void teleport(CommandSender sender, RPGItem item) {
        PowerTeleport pow = new PowerTeleport();
        pow.item = item;
        pow.cooldownTime = 20;
        pow.distance = 5;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power teleport $cooldown:i[] $distance:i[]")
    @CommandDocumentation("$command.rpgitem.teleport.full")
    @CommandGroup("item_power_teleport")
    public void teleport(CommandSender sender, RPGItem item, int cooldown, int distance) {
        PowerTeleport pow = new PowerTeleport();
        pow.item = item;
        pow.cooldownTime = cooldown;
        pow.distance = distance;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power tippedarrow $cooldown:i[] $effect:s[] $duration:i[] $amplifier:i[]")
    @CommandDocumentation("$command.rpgitem.tippedarrow+PotionEffectType")
    @CommandGroup("item_power_tippedarrow")
    public void tippedarrow(CommandSender sender, RPGItem item, int cooldown, String effect, int duration, int amplifier) {
        PowerTippedArrow pow = new PowerTippedArrow();
        pow.item = item;
        pow.cooldownTime = cooldown;
        pow.duration = duration;
        pow.amplifier = amplifier;
        pow.type = PotionEffectType.getByName(effect);
        if (pow.type == null) {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.effect"), effect));
            return;
        }
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power tntcannon")
    @CommandDocumentation("$command.rpgitem.tntcannon")
    @CommandGroup("item_power_tntcannon")
    public void tntcannon(CommandSender sender, RPGItem item) {
        PowerTNTCannon pow = new PowerTNTCannon();
        pow.item = item;
        pow.cooldownTime = 20;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power tntcannon $cooldown:i[]")
    @CommandDocumentation("$command.rpgitem.tntcannon.full")
    @CommandGroup("item_power_tntcannon")
    public void tntcannon(CommandSender sender, RPGItem item, int cooldown) {
        PowerTNTCannon pow = new PowerTNTCannon();
        pow.item = item;
        pow.cooldownTime = cooldown;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power torch $cooldown:i[]")
    @CommandDocumentation("$command.rpgitem.torch")
    @CommandGroup("item_power_torch")
    public void torch(CommandSender sender, RPGItem item, int cooldown) {
        PowerTorch pow = new PowerTorch();
        pow.item = item;
        pow.cooldownTime = cooldown;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power skyhook $m[] $distance:i[]")
    @CommandDocumentation("$command.rpgitem.skyhook")
    @CommandGroup("item_power_skyhook")
    public void skyHook(CommandSender sender, RPGItem item, Material material, int distance) {
        PowerSkyHook pow = new PowerSkyHook();
        pow.item = item;
        pow.railMaterial = material;
        pow.hookDistance = distance;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power potiontick $amplifier:i[] $effect:s[]")
    @CommandDocumentation("$command.rpgitem.potiontick")
    @CommandGroup("item_power_potiontick")
    public void potionTick(CommandSender sender, RPGItem item, int amplifier, String effect) {
        PowerPotionTick pow = new PowerPotionTick();
        pow.item = item;
        pow.amplifier = amplifier;
        pow.effect = PotionEffectType.getByName(effect);
        if (pow.effect == null) {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.effect"), effect));
            return;
        }
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power food $foodpoints:i[]")
    @CommandDocumentation("$command.rpgitem.food")
    @CommandGroup("item_power_food")
    public void food(CommandSender sender, RPGItem item, int foodpoints) {
        PowerFood pow = new PowerFood();
        pow.item = item;
        pow.foodpoints = foodpoints;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    //always cause confusion...projectileType may eat the argument behind and get something like `skull 1`
/*    @CommandString("rpgitem $n[] power projectile $projectileType:s[]")
    @CommandDocumentation("$command.rpgitem.projectile")
    @CommandGroup("item_power_projectile_a")
    public void projectile(CommandSender sender, RPGItem item, String projectileType) {
        PowerProjectile power = new PowerProjectile();
        if (!power.acceptableType(projectileType)) {
            sender.sendMessage(ChatColor.RED + Locale.get("power.projectile.badType"));
            return;
        }
        power.setType(projectileType);
        power.item = item;
        item.addPower(power);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }*/

    @CommandString("rpgitem $n[] power projectile $projectileType:s[] $cooldown:i[]")
    @CommandDocumentation("$command.rpgitem.projectile.full")
    @CommandGroup("item_power_projectile_b")
    public void projectile(CommandSender sender, RPGItem item, String projectileType, int cooldown) {
        PowerProjectile power = new PowerProjectile();
        power.cooldownTime = cooldown;
        if (!power.acceptableType(projectileType)) {
            sender.sendMessage(ChatColor.RED + Locale.get("power.projectile.badType"));
            return;
        }
        power.setType(projectileType);
        power.item = item;
        item.addPower(power);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power projectile $projectileType:s[] $cooldown:i[] $range:i[] $amount:i[]")
    @CommandDocumentation("$command.rpgitem.projectile.cone")
    @CommandGroup("item_power_projectile_c")
    public void projectile(CommandSender sender, RPGItem item, String projectileType, int cooldown, int range, int amount) {
        PowerProjectile power = new PowerProjectile();
        if (projectileType.equalsIgnoreCase("fireball")) {
            sender.sendMessage(ChatColor.RED + Locale.get("power.projectile.noFireball"));
            return;
        }
        if (!power.acceptableType(projectileType)) {
            sender.sendMessage(ChatColor.RED + Locale.get("power.projectile.badType"));
            return;
        }
        power.setType(projectileType);
        power.range = range;
        power.amount = amount;
        power.cooldownTime = cooldown;
        power.item = item;
        power.cone = true;
        item.addPower(power);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power deathcommand $chance:i[] $command:s[]")
    @CommandDocumentation("$command.rpgitem.deathcommand")
    @CommandGroup("item_power_deathcommand")
    public void deathcommand(CommandSender sender, RPGItem item, int chance, String command) {
        PowerDeathCommand power = new PowerDeathCommand();
        power.item = item;
        power.chance = chance;
        power.command = command;
        item.addPower(power);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power deathcommand $chance:i[] $count:i[] $command:s[] $descriptionline:s[]")
    @CommandDocumentation("$command.rpgitem.deathcommand.full")
    @CommandGroup("item_power_deathcommand")
    public void deathcommand(CommandSender sender, RPGItem item, int chance, int count, String command, String description) {
        PowerDeathCommand power = new PowerDeathCommand();
        power.item = item;
        power.chance = chance;
        power.command = command;
        power.desc = description;
        power.count = count;
        item.addPower(power);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power forcefield $cooldown:i[] $radius:i[] $height:i[] $base:i[] $duration:i[]")
    @CommandDocumentation("$command.rpgitem.forcefield")
    @CommandGroup("item_power_forcefield")
    public void forcefield(CommandSender sender, RPGItem item, int cooldown, int radius, int height, int base, int duration) {
        PowerForceField p = new PowerForceField();
        p.item = item;
        p.cooldownTime = cooldown;
        p.radius = radius;
        p.height = height;
        p.base = base;
        p.ttl = duration;
        item.addPower(p);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power attract")
    @CommandDocumentation("$command.rpgitem.attract")
    @CommandGroup("item_power_attract")
    public void attract(CommandSender sender, RPGItem item) {
        PowerAttract p = new PowerAttract();
        p.item = item;
        item.addPower(p);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power attract $radius:i[] $maxSpeed:f[]")
    @CommandDocumentation("$command.rpgitem.attract.full")
    @CommandGroup("item_power_attract")
    public void attract(CommandSender sender, RPGItem item, int radius, double speed) {
        PowerAttract p = new PowerAttract();
        p.item = item;
        p.radius = radius;
        p.maxSpeed = speed;
        item.addPower(p);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power pumpkin $chance:i[] $dropChance:f[]")
    @CommandDocumentation("$command.rpgitem.pumpkin")
    @CommandGroup("item_power_pumpkin")
    public void lantern(CommandSender sender, RPGItem item, int chance, double dropChance) {
        PowerPumpkin p = new PowerPumpkin();
        p.item = item;
        p.chance = chance;
        p.drop = dropChance;
        item.addPower(p);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power particle $visualeffect:s[]")
    @CommandDocumentation("$command.rpgitem.particle")
    @CommandGroup("item_power_particle")
    public void particle(CommandSender sender, RPGItem item, String effect) {
        if (!PowerParticle.acceptableEffect(effect)) {
            for (Effect e : Effect.values()) {
                if (PowerParticle.acceptableEffect(e.name()))
                    sender.sendMessage(e.name());
            }
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.visualeffect"), effect));
            return;
        }
        PowerParticle p = new PowerParticle();
        p.item = item;
        p.effect = effect.toUpperCase();
        item.addPower(p);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power particletick $visualeffect:s[] $interval:i[]")
    @CommandDocumentation("$command.rpgitem.particletick")
    @CommandGroup("item_power_particletick")
    public void particle(CommandSender sender, RPGItem item, String effect, int interval) {
        if (!PowerParticle.acceptableEffect(effect)) {
            for (Effect e : Effect.values()) {
                if (PowerParticle.acceptableEffect(e.name()))
                    sender.sendMessage(e.name());
            }
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.visualeffect"), effect));
            return;
        }
        PowerParticleTick p = new PowerParticleTick();
        p.item = item;
        p.effect = effect.toUpperCase();
        p.interval = interval;
        item.addPower(p);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power unbreakable")
    @CommandDocumentation("$command.rpgitem.unbreakable")
    @CommandGroup("item_power_unbreakable")
    public void particle(CommandSender sender, RPGItem item) {
        PowerUnbreakable p = new PowerUnbreakable();
        p.item = item;
        item.addPower(p);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power lorefilter $regex:s[] $descriptionline:s[]")
    @CommandDocumentation("$command.rpgitem.lorefilter")
    @CommandGroup("item_power_lorefilter")
    public void lorefilter(CommandSender sender, RPGItem item, String regex, String description) {
        PowerLoreFilter p = new PowerLoreFilter();
        p.item = item;
        p.regex = regex;
        p.desc = description;
        item.addPower(p);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power ranged")
    @CommandDocumentation("$command.rpgitem.ranged")
    @CommandGroup("item_power_ranged")
    public void ranged(CommandSender sender, RPGItem item) {
        PowerRanged pow = new PowerRanged();
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power rangedonly")
    @CommandDocumentation("$command.rpgitem.rangedonly")
    @CommandGroup("item_power_rangedonly")
    public void rangedonly(CommandSender sender, RPGItem item) {
        PowerRangedOnly pow = new PowerRangedOnly();
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power deflect")
    @CommandDocumentation("$command.rpgitem.deflect")
    @CommandGroup("item_power_deflect")
    public void deflect(CommandSender sender, RPGItem item) {
        PowerDeflect pow = new PowerDeflect();
        pow.item = item;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }

    @CommandString("rpgitem $n[] power realdamage $cooldown:i[] $damage:i[]")
    @CommandDocumentation("$command.rpgitem.realdamage")
    @CommandGroup("item_power_realdamage")
    public void realdamage(CommandSender sender, RPGItem item, int cooldown, int damage) {
        PowerRealDamage pow = new PowerRealDamage();
        pow.item = item;
        pow.cooldownTime = cooldown;
        pow.realDamage = damage;
        item.addPower(pow);
        ItemManager.save(Plugin.plugin);
        sender.sendMessage(ChatColor.AQUA + Locale.get("message.power.ok"));
    }
}
