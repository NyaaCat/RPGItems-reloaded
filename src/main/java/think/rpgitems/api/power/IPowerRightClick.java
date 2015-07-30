package think.rpgitems.api.power;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Powers when player right clicked
 */
public interface IPowerRightClick extends IPower {
    /**
     * Invoked when player right clicked.
     *
     * @param player    the player
     * @param clickedOn the entity player clicked on, null if click in air or on block
     * @return power triggered or not
     */
    Boolean onRightClick(Player player, Entity clickedOn);
}
