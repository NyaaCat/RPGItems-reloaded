package think.rpgitems.power.impl;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

import java.util.List;

import static think.rpgitems.power.Utils.checkCooldown;
import static think.rpgitems.power.Utils.getNearbyEntities;

/**
 * Power attract.
 * <p>
 * Pull around mobs in {@link #radius radius} to player.
 * Moving the mobs with max speed of {@link #maxSpeed maxSpeed}
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = "RIGHT_CLICK", withSelectors = true)
public class PowerAttract extends BasePower implements PowerTick, PowerLeftClick, PowerRightClick {
    /**
     * Maximum radius
     */
    @Property(order = 0)
    public int radius = 5;
    /**
     * Maximum speed.
     */
    @Property(order = 1, required = true)
    public double maxSpeed = 0.4D;
    /**
     * Duration of this power when triggered by click in tick
     */
    @Property
    public int duration = 5;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;
    /**
     * Hooking Cost Pre-Tick
     */
    @Property
    public int attractingTickCost = 0;
    /**
     * Hooking Cost Pre-Entity-Tick
     */
    @Property
    public int attractingEntityTickCost = 0;

    /**
     * Cooldown time of this power
     */
    @Property
    public long cooldown = 20;

    /**
     * Whether allow attracting player
     */
    @Property
    public boolean attractPlayer;

    @Override
    public String getName() {
        return "attract";
    }

    @Override
    public String displayText() {
        return I18n.format("power.attract");
    }

    @Override
    public PowerResult<Void> tick(Player player, ItemStack stack) {
        return attract(player, stack);
    }

    private PowerResult<Void> attract(Player player, ItemStack stack) {
        if (!player.isOnline() || player.isDead()) {
            return PowerResult.noop();
        }
        if (!triggers.contains(Trigger.TICK) && !stack.equals(player.getInventory().getItemInMainHand())) {
            return PowerResult.noop();
        }
        double factor = Math.sqrt(radius - 1.0) / maxSpeed;
        List<Entity> entities = getNearbyEntities(this, player.getLocation(), player, radius);
        if (entities.isEmpty()) return null;
        if (!item.consumeDurability(stack, attractingTickCost)) return null;
        for (Entity e : entities) {
            if (e instanceof LivingEntity
                        && (attractPlayer || !(e instanceof Player))) {
                if (!item.consumeDurability(stack, attractingEntityTickCost)) break;
                Location locTarget = e.getLocation();
                Location locPlayer = player.getLocation();
                double d = locTarget.distance(locPlayer);
                if (d < 1 || d > radius) continue;
                double newVelocity = Math.sqrt(d - 1) / factor;
                Vector direction = locPlayer.subtract(locTarget).toVector().normalize();
                e.setVelocity(direction.multiply(newVelocity));
            }
        }
        return PowerResult.ok();
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!item.consumeDurability(stack, cost)) return PowerResult.cost();
        new BukkitRunnable() {
            int dur = duration;

            @Override
            public void run() {
                if (--dur <= 0) {
                    this.cancel();
                    return;
                }
                attract(player, stack);
            }
        }.runTaskTimer(RPGItem.getPlugin(), 0, 1);
        return PowerResult.ok();
    }
}
