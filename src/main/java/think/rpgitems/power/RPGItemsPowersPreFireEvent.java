package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.RPGItem;

import java.util.List;

public class RPGItemsPowersPreFireEvent<TEvent extends Event, TPower extends Pimpl, TResult, TReturn> extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancel;

    private final ItemStack itemStack;
    private final TEvent event;
    private final RPGItem rpgItem;
    private final Player player;
    private final Trigger<TEvent, TPower, TResult, TReturn> trigger;
    private final List<TPower> powers;

    public RPGItemsPowersPreFireEvent(Player player, ItemStack itemStack, TEvent event, RPGItem rpgItem, Trigger<TEvent, TPower, TResult, TReturn> trigger, List<TPower> powers) {
        this.itemStack = itemStack;
        this.event = event;
        this.rpgItem = rpgItem;
        this.player = player;
        this.trigger = trigger;
        this.powers = powers;
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

    public Trigger getTrigger() {
        return trigger;
    }

    public List<? extends Pimpl> getPowers() {
        return powers;
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

    public TEvent getEvent() {
        return event;
    }
}
