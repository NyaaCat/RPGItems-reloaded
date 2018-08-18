package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;
import think.rpgitems.commands.Validator;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

import java.util.Collections;
import java.util.List;

import static think.rpgitems.utils.PowerUtils.checkCooldown;
import static think.rpgitems.utils.PowerUtils.getNearbyEntities;

/**
 * Power attract.
 * <p>
 * Pull around mobs in {@link #radius radius} to player.
 * Moving the mobs with max speed of {@link #maxSpeed maxSpeed}
 * </p>
 */
@SuppressWarnings("WeakerAccess")
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
    @Validator(value = "checkDuLeCd", message = "power.properties.attract.main_duration")
    public int duration = 5;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;
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
    @Validator(value = "checkCdGeDu", message = "power.properties.attract.main_duration")
    public long cooldown = 20;

    /**
     * Whether allow attracting player
     */
    @Property
    public boolean attractPlayer;

    public boolean checkCdGeDu(String value) {
        try {
            return Long.parseUnsignedLong(value) >= duration;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean checkDuLeCd(String value) {
        try {
            return Long.parseUnsignedLong(value) <= cooldown;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getName() {
        return "attract";
    }

    @Override
    public String displayText() {
        return I18n.format("power.attract");
    }

    @Override
    public void init(ConfigurationSection section){
        triggers = Collections.singleton(TriggerType.LEFT_CLICK);
        super.init(section);
    }

    @Override
    public PowerResult<Void> tick(Player player, ItemStack stack) {
        if (triggers.contains(TriggerType.TICK)) return attract(player, stack);
        return PowerResult.noop();
    }

    private PowerResult<Void> attract(Player player, ItemStack stack) {
        if (!player.isOnline() || player.isDead()) {
            return PowerResult.noop();
        }
        if (!triggers.contains(TriggerType.TICK) && !stack.equals(player.getInventory().getItemInMainHand())) {
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
    public PowerResult<Void> leftClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!item.consumeDurability(stack, consumption)) return PowerResult.cost();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(RPGItem.getPlugin(), () -> attract(player, stack), 0, duration);
        return PowerResult.ok();
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!item.consumeDurability(stack, consumption)) return PowerResult.cost();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(RPGItem.getPlugin(), () -> attract(player, stack), 0, duration);
        return PowerResult.ok();
    }
}
