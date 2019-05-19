package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.RPGItem;

import java.util.Map;

public class RPGItemsPowersPostFireEvent<TEvent extends Event, TPower extends Power, TResult, TReturn> extends Event {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    private final TEvent event;
    private final ItemStack itemStack;
    private final RPGItem rpgItem;
    private final Player player;
    private final Trigger<TEvent, TPower, TResult, TReturn> trigger;
    private final Map<Power, PowerResult> results;
    private final TReturn ret;

    public RPGItemsPowersPostFireEvent(Player player, ItemStack itemStack, TEvent event, RPGItem rpgItem, Trigger<TEvent, TPower, TResult, TReturn> trigger, Map<Power, PowerResult> powerResults, TReturn ret) {
        this.event = event;
        this.itemStack = itemStack;
        this.rpgItem = rpgItem;
        this.player = player;
        this.trigger = trigger;
        this.results = powerResults;
        this.ret = ret;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public RPGItem getItem() {
        return rpgItem;
    }

    public Trigger<TEvent, TPower, TResult, TReturn> getTrigger() {
        return trigger;
    }

    public Map<Power, PowerResult> getResults() {
        return results;
    }

    public TEvent getEvent() {
        return event;
    }

    public TReturn getRet() {
        return ret;
    }
}
