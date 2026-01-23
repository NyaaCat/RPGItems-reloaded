package think.rpgitems.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired during RPGItems reload, AFTER PowerManager.clear() and BEFORE ItemManager.reload().
 * Extension plugins should listen to this event and re-register their powers.
 */
public class RPGItemsReloadEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
