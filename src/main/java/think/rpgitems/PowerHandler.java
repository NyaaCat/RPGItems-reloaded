package think.rpgitems;

import org.bukkit.ChatColor;
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
    
    @CommandString("rpgitem $n[] power fire $cooldown:i[] $distance:i[] $burnduration:i[]")
    @CommandDocumentation("$command.rpgitem.fire")
    @CommandGroup("item_power_fire")
    public void fire(CommandSender sender, RPGItem item, int cooldown, int distance, int burnduration) {
        PowerFire pow = new PowerFire();
        pow.cooldownTime = 20;
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
    public void lifsteal(CommandSender sender, RPGItem item, int chance) {
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
        pow.time = duration;
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
}
