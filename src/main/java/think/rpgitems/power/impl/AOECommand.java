package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import java.util.List;

import static think.rpgitems.power.Utils.*;

/**
 * Power aoecommand.
 * <p>
 * The item will run {@link #command} for every entity
 * in range({@link #rm min} blocks ~ {@link #r max} blocks in {@link #facing view angle})
 * on isRightgiving the {@link #permission}
 * just for the use of the command.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, implClass = AOECommand.Impl.class)
public class AOECommand extends Command {
    @Property
    public boolean selfapplication = false;

    @Property
    @AcceptedValue({"entity", "player", "mobs"})
    public String type = "entity";

    @Property(order = 6)
    public int r = 10;

    @Property(order = 5)
    public int rm = 0;

    @Property(order = 7, required = true)
    public double facing = 30;

    @Property
    public int c = 100;

    @Property
    public boolean mustsee = false;

    /**
     * Maximum count
     */
    public int getC() {
        return c;
    }

    /**
     * Maximum view angle
     */
    public double getFacing() {
        return facing;
    }

    @Override
    public String getName() {
        return "aoecommand";
    }

    /**
     * Maximum radius
     */
    public int getR() {
        return r;
    }

    /**
     * Minimum radius
     */
    public int getRm() {
        return rm;
    }

    /**
     * Type of targets. Can be `entity` `player` `mobs` now
     * entity: apply the command to every {@link LivingEntity} in range
     * player: apply the command to every {@link Player} in range
     * mobs: apply the command to every {@link LivingEntity}  except {@link Player}in range
     */
    public String getType() {
        return type;
    }

    /**
     * Whether only apply to the entities that player have line of sight
     */
    public boolean isMustsee() {
        return mustsee;
    }

    /**
     * Whether the command will be apply to the user
     */
    public boolean isSelfapplication() {
        return selfapplication;
    }

    public class Impl implements PowerPlain, PowerRightClick, PowerLeftClick, PowerHit {
        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkAndSetCooldown(getPower(), player, getCooldown(), true, false, getCommand()))
                return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            if (!player.isOnline()) return PowerResult.noop();

            attachPermission(player, getPermission());

            String usercmd = handlePlayerPlaceHolder(player, getCommand());

            boolean wasOp = player.isOp();
            try {
                if (getPermission().equals("*"))
                    player.setOp(true);
                boolean forPlayers = getType().equalsIgnoreCase("player");
                boolean forMobs = getType().equalsIgnoreCase("mobs");
                int count = getC();
                if (getType().equalsIgnoreCase("entity") || forPlayers || forMobs) {
                    List<LivingEntity> nearbyEntities = getNearestLivingEntities(getPower(), player.getLocation(), player, getR(), getRm());
                    List<LivingEntity> ent = getLivingEntitiesInCone(nearbyEntities, player.getEyeLocation().toVector(), getFacing(), player.getEyeLocation().getDirection());
                    LivingEntity[] entities = ent.toArray(new LivingEntity[0]);
                    for (int i = 0; i < count && i < entities.length; ++i) {
                        String cmd = usercmd;
                        LivingEntity e = entities[i];
                        if ((isMustsee() && !player.hasLineOfSight(e))
                                    || (!isSelfapplication() && e == player)
                                    || (forPlayers && !(e instanceof Player))
                                    || (forMobs && e instanceof Player)
                        ) {
                            ++count;
                            continue;
                        }
                        cmd = CommandHit.handleEntityPlaceHolder(e, cmd);
                        Bukkit.getServer().dispatchCommand(player, cmd);
                    }
                }
            } finally {
                if (getPermission().equals("*"))
                    player.setOp(wasOp);
            }

            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return AOECommand.this;
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack).with(damage);
        }
    }
}
