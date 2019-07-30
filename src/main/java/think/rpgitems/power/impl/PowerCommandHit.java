package think.rpgitems.power.impl;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import static think.rpgitems.power.Utils.attachPermission;
import static think.rpgitems.power.Utils.checkAndSetCooldown;


/**
 * Power commandhit.
 * <p>
 * The item will run {@link #getCommand()} when player hits some {@link LivingEntity}
 * giving the permission {@link #getPermission()} just for the use of the command.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = "HIT", generalInterface = PowerLivingEntity.class, implClass = PowerCommandHit.Impl.class)
public class PowerCommandHit extends PowerCommand {

    @Property
    private double minDamage = 0;

    public class Impl implements PowerHit, PowerLivingEntity {
        /**
         * Execute command
         *
         * @param player player
         * @param e      entity
         * @param damage damage
         * @return PowerResult with proposed damage
         */
        protected PowerResult<Void> executeCommand(Player player, LivingEntity e, double damage) {
            if (!player.isOnline()) return PowerResult.noop();

            attachPermission(player, getPermission());
            boolean wasOp = player.isOp();
            try {
                if (getPermission().equals("*"))
                    player.setOp(true);

                String cmd = getCommand();

                cmd = handleEntityPlaceHolder(e, cmd);

                cmd = handlePlayerPlaceHolder(player, cmd);

                cmd = cmd.replaceAll("\\{damage}", String.valueOf(damage));

                boolean result = player.performCommand(cmd);
                return result ? PowerResult.ok() : PowerResult.fail();
            } finally {
                if (getPermission().equals("*"))
                    player.setOp(wasOp);
            }
        }


        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, Double damage) {
            if (damage == null || damage < getMinDamage()) return PowerResult.noop();
            if (!checkAndSetCooldown(getPower(), player, getCooldown(), true, false, getCommand())) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();

            return executeCommand(player, entity, damage);
        }

        @Override
        public Power getPower() {
            return PowerCommandHit.this;
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
    public String displayText() {
        return ChatColor.GREEN + getDisplay();
    }

    /**
     * Minimum damage to trigger
     */
    public double getMinDamage() {
        return minDamage;
    }

    @Override
    public String getName() {
        return "commandhit";
    }

    public void setMinDamage(double minDamage) {
        this.minDamage = minDamage;
    }
}
