package think.rpgitems.power;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Power aoecommand.
 * <p>
 * The item will run {@link #command} for every entity
 * in range({@link #rm min} blocks ~ {@link #r max} blocks in {@link #facing view angle})
 * on {@link #isRight click} giving the {@link #permission}
 * just for the use of the command.
 * </p>
 */
public class PowerAOECommand extends PowerCommand {
    /**
     * Whether the command will be apply to the user
     */
    public boolean selfapplication = false;

    /**
     * Type of targets. Can be `entity` `player` `mobs` now
     * entity: apply the command to every {@link LivingEntity} in range
     * player: apply the command to every {@link Player} in range
     * mobs: apply the command to every {@link LivingEntity}  except {@link Player}in range
     */
    public String type = "entity";

    /**
     * Maximum radius
     */
    public int r = 10;

    /**
     * Minimum radius
     */
    public int rm = 0;

    /**
     * Maximum view angle
     */
    public double facing = 30;

    /**
     * Maximum count
     */
    public int c = 100;

    /**
     * Whether only apply to the entities that player have line of sight
     */
    public boolean mustsee = false;

    @Override
    public String getName() {
        return "aoecommand";
    }

    private void aoeCommand(Player player) {
        if (!player.isOnline()) return;

        AttachPermission(player, permission);

        String usercmd = command;
        usercmd = usercmd.replaceAll("\\{player}", player.getName());
        usercmd = usercmd.replaceAll("\\{player.x}", Float.toString(-player.getLocation().getBlockX()));
        usercmd = usercmd.replaceAll("\\{player.y}", Float.toString(-player.getLocation().getBlockY()));
        usercmd = usercmd.replaceAll("\\{player.z}", Float.toString(-player.getLocation().getBlockZ()));
        usercmd = usercmd.replaceAll("\\{player.yaw}", Float.toString(90 + player.getEyeLocation().getYaw()));
        usercmd = usercmd.replaceAll("\\{player.pitch}", Float.toString(-player.getEyeLocation().getPitch()));


        boolean wasOp = player.isOp();
        if (permission.equals("*"))
            player.setOp(true);
        boolean forPlayers = type.equalsIgnoreCase("player");
        boolean forMobs = type.equalsIgnoreCase("mobs");

        if (type.equalsIgnoreCase("entity") || forPlayers || forMobs) {
            LivingEntity[] nearbyEntities = getNearbyLivingEntities(player.getLocation(), r, rm);
            List<LivingEntity> ent = getEntitiesInCone(nearbyEntities, player.getEyeLocation().toVector(), facing, player.getEyeLocation().getDirection());
            LivingEntity[] entities = ent.toArray(new LivingEntity[ent.size()]);
            for (int i = 0; i < c && i < entities.length; ++i) {
                String cmd = usercmd;
                LivingEntity e = entities[i];
                if ((mustsee && !player.hasLineOfSight(e))
                        || (!selfapplication && e == player)
                        || (forPlayers && !(e instanceof Player))
                        || (forMobs && e instanceof Player)
                        ) {
                    ++c;
                    continue;
                }
                cmd = cmd.replaceAll("\\{entity}", e.getName());
                cmd = cmd.replaceAll("\\{entity.uuid}", e.getUniqueId().toString());
                cmd = cmd.replaceAll("\\{entity.x}", Float.toString(e.getLocation().getBlockX()));
                cmd = cmd.replaceAll("\\{entity.y}", Float.toString(e.getLocation().getBlockY()));
                cmd = cmd.replaceAll("\\{entity.z}", Float.toString(e.getLocation().getBlockZ()));
                cmd = cmd.replaceAll("\\{entity.yaw}", Float.toString(90 + e.getEyeLocation().getYaw()));
                cmd = cmd.replaceAll("\\{entity.pitch}", Float.toString(-e.getEyeLocation().getPitch()));
                Bukkit.getServer().dispatchCommand(player, cmd);
            }
        }

        if (permission.equals("*"))
            player.setOp(wasOp);
    }

    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true))return;
        if (!isRight || !checkCooldownByString(player, item, command, cooldownTime)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        aoeCommand(player);
    }

    @Override
    public void leftClick(Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true))return;
        if (isRight || !checkCooldownByString(player, item, command, cooldownTime)) return;
        if (!item.consumeDurability(stack, consumption)) return;
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
        mustsee = s.getBoolean("mustsee", mustsee);
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
