package think.rpgitems.event;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class BeamEndEvent extends Event {
  public static final HandlerList handlerList = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlerList;
  }

  public static HandlerList getHandlerList() {
    return handlerList;
  }

  private final Player player;
  private final Entity fromEntity;
  private final Location location;
  private final ItemStack itemStack;
  private int depth;

  public BeamEndEvent(
      Player player, Entity fromEntity, Location location, ItemStack itemStack, int depth) {
    this.player = player;
    this.fromEntity = fromEntity;
    this.location = location;
    this.itemStack = itemStack;
    this.depth = depth;
  }

  public Player getPlayer() {
    return player;
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

  public int getDepth() {
    return depth;
  }
}
