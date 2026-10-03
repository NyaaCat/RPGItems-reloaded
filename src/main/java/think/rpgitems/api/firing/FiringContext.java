package think.rpgitems.api.firing;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;

import java.util.Objects;

/**
 * What a power knows about itself at the moment it is activated. Passed explicitly to
 * {@link FiringLocationProvider#bind(FiringContext)}; RPGItems keeps no reference to it afterwards.
 *
 * @param player the player the power is firing for (item owner, cooldown holder, damage owner)
 * @param source the entity the power was told to fire from. This is {@code player} for ordinary triggers and
 *               the entity handed to {@link think.rpgitems.power.PowerLivingEntity#fire} otherwise
 * @param item   the item stack carrying the power
 * @param power  the power being fired
 */
public record FiringContext(Player player, LivingEntity source, ItemStack item, Power power) {
    public FiringContext {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(source, "source");
    }
}
