package think.rpgitems.api.power;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Powers triggered on player left clicked on blocks or air
 */
public interface IPowerLeftClick extends IPower {
    /**
     * Involed when player left clicked on a block or in air
     *
     * @param player        the player
     * @param blockLocation the block player click on, null if clicked in air
     * @return power triggered or not
     */
    Boolean onLeftClick(Player player, Location blockLocation);
}
