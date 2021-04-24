package think.rpgitems.power.impl;

import static think.rpgitems.power.Utils.*;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;

/**
 * Power aoecommand.
 *
 * <p>The item will run {@link #command} for every entity in range({@link #rm min} blocks ~ {@link
 * #r max} blocks in {@link #facing view angle}) on isRightgiving the {@link #permission} just for
 * the use of the command.
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, implClass = AOECommand.Impl.class)
public class AOECommand extends Command {
    @Property public boolean selfapplication = false;

    @Property
    @AcceptedValue({"entity", "player", "mobs"})
    public String type = "entity";

    @Property(order = 6)
    public int r = 10;

    @Property(order = 5)
    public int rm = 0;

    @Property(order = 7, required = true)
    public double facing = 30;

    @Property public int c = 100;

    @Property public boolean mustsee = false;

    /** Maximum count */
    public int getC() {
        return c;
    }

    /** Maximum view angle */
    public double getFacing() {
        return facing;
    }

    @Override
    public String getName() {
        return "aoecommand";
    }

    /** Maximum radius */
    public int getR() {
        return r;
    }

    /** Minimum radius */
    public int getRm() {
        return rm;
    }

    /**
     * Type of targets. Can be `entity` `player` `mobs` now entity: apply the command to every
     * {@link LivingEntity} in range player: apply the command to every {@link Player} in range
     * mobs: apply the command to every {@link LivingEntity} except {@link Player}in range
     */
    public String getType() {
        return type;
    }

    /** Whether only apply to the entities that player have line of sight */
    public boolean isMustsee() {
        return mustsee;
    }

    /** Whether the command will be apply to the user */
    public boolean isSelfapplication() {
        return selfapplication;
    }

    public static class Impl
            implements PowerPlain<AOECommand>,
                    PowerRightClick<AOECommand>,
                    PowerLeftClick<AOECommand>,
                    PowerHit<AOECommand>,
                    PowerBeamHit<AOECommand>,
                    PowerProjectileHit<AOECommand>,
                    PowerSneak<AOECommand>,
                    PowerLivingEntity<AOECommand> {
        @Override
        public PowerResult<Void> rightClick(
                AOECommand power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(AOECommand power, Player player, ItemStack stack) {
            List<LivingEntity> nearbyEntities =
                    getNearestLivingEntities(
                            power, player.getLocation(), player, power.getR(), power.getRm());
            List<LivingEntity> livingEntitiesInCone =
                    getLivingEntitiesInCone(
                            nearbyEntities,
                            player.getEyeLocation().toVector(),
                            power.getFacing(),
                            player.getEyeLocation().getDirection());
            return fire(power, player, stack, livingEntitiesInCone);
        }

        private PowerResult<Void> fire(
                AOECommand power,
                Player player,
                ItemStack stack,
                List<LivingEntity> entitiesInCone) {
            if (!player.isOnline()) return PowerResult.noop();

            attachPermission(player, power.getPermission());

            String usercmd = handlePlayerPlaceHolder(player, power.getCommand());
            String aoeType = power.getType();
            boolean wasOp = player.isOp();
            try {
                if (power.getPermission().equals("*")) player.setOp(true);
                boolean forPlayers = aoeType.equalsIgnoreCase("player");
                boolean forMobs = aoeType.equalsIgnoreCase("mobs");
                int count = power.getC();
                if (aoeType.equalsIgnoreCase("entity") || forPlayers || forMobs) {
                    LivingEntity[] entities = entitiesInCone.toArray(new LivingEntity[0]);
                    for (int i = 0; i < count && i < entities.length; ++i) {
                        String cmd = usercmd;
                        LivingEntity e = entities[i];
                        if ((power.isMustsee() && !player.hasLineOfSight(e))
                                || (!power.isSelfapplication() && e == player)
                                || (forPlayers && !(e instanceof Player))
                                || (forMobs && e instanceof Player)) {
                            ++count;
                            continue;
                        }
                        cmd = CommandHit.handleEntityPlaceHolder(e, cmd);
                        Bukkit.getServer().dispatchCommand(player, cmd);
                    }
                }
            } finally {
                if (power.getPermission().equals("*")) player.setOp(wasOp);
            }

            return PowerResult.ok();
        }

        @Override
        public Class<? extends AOECommand> getPowerClass() {
            return AOECommand.class;
        }

        @Override
        public PowerResult<Void> leftClick(
                AOECommand power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Double> hit(
                AOECommand power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                EntityDamageByEntityEvent event) {
            return fire(power, player, stack).with(damage);
        }

        @Override
        public PowerResult<Double> hitEntity(
                AOECommand power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                BeamHitEntityEvent event) {
            int r = power.getR();
            List<LivingEntity> nearbyEntities =
                    getNearbyEntities(power, player.getLocation(), player, r).stream()
                            .filter(entity1 -> entity1 instanceof LivingEntity)
                            .map(entity1 -> entity)
                            .collect(Collectors.toList());

            return fire(power, player, stack, nearbyEntities).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(
                AOECommand power,
                Player player,
                ItemStack stack,
                Location location,
                BeamHitBlockEvent event) {
            int r = power.getR();
            List<LivingEntity> nearbyEntities =
                    getNearbyEntities(power, location, player, r).stream()
                            .filter(entity -> entity instanceof LivingEntity)
                            .map(entity -> ((LivingEntity) entity))
                            .collect(Collectors.toList());

            return fire(power, player, stack, nearbyEntities);
        }

        @Override
        public PowerResult<Void> beamEnd(
                AOECommand power,
                Player player,
                ItemStack stack,
                Location location,
                BeamEndEvent event) {
            int r = power.getR();
            List<LivingEntity> nearbyEntities =
                    getNearbyEntities(power, location, player, r).stream()
                            .filter(entity -> entity instanceof LivingEntity)
                            .map(entity -> ((LivingEntity) entity))
                            .collect(Collectors.toList());
            return fire(power, player, stack, nearbyEntities);
        }

        @Override
        public PowerResult<Void> projectileHit(
                AOECommand power, Player player, ItemStack stack, ProjectileHitEvent event) {
            int r = power.getR();
            List<LivingEntity> nearbyEntities =
                    getNearbyEntities(power, event.getEntity().getLocation(), player, r).stream()
                            .filter(entity -> entity instanceof LivingEntity)
                            .map(entity -> ((LivingEntity) entity))
                            .collect(Collectors.toList());

            return fire(power, player, stack, nearbyEntities);
        }

        @Override
        public PowerResult<Void> sneak(
                AOECommand power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(
                AOECommand power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                @Nullable Double value) {
            List<LivingEntity> nearbyEntities =
                    getNearestLivingEntities(
                            power, entity.getLocation(), player, power.getR(), power.getRm());
            List<LivingEntity> livingEntitiesInCone =
                    getLivingEntitiesInCone(
                            nearbyEntities,
                            entity.getEyeLocation().toVector(),
                            power.getFacing(),
                            entity.getEyeLocation().getDirection());
            return fire(power, player, stack, livingEntitiesInCone);
        }
    }
}
