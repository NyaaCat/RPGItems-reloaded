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

    private final ItemStack itemStack;
    private final RPGItem rpgItem;
    private final Player player;
    private final TriggerType triggerType;
    private final List<? extends Power> powers;

    public RgiPowersPreFireEvent(ItemStack itemStack, RPGItem rpgItem, Player player, TriggerType triggerType, List<? extends Power> powers) {
        this.itemStack = itemStack;
        this.rpgItem = rpgItem;
        this.player = player;
        this.triggerType = triggerType;
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

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public List<? extends Power> getPowers() {
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
}
