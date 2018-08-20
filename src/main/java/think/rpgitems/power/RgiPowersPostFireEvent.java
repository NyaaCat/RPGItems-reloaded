package think.rpgitems.power;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.RPGItem;

import java.util.Map;
import java.util.Map;
import java.util.SortedMap;

public class RgiPowersPostFireEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final ItemStack itemStack;
    private final RPGItem rpgItem;
    private final Player player;
    private final TriggerType triggerType;
    private final Map<Power, PowerResult> powerResults;

    public RgiPowersPostFireEvent(ItemStack itemStack, RPGItem rpgItem, Player player, TriggerType triggerType, Map<Power, PowerResult> powerResults) {
        this.itemStack = itemStack;
        this.rpgItem = rpgItem;
        this.player = player;
        this.triggerType = triggerType;
        this.powerResults = powerResults;
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

    public Map<Power, PowerResult> getPowerResults() {
        return powerResults;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
