package think.rpgitems.power;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PowerLivingEntity<P extends Power> extends Pimpl<P> {

    /**
     * A trigger that fire a power with an entity and an double value (usually the damage)
     *
     * @param player Player
     * @param stack Item that triggered this power
     * @param entity Entity that involved in this trigger
     * @param value Value that involved in this trigger
     * @return PowerResult
     */
    @CheckReturnValue
    PowerResult<Void> fire(
            P power, Player player, ItemStack stack, LivingEntity entity, @Nullable Double value);
}
