package think.rpgitems.power;

import javax.annotation.CheckReturnValue;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;

public interface PowerBeamHit<P extends Power> extends Pimpl<P> {
  @CheckReturnValue
  default PowerResult<Double> hitEntity(
      P power,
      Player player,
      ItemStack stack,
      LivingEntity entity,
      double damage,
      BeamHitEntityEvent event) {
    return PowerResult.fail();
  }

  @CheckReturnValue
  PowerResult<Void> hitBlock(
      P power, Player player, ItemStack stack, Location location, BeamHitBlockEvent event);

  @CheckReturnValue
  PowerResult<Void> beamEnd(
      P power, Player player, ItemStack stack, Location location, BeamEndEvent event);
}
