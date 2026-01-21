package think.rpgitems.power.impl;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Represents an active dash effect managed by {@link DashManager}.
 * Maintains the player's velocity for a specified duration.
 */
public class ActiveDash implements Tickable {
    private final Player player;
    private final Vector velocity;
    private int remainingTicks;

    /**
     * Creates a new active dash effect.
     *
     * @param player The player being dashed
     * @param velocity The velocity to maintain
     * @param durationTicks How many ticks to maintain the velocity
     */
    public ActiveDash(Player player, Vector velocity, int durationTicks) {
        this.player = player;
        this.velocity = velocity.clone();
        this.remainingTicks = durationTicks;
    }

    @Override
    public boolean tick() {
        if (!player.isOnline() || player.isDead()) {
            return false;
        }

        remainingTicks--;
        if (remainingTicks <= 0) {
            return false;
        }

        player.setVelocity(velocity);
        return true;
    }

    public Player getPlayer() {
        return player;
    }
}
