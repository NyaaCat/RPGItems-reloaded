package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.RPGItem;

import java.util.List;

public class RgiPowersPreFireEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancel;

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

    public List<Power> getPowers() {
        return null;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }
}
