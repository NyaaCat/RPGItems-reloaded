package think.rpgitems.event;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class BeamHitBlockEvent extends Event {
    public static final HandlerList handlerList = new HandlerList();
    private final Player player;
    private final Entity from;
    private final Block hitBlock;
    private final ItemStack itemStack;
    private final Location location;
    private final int depth;
    public BeamHitBlockEvent(Player player, Entity from, Block hitBlock, Location location, ItemStack itemStack, int depth) {
        this.player = player;
        this.from = from;
        this.hitBlock = hitBlock;
        this.location = location;
        this.itemStack = itemStack;
        this.depth = depth;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public Location getLocation() {
        return location;
    }

    public Entity getFrom() {
        return from;
    }

    public Player getPlayer() {
        return player;
    }

    public Block getHitBlock() {
        return hitBlock;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getDepth() {
        return depth;
    }
}
