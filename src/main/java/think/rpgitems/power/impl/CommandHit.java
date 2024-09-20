package think.rpgitems.power.impl;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import java.util.HashMap;

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
@Meta(defaultTrigger = "HIT", generalInterface = PowerLivingEntity.class, implClass = CommandHit.Impl.class)
public class CommandHit extends Command {

    @Property
    public double minDamage = 0;

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

    @Override
    public String displayText() {
        return ChatColor.GREEN + getDisplay();
    }

    public class Impl implements PowerHit, PowerLivingEntity, PowerBeamHit {
        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, Double damage) {
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("damage",damage);
            argsMap.put("target",entity);
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent())
                return PowerResult.fail();
            if (damage == null || damage < getMinDamage()) return PowerResult.noop();
            if (!checkAndSetCooldown(getPower(), player, getCooldown(), true, false, getItem().getUid() + "." + getCommand()))
                return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();

            return executeCommand(player, entity, damage);
        }

        @Override
        public Power getPower() {
            return CommandHit.this;
        }

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
        public PowerResult<Double> hitEntity(Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> beamEnd(Player player, ItemStack stack, Location location, BeamEndEvent event) {
            return PowerResult.noop();
        }
    }
}
