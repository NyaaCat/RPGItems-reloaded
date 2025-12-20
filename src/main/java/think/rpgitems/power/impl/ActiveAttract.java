package think.rpgitems.power.impl;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.Supplier;

/**
 * Represents an active attract effect managed by {@link AttractManager}.
 */
public class ActiveAttract implements Tickable {
    private final Player player;
    private final Location location;
    private final ItemStack stack;
    private final Supplier<List<Entity>> entitySupplier;
    private final Attract power;
    private int remainingDuration;

    public ActiveAttract(Player player, Location location, ItemStack stack,
                         Supplier<List<Entity>> entitySupplier, Attract power, int duration) {
        this.player = player;
        this.location = location;
        this.stack = stack;
        this.entitySupplier = entitySupplier;
        this.power = power;
        this.remainingDuration = duration;
    }

    @Override
    public boolean tick() {
        if (--remainingDuration <= 0) {
            return false;
        }

        if (!player.isOnline() || player.isDead()) {
            return false;
        }

        double factor = Math.sqrt(power.getRadius() - 1.0) / power.getMaxSpeed();
        List<Entity> entities = entitySupplier.get();
        if (entities.isEmpty()) {
            return true;
        }

        if (!power.getItem().consumeDurability(stack, power.getAttractingTickCost())) {
            return true;
        }

        for (Entity e : entities) {
            if (e instanceof LivingEntity
                    && !e.hasMetadata("NPC")
                    && (power.isAttractPlayer() || !(e instanceof Player))) {
                if (!power.getItem().consumeDurability(stack, power.getAttractingEntityTickCost())) {
                    break;
                }
                Location locTarget = e.getLocation();
                double d = locTarget.distance(location);
                if (d < 1 || d > power.getRadius()) {
                    continue;
                }
                double newVelocity = Math.sqrt(d - 1) / factor;
                if (Double.isInfinite(newVelocity)) {
                    newVelocity = 0;
                }
                Vector direction = location.clone().subtract(locTarget).toVector().normalize();
                e.setVelocity(direction.multiply(newVelocity));
            }
        }
        return true;
    }
}
