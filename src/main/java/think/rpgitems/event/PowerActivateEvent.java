package think.rpgitems.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;

import java.util.HashMap;

public class PowerActivateEvent extends Event implements Cancellable {
    public static final HandlerList handlerList = new HandlerList();
    private final Player player;
    private boolean cancelled;
    private final ItemStack itemStack;
    private final Power power;
    private HashMap<String,Object> args = null;
    public PowerActivateEvent(Player player, ItemStack itemStack, Power power) {
        this.player = player;
        this.itemStack = itemStack;
        this.power = power;
    }
    public PowerActivateEvent(Player player, ItemStack itemStack, Power power, HashMap<String,Object> args) {
        this.player = player;
        this.itemStack = itemStack;
        this.power = power;
        this.args = args;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() { return handlerList; }

    public Player getPlayer() {
        return player;
    }

    public Power getPower() {
        return power;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public HashMap<String, Object> getArgs(){return args;}

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
