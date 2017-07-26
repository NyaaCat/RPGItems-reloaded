package think.rpgitems.power;

import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.AcceptedValue;
import think.rpgitems.commands.Property;
import think.rpgitems.commands.Setter;
import think.rpgitems.commands.Validator;
import think.rpgitems.power.types.PowerRightClick;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Power projectile.
 * <p>
 * Launches projectile of type {@link #projectileType} with {@link #gravity} when right clicked.
 * If use {@link #cone} mode, {@link #amount} of projectiles will randomly distributed in the cone
 * with angle {@link #range} centered with player's direction.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public class PowerProjectile extends Power implements PowerRightClick {
    /**
     * Z_axis.
     */
    private static final Vector z_axis = new Vector(0, 0, 1);
    /**
     * X_axis.
     */
    private static final Vector x_axis = new Vector(1, 0, 0);
    /**
     * Y_axis.
     */
    private static final Vector y_axis = new Vector(0, 1, 0);
    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldownTime = 20;
    /**
     * Whether launch projectiles in cone
     */
    @Property(order = 1)
    public boolean cone = false;
    /**
     * Whether the projectile have gravity
     */
    public boolean gravity = true;
    /**
     * Range will projectiles spread, in degree
     */
    @Property(order = 3)
    public int range = 15;
    /**
     * Amount of projectiles
     */
    @Property(order = 4)
    public int amount = 5;
    /**
     * Speed of projectiles
     */
    @Property(order = 5)
    public double speed = 1;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 1;
    /**
     * Type of projectiles
     */
    @AcceptedValue({
                           "skull",
                           "fireball",
                           "snowball",
                           "smallfireball",
                           "llamaspit",
                           "arrow"
    })
    @Validator(value = "acceptableType", message = "power.projectile.noFireball")
    @Setter("setType")
    @Property(order = 2, required = true)
    public Class<? extends Projectile> projectileType = Snowball.class;

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldownTime");
        setType(s.getString("projectileType"));
        cone = s.getBoolean("isCone");
        range = s.getInt("range");
        amount = s.getInt("amount");
        consumption = s.getInt("consumption", 1);
        speed = s.getDouble("speed", 1);
        gravity = s.getBoolean("gravity", true);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldownTime", cooldownTime);
        s.set("projectileType", getType());
        s.set("isCone", cone);
        s.set("range", range);
        s.set("amount", amount);
        s.set("consumption", consumption);
        s.set("speed", speed);
        s.set("gravity", gravity);
    }

    /**
     * Gets type name
     *
     * @return Type name
     */
    public String getType() {
        if (projectileType == WitherSkull.class)
            return "skull";
        else if (projectileType == Fireball.class)
            return "fireball";
        else if (projectileType == SmallFireball.class)
            return "smallfireball";
        else if (projectileType == Arrow.class)
            return "arrow";
        else if (projectileType == LlamaSpit.class)
            return "llamaspit";
        else
            return "snowball";
    }

    /**
     * Sets type from type name
     *
     * @param type Type name
     */
    public void setType(String type) {
        switch (type) {
            case "skull":
                projectileType = WitherSkull.class;
                break;
            case "fireball":
                projectileType = Fireball.class;
                break;
            case "smallfireball":
                projectileType = SmallFireball.class;
                break;
            case "arrow":
                projectileType = Arrow.class;
                break;
            case "llamaspit":
                projectileType = LlamaSpit.class;
                break;
            default:
                projectileType = Snowball.class;
                break;
        }
    }

    /**
     * Check if the type is acceptable
     *
     * @param str Type name
     * @return If acceptable
     */
    public boolean acceptableType(String str) {
        return !(cone && str.equalsIgnoreCase("fireball"));
    }

    @Override
    public String getName() {
        return "projectile";
    }

    @Override
    public String displayText() {
        return I18n.format(cone ? "power.projectile.cone" : "power.projectile.display", getType(), (double) cooldownTime / 20d);
    }

    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true)) return;
        if (!checkCooldown(player, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        if (!cone) {
            Projectile projectile = player.launchProjectile(projectileType, player.getEyeLocation().getDirection().multiply(speed));
            Events.rpgProjectiles.put(projectile.getEntityId(), item.getID());
            projectile.setGravity(gravity);
            if (projectileType == Arrow.class)
                Events.removeArrows.add(projectile.getEntityId());
            if (!gravity) {
                (new BukkitRunnable() {
                    @Override
                    public void run() {
                        projectile.remove();
                    }
                }).runTaskLater(RPGItems.plugin, 80);
            }
        } else {
            Vector loc = player.getEyeLocation().getDirection();
            range = Math.abs(range) % 360;
            double phi = range / 180f * Math.PI;
            Vector a, b;
            Vector ax1 = loc.getCrossProduct(z_axis);
            if (ax1.length() < 0.01) {
                a = x_axis.clone();
                b = y_axis.clone();
            } else {
                a = ax1.normalize();
                b = loc.getCrossProduct(a).normalize();
            }
            for (int i = 0; i < amount; i++) {
                double z = range == 0 ? 1 : ThreadLocalRandom.current().nextDouble(Math.cos(phi), 1);
                double det = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
                double theta = Math.acos(z);
                Vector v = a.clone().multiply(Math.cos(det)).add(b.clone().multiply(Math.sin(det))).multiply(Math.sin(theta)).add(loc.clone().multiply(Math.cos(theta)));
                Projectile projectile = player.launchProjectile(projectileType, v.normalize().multiply(speed));
                Events.rpgProjectiles.put(projectile.getEntityId(), item.getID());
                projectile.setGravity(gravity);
                if (projectileType == Arrow.class)
                    Events.removeArrows.add(projectile.getEntityId());
                if (!gravity) {
                    (new BukkitRunnable() {
                        @Override
                        public void run() {
                            projectile.remove();
                        }
                    }).runTaskLater(RPGItems.plugin, 80);
                }
            }
        }
    }

}
