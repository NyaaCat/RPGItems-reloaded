package think.rpgitems.support;


import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import think.rpgitems.RPGItems;
import think.rpgitems.event.PowerActivateEvent;

import java.util.logging.Level;

import static org.bukkit.Bukkit.getServer;

public class ResidenceSupport {
    public static boolean useResidence = true;
    private static boolean hasSupport = false;
    public static boolean hasSupport(){
        return hasSupport;
    }
    public static void init(RPGItems plugin) {
        try{
            useResidence = plugin.cfg.useResidence;
            Plugin resPlug = plugin.getServer().getPluginManager().getPlugin("Residence");
            if (!useResidence || resPlug==null) {
                return;
            }
            String resVersion = resPlug.getDescription().getVersion();
            RPGItems.logger.info("Residence version: " + resVersion + " found");
            hasSupport = true;
        }catch (Exception e){
            RPGItems.logger.log(Level.WARNING, "Error enabling Residence support", e);
            hasSupport = false;
        }
        if(hasSupport()){
            FlagPermissions.addFlag("enable-rpgitems-power");
            FlagPermissions.addMaterialToUseFlag(Material.DRAGON_BREATH, Flags.getFlag("enable-rpgitems-power"));
            getServer().getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onPowerActivate(PowerActivateEvent event) {
                    Location loc = event.getPlayer().getLocation();
                    ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(loc);
                    if(res!=null){
                        ResidencePermissions perms = res.getPermissions();
                        boolean hasPermission = perms.playerHas(event.getPlayer().getName(),"enable-rpgitems-power", true);
                        if(!hasPermission)
                            event.setCancelled(true);
                    }
                }
            }, plugin);
        }
    }
}
