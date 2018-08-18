package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.RPGItem;

import java.util.List;
import java.util.Map;

public class RgiPowersPostFireEvent extends Event{

    private static final HandlerList handlers = new HandlerList();

    public Player getPlayer() {
        return null;
    }

    public ItemStack getItemStack() {
        return null;
    }

    public RPGItem getItem() {
        return null;
    }

    public TriggerType getTriggerType() {
        return null;
    }

    public Map<Power, PowerResult> getPowerResults() {
        return null;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
