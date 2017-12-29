package think.rpgitems.power.types;

import org.bukkit.entity.Player;

public interface PowerDelayable extends Power{
    void TriggerLater(Power power);
}
