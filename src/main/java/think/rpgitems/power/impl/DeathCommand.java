package think.rpgitems.power.impl;

import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

/**
 * Power deathcommand.
 *
 * <p>With a 1/{@link #chance chance} to kill the target then execute the {@link #command} for
 * {@link #count} times. {@link #desc Description} will be displayed in item lore. `${x}` `${y}` and
 * `${z}` in the command will be replaced with the death location of the enemy.
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "HIT", implClass = DeathCommand.Impl.class)
public class DeathCommand extends BasePower {

  private static final Random rand = new Random();

  @Property(order = 1, required = true)
  public String command = "";

  @Property(order = 0)
  public int chance = 20;

  @Property(order = 3)
  public String desc = "";

  @Property(order = 2)
  public int count = 1;

  /** Command to be executed */
  public String getCommand() {
    return command;
  }

  /** Times to run the {@link #command} */
  public int getCount() {
    return count;
  }

  @Override
  public String getName() {
    return "deathcommand";
  }

  @Override
  public String displayText() {
    return I18n.formatDefault(
        "power.deathcommand",
        getChance(),
        getDesc().equals("") ? "execute some command" : getDesc());
  }

  /** Chance of triggering this power */
  public int getChance() {
    return chance;
  }

  /** Description in display text */
  public String getDesc() {
    return desc;
  }

  public static Random getRand() {
    return rand;
  }

  public static class Impl implements PowerHit<DeathCommand> {
    @Override
    public PowerResult<Double> hit(
        DeathCommand power,
        Player player,
        ItemStack stack,
        LivingEntity entity,
        double damage,
        EntityDamageByEntityEvent event) {
      if (getRand().nextInt(power.getChance()) == 0) {
        Location loc = entity.getLocation();
        int x = (int) loc.getX();
        int y = (int) loc.getY();
        int z = (int) loc.getZ();
        entity.setHealth(0);
        String cmd =
            power
                .getCommand()
                .replace("${x}", String.valueOf(x))
                .replace("${y}", String.valueOf(y))
                .replace("${z}", String.valueOf(z));
        for (int i = 0; i < power.getCount(); i++)
          Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        return PowerResult.ok(damage);
      }
      return PowerResult.noop();
    }

    @Override
    public Class<? extends DeathCommand> getPowerClass() {
      return DeathCommand.class;
    }
  }
}
