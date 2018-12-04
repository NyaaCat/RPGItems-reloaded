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
public class PowerCommandHit extends PowerCommand implements PowerHit, PowerLivingEntity {

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

            cmd = handleEntityPlaceHolder(e, cmd);

            cmd = handlePlayerPlaceHolder(player, cmd);

            cmd = cmd.replaceAll("\\{damage}", String.valueOf(damage));

            boolean result = player.performCommand(cmd);
            return result ? PowerResult.ok() : PowerResult.fail();
        } finally {
            if (permission.equals("*"))
                player.setOp(wasOp);
        }
    }

    public static String handleEntityPlaceHolder(LivingEntity e, String cmd) {
        cmd = cmd.replaceAll("\\{entity}", e.getName());
        cmd = cmd.replaceAll("\\{entity\\.uuid}", e.getUniqueId().toString());
        cmd = cmd.replaceAll("\\{entity\\.x}", Float.toString(e.getLocation().getBlockX()));
        cmd = cmd.replaceAll("\\{entity\\.y}", Float.toString(e.getLocation().getBlockY()));
        cmd = cmd.replaceAll("\\{entity\\.z}", Float.toString(e.getLocation().getBlockZ()));
        cmd = cmd.replaceAll("\\{entity\\.yaw}", Float.toString(90 + e.getEyeLocation().getYaw()));
        cmd = cmd.replaceAll("\\{entity\\.pitch}", Float.toString(-e.getEyeLocation().getPitch()));
        return cmd;
    }

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        return fire(player, stack, entity, damage).with(damage);
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, double damage) {
        if (damage < minDamage) return PowerResult.noop();
        if (!checkCooldownByString(this, player, command, cooldown, true, false)) return PowerResult.cd();
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
