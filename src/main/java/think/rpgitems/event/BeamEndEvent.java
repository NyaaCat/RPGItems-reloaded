package think.rpgitems.event;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class BeamEndEvent extends Event {
    public static final HandlerList handlerList = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
    public static HandlerList getHandlerList(){
        return handlerList;
    }

    private final Entity fromEntity;
    private final Location location;
    private final ItemStack itemStack;

    public BeamEndEvent(Entity fromEntity, Location location, ItemStack itemStack) {
        this.fromEntity = fromEntity;
        this.location = location;
        this.itemStack = itemStack;
    }

    public Entity getFromEntity() {
        return fromEntity;
    }

    public Location getLocation() {
        return location;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
