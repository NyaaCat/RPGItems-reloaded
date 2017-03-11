package think.rpgitems.power;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.Events;
import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerRightClick;

import java.util.concurrent.ThreadLocalRandom;

public class PowerProjectile extends Power implements PowerRightClick {
    public static final String name = "projectile";
    public static final Vector z_axis = new Vector(0, 0, 1);
    public static final Vector x_axis = new Vector(1, 0, 0);
    public static final Vector y_axis = new Vector(0, 1, 0);
    public long cooldownTime = 20;
    private Class<? extends Projectile> projectileType = Snowball.class;
    public boolean cone = false;
    public boolean gravity = true;
    public int range = 15;
    public int amount = 5;
    public int consumption = 1;

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldownTime");
        setType(s.getString("projectileType"));
        cone = s.getBoolean("isCone");
        range = s.getInt("range");
        amount = s.getInt("amount");
        consumption = s.getInt("consumption", 1);
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
        s.set("gravity", gravity);
    }

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

    public boolean acceptableType(String str) {
        return str.equals("skull") || str.equals("fireball") || str.equals("snowball") || str.equals("smallfireball") || str.equals("llamaspit") || str.equals("arrow");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get(cone ? "power.projectile.cone" : "power.projectile"), getType(), (double) cooldownTime / 20d);
    }

    @Override
    public void rightClick(Player player, ItemStack is, Block clicked) {
        long cooldown;
        if (item.getHasPermission() && !player.hasPermission(item.getPermission())) {
        } else {
            RPGValue value = RPGValue.get(player, item, "projectile.cooldown");
            if (value == null) {
                cooldown = System.currentTimeMillis() / 50;
                value = new RPGValue(player, item, "projectile.cooldown", cooldown);
            } else {
                cooldown = value.asLong();
            }
            if (cooldown <= System.currentTimeMillis() / 50) {
                if (!item.consumeDurability(is, consumption)) return;
                value.set(System.currentTimeMillis() / 50 + cooldownTime);
                if (!cone) {
                    Projectile projectile = player.launchProjectile(projectileType);
                    projectile.setGravity(gravity);
                    if (projectileType == Arrow.class)
                        Events.removeArrows.put(projectile.getEntityId(), (byte) 1);
                    if (!gravity) {
                        (new BukkitRunnable() {
                            @Override
                            public void run() {
                                projectile.remove();
                            }
                        }).runTaskLater(Plugin.plugin, 60);
                    }
                } else {
                    Vector loc = player.getEyeLocation().getDirection();
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
                        Projectile projectile = player.launchProjectile(projectileType, v.normalize());
                        projectile.setGravity(gravity);
                        if (projectileType == Arrow.class)
                            Events.removeArrows.put(projectile.getEntityId(), (byte) 1);
                        if (!gravity) {
                            (new BukkitRunnable() {
                                @Override
                                public void run() {
                                    projectile.remove();
                                }
                            }).runTaskLater(Plugin.plugin, 60);
                        }
                    }
                }
            } else {
                player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown"), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
            }
        }
    }

}
