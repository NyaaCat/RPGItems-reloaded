package think.rpgitems.power;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.trigger.Trigger;

public class RPGItemsPowersPostFireEvent<
                TEvent extends Event,
                TPower extends Power,
                TPimpl extends Pimpl<TPower>,
                TResult,
                TReturn>
        extends Event {

    private static final HandlerList handlers = new HandlerList();

    @NotNull
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
    private final Trigger<TEvent, TPower, TPimpl, TResult, TReturn> trigger;
    private final Map<PropertyHolder, PowerResult<?>> results;
    private final TReturn ret;

    public RPGItemsPowersPostFireEvent(
            Player player,
            ItemStack itemStack,
            TEvent event,
            RPGItem rpgItem,
            Trigger<TEvent, TPower, TPimpl, TResult, TReturn> trigger,
            Map<PropertyHolder, PowerResult<?>> powerResults,
            TReturn ret) {
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

    public Trigger<TEvent, TPower, TPimpl, TResult, TReturn> getTrigger() {
        return trigger;
    }

    public Map<PropertyHolder, PowerResult<?>> getResults() {
        return results;
    }

    public TEvent getEvent() {
        return event;
    }

    public TReturn getRet() {
        return ret;
    }
}
