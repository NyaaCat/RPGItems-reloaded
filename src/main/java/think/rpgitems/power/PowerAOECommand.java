package think.rpgitems.power;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.Plugin;

import java.util.List;

public class PowerAOECommand extends PowerCommand {
    public boolean selfapplication = false;

    public String type = "entity";

    public int r = 10;

    public int rm = 0;

    public double facing = 30;

    public int c = 100;

    public boolean mustsee = false;

    @Override
    public String displayText() {
        return super.displayText();
    }

    @Override
    public String getName() {
        return "aoecommand";
    }

    private void aoeCommand(Player player){
        if (!player.isOnline()) return;

        if (permission.length() != 0 && !permission.equals("*")) {
            PermissionAttachment attachment = player.addAttachment(Plugin.plugin, 1);
            String[] perms = permission.split("\\.");
            StringBuilder p = new StringBuilder();
            for (int i = 0; i < perms.length; i++) {
                p.append(perms[i]);
                attachment.setPermission(p.toString(), true);
                p.append('.');
            }
        }

        String usercmd = command;
        usercmd = usercmd.replaceAll("\\{player\\}", player.getName());
        usercmd = usercmd.replaceAll("\\{player.x\\}", Float.toString(-player.getLocation().getBlockX()));
        usercmd = usercmd.replaceAll("\\{player.y\\}", Float.toString(-player.getLocation().getBlockY()));
        usercmd = usercmd.replaceAll("\\{player.z\\}", Float.toString(-player.getLocation().getBlockZ()));
        usercmd = usercmd.replaceAll("\\{player.yaw\\}", Float.toString(90 + player.getEyeLocation().getYaw()));
        usercmd = usercmd.replaceAll("\\{player.pitch\\}", Float.toString(-player.getEyeLocation().getPitch()));


        boolean wasOp = player.isOp();
        if (permission.equals("*"))
            player.setOp(true);
        boolean isPlayer = type.equalsIgnoreCase("player");

        if(type.equalsIgnoreCase("entity") || isPlayer){
            LivingEntity[] nearbyEntities = getNearbyLivingEntities(player.getLocation(), r, rm);
            List<LivingEntity> ent = getEntitiesInCone(nearbyEntities, player.getEyeLocation().toVector(), facing, player.getEyeLocation().getDirection());
            LivingEntity[] entities = ent.toArray(new LivingEntity[ent.size()]);
            for(int i = 0; i < c && i < entities.length; ++i){
                String cmd = usercmd;
                LivingEntity e = entities[i];
                if(     (mustsee && ! player.hasLineOfSight(e))
                        || (!selfapplication && e == player)
                        || (isPlayer && !(e instanceof Player)) ){
                    ++c;
                    continue;
                }
                cmd = cmd.replaceAll("\\{entity\\}", e.getName());
                cmd = cmd.replaceAll("\\{entity.uuid\\}", e.getUniqueId().toString());
                cmd = cmd.replaceAll("\\{entity.x\\}", Float.toString(e.getLocation().getBlockX()));
                cmd = cmd.replaceAll("\\{entity.y\\}", Float.toString(e.getLocation().getBlockY()));
                cmd = cmd.replaceAll("\\{entity.z\\}", Float.toString(e.getLocation().getBlockZ()));
                cmd = cmd.replaceAll("\\{entity.yaw\\}", Float.toString( 90 + e.getEyeLocation().getYaw()));
                cmd = cmd.replaceAll("\\{entity.pitch\\}", Float.toString(-e.getEyeLocation().getPitch()));
                Bukkit.getServer().dispatchCommand(player, cmd);
            }
        }

        if (permission.equals("*"))
            player.setOp(wasOp);
    }

    @Override
    public void rightClick(Player player, ItemStack i, Block clicked) {
        if (item.getHasPermission() && !player.hasPermission(item.getPermission())) return;
        if (!isRight || !updateCooldown(player)) return;
        if(!item.consumeDurability(i,consumption))return;
        aoeCommand(player);
    }

    @Override
    public void leftClick(Player player, ItemStack i, Block clicked) {
        if (item.getHasPermission() && !player.hasPermission(item.getPermission())) return;
        if (isRight || !updateCooldown(player)) return;
        if(!item.consumeDurability(i,consumption))return;
        aoeCommand(player);
    }

    @Override
    public void init(ConfigurationSection s) {
        type = s.getString("type", "entity");
        r = s.getInt("r", 10);
        rm = s.getInt("rm", 0);
        facing = s.getDouble("facing", 30);
        c = s.getInt("c", 100);
        selfapplication = s.getBoolean("selfapplication", false);
        mustsee = s.getBoolean("mustSee", mustsee);
        super.init(s);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("type", type);
        s.set("r", r);
        s.set("rm", rm);
        s.set("facing", facing);
        s.set("c", c);
        s.set("selfapplication", selfapplication);
        s.set("mustsee", mustsee);
        super.save(s);
    }
}
