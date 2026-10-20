package think.rpgitems.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired during RPGItems reload, AFTER PowerManager.clear() and BEFORE ItemManager.reload().
 * Extension plugins should listen to this event and re-register their powers, conditions, markers,
 * description resolvers and power adapters ({@code PowerManager.registerAdapter}): the clear drops all of them.
 * Triggers stay registered.
 * <p>
 * Extensions loaded from {@code plugins/RPGItems/ext} stay enabled through a reload (Bukkit cannot unload or
 * re-load a plugin), so this event, not onEnable, is where they re-register. Only extension jars that were not
 * loaded before are loaded and enabled by a reload, before this event is fired.
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
