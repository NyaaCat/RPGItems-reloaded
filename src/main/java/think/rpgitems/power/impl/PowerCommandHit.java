package think.rpgitems.power.impl;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import static think.rpgitems.power.Utils.attachPermission;
import static think.rpgitems.power.Utils.checkCooldownByString;


/**
 * Power commandhit.
 * <p>
 * The item will run {@link #command} when player hits some {@link LivingEntity}
 * giving the permission {@link #permission} just for the use of the command.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = "HIT", generalInterface = PowerLivingEntity.class)
public class PowerCommandHit extends BasePower implements PowerHit, PowerLivingEntity {

    /**
     * Command to be executed
     */
    @Property(order = 2, required = true)
    public String command = "";
    /**
     * Display text of this power
     */
    @Property(order = 1)
    public String display = "Runs command";
    /**
     * Permission will be given to user executing the {@link #command}
     */
    @Property(order = 3)
    public String permission = "";
    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;
    /**
     * Minimum damage to trigger
     */
    @Property
    public double minDamage = 0;

    /**
     * Execute command
     * @param player player
     * @param e      entity
     * @param damage damage
     *
     * @return PowerResult with proposed damage
     */
    protected PowerResult<Void> executeCommand(Player player, LivingEntity e, double damage) {
        if (!player.isOnline()) return PowerResult.noop();

        attachPermission(player, permission);
        boolean wasOp = player.isOp();
        try {
            if (permission.equals("*"))
                player.setOp(true);

            String cmd = command;

            cmd = cmd.replaceAll("\\{entity}", e.getName());
            cmd = cmd.replaceAll("\\{entity.uuid}", e.getUniqueId().toString());
            cmd = cmd.replaceAll("\\{entity.x}", Float.toString(e.getLocation().getBlockX()));
            cmd = cmd.replaceAll("\\{entity.y}", Float.toString(e.getLocation().getBlockY()));
            cmd = cmd.replaceAll("\\{entity.z}", Float.toString(e.getLocation().getBlockZ()));
            cmd = cmd.replaceAll("\\{entity.yaw}", Float.toString(90 + e.getEyeLocation().getYaw()));
            cmd = cmd.replaceAll("\\{entity.pitch}", Float.toString(-e.getEyeLocation().getPitch()));

            cmd = cmd.replaceAll("\\{player}", player.getName());
            cmd = cmd.replaceAll("\\{player.x}", Float.toString(-player.getLocation().getBlockX()));
            cmd = cmd.replaceAll("\\{player.y}", Float.toString(-player.getLocation().getBlockY()));
            cmd = cmd.replaceAll("\\{player.z}", Float.toString(-player.getLocation().getBlockZ()));
            cmd = cmd.replaceAll("\\{player.yaw}", Float.toString(90 + player.getEyeLocation().getYaw()));
            cmd = cmd.replaceAll("\\{player.pitch}", Float.toString(-player.getEyeLocation().getPitch()));

            cmd = cmd.replaceAll("\\{damage}", String.valueOf(damage));

            boolean result = player.performCommand(cmd);
            return result ? PowerResult.ok() : PowerResult.fail();
        } finally {
            if (permission.equals("*"))
                player.setOp(wasOp);
        }
    }

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        return fire(player, stack, entity, damage).with(damage);
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, double damage) {
        if (damage < minDamage) return PowerResult.noop();
        if (!checkCooldownByString(player, getItem(), command, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();

        return executeCommand(player, entity, damage);
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + display;
    }

    @Override
    public String getName() {
        return "commandhit";
    }
}
