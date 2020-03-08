package think.rpgitems.event;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import think.rpgitems.power.impl.Beam;

public class BeamHitEntityEvent extends Event {
    public static final HandlerList handlerList = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList(){
        return handlerList;
    }

    private final Entity from;
    private final LivingEntity entity;
    private ItemStack itemStack;
    private double damage;
    private final Location loc;
    private final BoundingBox boundingBox;
    private final Vector velocity;
    private int depth;

    public BeamHitEntityEvent(Entity from, LivingEntity entity, ItemStack itemStack, double damage, Location loc, BoundingBox boundingBox, Vector vector, int depth){
        this.from = from;
        this.entity = entity;
        this.itemStack = itemStack;
        this.damage = damage;
        this.loc = loc;
        this.boundingBox = boundingBox;
        this.velocity = vector;
        this.depth = depth;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public Vector getVelocity() {
        return velocity;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public Entity getFrom() {
        return from;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public double getDamage() {
        return damage;
    }

    public Location getLoc() {
        return loc;
    }

    public int getDepth() {
        return depth;
    }
}
