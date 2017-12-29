package think.rpgitems.power.types;

import org.bukkit.entity.Player;
import think.rpgitems.commands.Property;

public interface PowerDelayable extends Power{
    @Property(order = 4)
    public int delay = 0;

    default void TriggerLater(Power power,Player player){};

}
