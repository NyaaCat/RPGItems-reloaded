package think.rpgitems.power.impl;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Represents an active dash effect managed by {@link DashManager}.
 * Maintains the player's velocity for a specified duration.
 *
 * Optimizations:
 * - Stores velocity as primitives (no Vector clone in constructor)
 * - Reuses single Vector object for setVelocity calls (zero allocation per tick)
 */
public class ActiveDash implements Tickable {
    private final Player player;
    private final Vector velocity;  // Reused each tick
    private int remainingTicks;
    boolean markedForRemoval;

    public ActiveDash(Player player, Vector velocity, int durationTicks) {
        this.player = player;
        // Store our own Vector with same values - reused each tick
        this.velocity = new Vector(velocity.getX(), velocity.getY(), velocity.getZ());
        this.remainingTicks = durationTicks;
        this.markedForRemoval = false;
    }

    @Override
    public boolean tick() {
        if (markedForRemoval || !player.isOnline() || player.isDead()) {
            return false;
        }

        if (--remainingTicks <= 0) {
            return false;
        }

        player.setVelocity(velocity);
        return true;
    }

    public Player getPlayer() {
        return player;
    }
}
