package think.rpgitems.power.impl;

import static think.rpgitems.power.Utils.attachPermission;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;

/**
 * Power commandhit.
 *
 * <p>The item will run {@link #getCommand()} when player hits some {@link LivingEntity} giving the
 * permission {@link #getPermission()} just for the use of the command.
 */
@SuppressWarnings("WeakerAccess")
@Meta(
    defaultTrigger = "HIT",
    generalInterface = PowerLivingEntity.class,
    implClass = CommandHit.Impl.class)
public class CommandHit extends Command {

  @Property public double minDamage = 0;

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

  /** Minimum damage to trigger */
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

  public static class Impl
      implements PowerHit<CommandHit>, PowerLivingEntity<CommandHit>, PowerBeamHit<CommandHit> {
    @Override
    public PowerResult<Double> hit(
        CommandHit power,
        Player player,
        ItemStack stack,
        LivingEntity entity,
        double damage,
        EntityDamageByEntityEvent event) {
      return fire(power, player, stack, entity, damage).with(damage);
    }

    @Override
    public PowerResult<Void> fire(
        CommandHit power, Player player, ItemStack stack, LivingEntity entity, Double damage) {
      if (damage == null || damage < power.getMinDamage()) return PowerResult.noop();

      return executeCommand(power, player, entity, damage);
    }

    @Override
    public Class<? extends CommandHit> getPowerClass() {
      return CommandHit.class;
    }

    /**
     * Execute command
     *
     * @param player player
     * @param e entity
     * @param damage damage
     * @return PowerResult with proposed damage
     */
    protected PowerResult<Void> executeCommand(
        CommandHit power, Player player, LivingEntity e, double damage) {
      if (!player.isOnline()) return PowerResult.noop();

      attachPermission(player, power.getPermission());
      boolean wasOp = player.isOp();
      try {
        if (power.getPermission().equals("*")) player.setOp(true);

        String cmd = power.getCommand();

        cmd = handleEntityPlaceHolder(e, cmd);

        cmd = handlePlayerPlaceHolder(player, cmd);

        cmd = cmd.replaceAll("\\{damage}", String.valueOf(damage));

        boolean result = player.performCommand(cmd);
        return result ? PowerResult.ok() : PowerResult.fail();
      } finally {
        if (power.getPermission().equals("*")) player.setOp(wasOp);
      }
    }

    @Override
    public PowerResult<Double> hitEntity(
        CommandHit power,
        Player player,
        ItemStack stack,
        LivingEntity entity,
        double damage,
        BeamHitEntityEvent event) {
      return fire(power, player, stack, entity, damage).with(damage);
    }

    @Override
    public PowerResult<Void> hitBlock(
        CommandHit power,
        Player player,
        ItemStack stack,
        Location location,
        BeamHitBlockEvent event) {
      return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> beamEnd(
        CommandHit power, Player player, ItemStack stack, Location location, BeamEndEvent event) {
      return PowerResult.noop();
    }
  }
}
