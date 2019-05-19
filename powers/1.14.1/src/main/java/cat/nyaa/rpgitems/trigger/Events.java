package cat.nyaa.rpgitems.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootCrossbowEvent;
import think.rpgitems.RPGItems;
import think.rpgitems.trigger.TriggerListener;

@TriggerListener
public class Events implements Listener {
    public Events() {
        RPGItems.logger.info("Loading 1.14 event listener");
    }

    @EventHandler
    public void onPlayerLoadCrossbow(EntityShootCrossbowEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            RPGItems.logger.warning(player.getDisplayName());
            RPGItems.logger.warning(event.toString());
            event.setCancelled(true);
        }
    }
}
