package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.commands.AcceptedValue;
import think.rpgitems.commands.Property;
import think.rpgitems.commands.Validator;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.PowerLeftClick;
import think.rpgitems.power.PowerRightClick;
import think.rpgitems.power.PowerTick;
import think.rpgitems.power.TriggerType;

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
    @Validator(value = "checkDuLeCd", message = "powers.attract.main_duration")
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

    @Property
    @AcceptedValue({"TICK", "LEFT_CLICK", "RIGHT_CLICK"})
    public TriggerType triggerType = TriggerType.TICK;

    /**
     * Cooldown time of this power
     */
    @Property
    @Validator(value = "checkCdGeDu", message = "powers.attract.main_duration")
    public long cooldownTime = 20;

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
            return Long.parseUnsignedLong(value) <= cooldownTime;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void init(ConfigurationSection s) {
        radius = s.getInt("radius");
        maxSpeed = s.getDouble("maxSpeed");
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("radius", radius);
        s.set("maxSpeed", maxSpeed);
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
    public void tick(Player player, ItemStack stack) {
        if (triggerType.equals(TriggerType.TICK)) attract(player, stack);
    }

    private void attract(Player player, ItemStack stack) {
        if (!player.isOnline() || player.isDead()) {
            return;
        }
        if (triggerType != TriggerType.TICK && !stack.equals(player.getInventory().getItemInMainHand())) {
            return;
        }
        if (!getItem().checkPermission(player, true)) return;
        double factor = Math.sqrt(radius - 1.0) / maxSpeed;
        List<Entity> entities = getNearbyEntities(this, player.getLocation(), player, radius);
        if (entities.isEmpty()) return;
        if (!item.consumeDurability(stack, attractingTickCost)) return;
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
    }

    @Override
    public void leftClick(Player player, ItemStack stack, Block clicked) {
        if (triggerType.equals(TriggerType.LEFT_CLICK)) {
            if (!checkCooldown(this, player, cooldownTime, true)) return;
            if (!item.consumeDurability(stack, consumption)) return;
            Bukkit.getScheduler().scheduleSyncRepeatingTask(RPGItem.getPlugin(), () -> attract(player, stack), 0, duration);
        }
    }

    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (triggerType.equals(TriggerType.RIGHT_CLICK)) {
            if (!checkCooldown(this, player, cooldownTime, true)) return;
            if (!item.consumeDurability(stack, consumption)) return;
            Bukkit.getScheduler().scheduleSyncRepeatingTask(RPGItem.getPlugin(), () -> attract(player, stack), 0, duration);
        }
    }
}
