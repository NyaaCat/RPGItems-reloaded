package think.rpgitems.power.types;

import org.bukkit.entity.Player;
import think.rpgitems.commands.Property;

public interface PowerDelayable extends Power{

    default void triggerLater(Power power,Player player){};

}
