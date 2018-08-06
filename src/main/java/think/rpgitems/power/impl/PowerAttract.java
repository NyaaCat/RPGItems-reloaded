package think.rpgitems.power.impl;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerTick;

import java.util.List;

import static think.rpgitems.utils.PowerUtils.getNearbyEntities;

/**
 * Power attract.
 * <p>
 * Pull around mobs in {@link #radius radius} to player when held in hand.
 * Moving the mobs with max speed of {@link #maxSpeed maxSpeed}
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public class PowerAttract extends BasePower implements PowerTick {
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
        if (!getItem().checkPermission(player, true)) return;
        double factor = Math.sqrt(radius - 1) / maxSpeed;
        List<Entity> entities = getNearbyEntities(this, player.getLocation(), player, radius);
        for (Entity e : entities) {
            if (e instanceof LivingEntity && !(e instanceof Player)) {
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
}
