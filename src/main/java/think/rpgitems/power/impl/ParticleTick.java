package think.rpgitems.power.impl;

import java.util.Collections;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.BaseTriggers;
import think.rpgitems.power.trigger.Trigger;

/**
 * Power particletick.
 *
 * <p>When item held in hand, spawn some particles around the user. With the time {@link #interval}
 * given in ticks.
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "TICK", implClass = ParticleTick.Impl.class)
public class ParticleTick extends Particles {
  @Property(order = 1)
  public int interval = 15;

  /** Interval of particle effect */
  public int getInterval() {
    return interval;
  }

  @Override
  public String getName() {
    return "particletick";
  }

  @Override
  public String displayText() {
    return I18n.formatDefault("power.particletick");
  }

  @Override
  public Set<Trigger> getTriggers() {
    return Collections.singleton(BaseTriggers.TICK);
  }

  public static class Impl
      implements PowerTick<ParticleTick>, PowerSneaking<ParticleTick>, PowerPlain<ParticleTick> {

    @Override
    public PowerResult<Void> tick(ParticleTick power, Player player, ItemStack stack) {
      return fire(power, player, stack);
    }

    @Override
    public PowerResult<Void> fire(ParticleTick power, Player player, ItemStack stack) {
      power.spawnParticle(player);
      return PowerResult.ok();
    }

    @Override
    public Class<? extends ParticleTick> getPowerClass() {
      return ParticleTick.class;
    }

    @Override
    public PowerResult<Void> sneaking(ParticleTick power, Player player, ItemStack stack) {
      return fire(power, player, stack);
    }
  }
}
