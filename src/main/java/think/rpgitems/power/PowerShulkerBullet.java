package think.rpgitems.power;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import think.rpgitems.power.types.PowerRightClick;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Power shulker bullet.
 * <p>
 * Launches shulker bullet when right clicked.
 * Target nearest entity
 * </p>
 */
public class PowerShulkerBullet extends Power implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    public long cooldownTime = 20;
    /**
     * Cost of this power
     */
    public int consumption = 0;
    /**
     * Range of this power
     */
    public double range = 10;

    private Class<? extends Projectile> projectileType = Snowball.class;

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldownTime");
        range = s.getDouble("range");
        consumption = s.getInt("consumption", 1);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldownTime", cooldownTime);
        s.set("consumption", consumption);
        s.set("range", range);
    }

    @Override
    public String getName() {
        return "shulkerbullet";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.shulkerbullet"), (double) cooldownTime / 20d);
    }

    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true)) return;
        if (!checkCooldown(player, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        ShulkerBullet bullet = player.launchProjectile(ShulkerBullet.class);
        List<LivingEntity> entities = getEntitiesInCone(getNearestLivingEntities(player.getEyeLocation(), player, range, 0), player.getLocation().toVector(), 30, player.getLocation().getDirection());
        if(!entities.isEmpty()){
            bullet.setTarget(entities.get(0));
        }
    }
}
