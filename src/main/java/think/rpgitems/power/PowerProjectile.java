package think.rpgitems.power;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerRightClick;

import java.util.Random;

public class PowerProjectile extends Power implements PowerRightClick {
    public static final String name = "projectile";
    public long cooldownTime = 20;
    private Class<? extends Projectile> projectileType = Snowball.class;
    public boolean cone = false;
    public int range = 15;
    public int amount = 5;
    public int consumption = 0;
    private Random rand = new Random();

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldownTime");
        setType(s.getString("projectileType"));
        cone = s.getBoolean("isCone");
        range = s.getInt("range");
        amount = s.getInt("amount");
        consumption = s.getInt("consumption", 1);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldownTime", cooldownTime);
        s.set("projectileType", getType());
        s.set("isCone", cone);
        s.set("range", range);
        s.set("amount", amount);
        s.set("consumption", consumption);
    }

    public void setType(String type) {
        switch (type) {
            case "skull": projectileType = WitherSkull.class; break;
            case "fireball": projectileType = Fireball.class; break;
            default: projectileType = Snowball.class; break;
        }
    }

    public String getType() {
        if (projectileType == WitherSkull.class)
            return "skull";
        else if (projectileType == Fireball.class)
            return "fireball";
        else
            return "snowball";
    }

    public boolean acceptableType(String str) {
        return str.equals("skull") || str.equals("fireball") || str.equals("snowball");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get(cone?"power.projectile.cone":"power.projectile"), getType(), (double) cooldownTime / 20d);
    }

    @Override
    public void rightClick(Player player, ItemStack is, Block clicked) {
        long cooldown;
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false) {
        } else {
            RPGValue value = RPGValue.get(player, item, "projectile.cooldown");
            if (value == null) {
                cooldown = System.currentTimeMillis() / 50;
                value = new RPGValue(player, item, "projectile.cooldown", cooldown);
            } else {
                cooldown = value.asLong();
            }
            if (cooldown <= System.currentTimeMillis() / 50) {
                item.consumeDurability(is,1);
                value.set(System.currentTimeMillis() / 50 + cooldownTime);
                if(!cone) {
                    player.launchProjectile(projectileType);
                } else {
                    double PHI = -player.getLocation().getPitch();
                    double THETA = -player.getLocation().getYaw();
                    for (int i = 0; i < amount; i++) {
                        double phi = (PHI + (rand.nextDouble() - 0.5) * 2 * range) / 180 * Math.PI;
                        double theta = (THETA + (rand.nextDouble() - 0.5) * 2 * range) / 180 * Math.PI;
                        player.launchProjectile(projectileType, new Vector(Math.cos(phi) * Math.sin(theta), Math.sin(phi), Math.cos(phi) * Math.cos(theta)));
                    }
                }
            } else {
                player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown"), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
            }
        }
    }

}
