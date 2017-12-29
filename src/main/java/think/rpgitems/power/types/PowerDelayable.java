package think.rpgitems.power.types;

import org.bukkit.entity.Player;

public interface PowerDelayable extends IPower {

    default void triggerLater(IPower IPower, Player player){};

}
