package think.rpgitems.power;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerRightClick;

public class PowerSkyHook extends Power implements PowerRightClick {
    
    public Material railMaterial = Material.GLASS;
    public int hookDistance = 10;
    
    @Override
    public void rightClick(final Player player) {
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false){
        }else{ 
    	RPGValue isHooking = RPGValue.get(player, item, "skyhook.isHooking");
        if (isHooking == null) {
            isHooking = new RPGValue(player, item, "skyhook.isHooking", false);
        }
        if (isHooking.asBoolean()) {
            player.setVelocity(player.getLocation().getDirection());
            isHooking.set(false);
            return;
        }
        Block block = player.getTargetBlock(null, hookDistance);
        if (block.getType() != railMaterial) {
            player.sendMessage(ChatColor.AQUA + Locale.get("message.skyhook.fail", Locale.getPlayerLocale(player)));
            return;
        }
        isHooking.set(true);
        final Location location = player.getLocation();
        player.setAllowFlight(true);
        player.setVelocity(location.getDirection().multiply(block.getLocation().distance(location) / 2d));
        player.setFlying(true);
        (new BukkitRunnable() {
            
            private int delay = 0;
            
            @Override
            public void run() {
                if (!player.getAllowFlight()) {
                    cancel();
                    RPGValue.get(player, item, "skyhook.isHooking").set(false);
                    return;
                }
                if (!RPGValue.get(player, item, "skyhook.isHooking").asBoolean()) {
                    player.setFlying(false);
                    if (player.getGameMode() != GameMode.CREATIVE) 
                        player.setAllowFlight(false);
                    cancel();
                    return;
                }
                player.setFlying(true);
                player.getLocation(location);
                location.add(0, 2.4, 0);
                if (delay < 20) {
                    delay++;
                    if (location.getBlock().getType() == railMaterial) {
                        delay = 20;
                    }
                    return;
                }
                Vector dir = location.getDirection().setY(0).normalize();
                location.add(dir);
                if (location.getBlock().getType() != railMaterial) {
                    player.setFlying(false);
                    if (player.getGameMode() != GameMode.CREATIVE) 
                        player.setAllowFlight(false);
                    cancel();
                    RPGValue.get(player, item, "skyhook.isHooking").set(false);
                    return;
                }
                player.setVelocity(dir.multiply(0.5));
                
            }
        }).runTaskTimer(Plugin.plugin, 0, 0);
    }
    }
    @Override
    public void init(ConfigurationSection s) {
        railMaterial = Material.valueOf(s.getString("railMaterial", "GLASS"));
        hookDistance = s.getInt("hookDistance", 10);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("railMaterial", railMaterial.toString());
        s.set("hookDistance", hookDistance);
    }

    @Override
    public String getName() {
        return "skyhook";
    }

    @Override
    public String displayText(String locale) {
        return ChatColor.GREEN + Locale.get("power.skyhook", locale);
    }

}
