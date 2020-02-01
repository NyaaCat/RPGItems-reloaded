package think.rpgitems.event;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class BeamHitBlockEvent extends Event {
    public static HandlerList handlerList = new HandlerList();
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
    public static HandlerList getHandlerList(){
        return handlerList;
    }

    private final Entity from;
    private final Block hitBlock;
    private Location location;
    private final ItemStack itemStack;

    public BeamHitBlockEvent(Entity from, Block hitBlock, Location location, ItemStack itemStack){
        this.from = from;
        this.hitBlock = hitBlock;
        this.location = location;
        this.itemStack = itemStack;
    }

    public Location getLocation() {
        return location;
    }

    public Entity getFrom() {
        return from;
    }

    public Block getHitBlock() {
        return hitBlock;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
