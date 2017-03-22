package think.rpgitems.power;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerTick;

/**
 * Power attract.
 * <p>
 * Pull around mobs in {@link #radius radius} to player when held in hand.
 * Moving the mobs with max speed of {@link #maxSpeed maxSpeed}
 * </p>
 */
public class PowerAttract extends Power implements PowerTick {
    /**
     * Maximum radius
     */
    public int radius = 5;
    /**
     * Maximum speed.
     */
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
        return ChatColor.RED + Locale.get("power.attract");
    }

    @Override
    public void tick(Player player, ItemStack item) {
        if (this.item.getHasPermission() && !player.hasPermission(this.item.getPermission())) return;
        double factor = Math.sqrt(radius - 1) / maxSpeed;
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
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
