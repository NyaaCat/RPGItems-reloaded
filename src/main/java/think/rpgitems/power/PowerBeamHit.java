package think.rpgitems.power;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;

import javax.annotation.CheckReturnValue;

public interface PowerBeamHit extends Pimpl {
    @CheckReturnValue
    default PowerResult<Double> hitEntity(Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
        return PowerResult.fail();
    }

    @CheckReturnValue
    PowerResult<Void> hitBlock(Player player, ItemStack stack, Location location, BeamHitBlockEvent event);

    @CheckReturnValue
    PowerResult<Void> beamEnd(Player player, ItemStack stack, Location location, BeamEndEvent event);
}
