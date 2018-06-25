package think.rpgitems.power;

import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.power.types.PowerRightClick;
import think.rpgitems.utils.ReflectionUtil;

import java.util.List;

/**
 * + * Power shulker bullet.
 * + * <p>
 * + * Launches shulker bullet when right clicked.
 * + * Target nearest entity
 * + * </p>
 * +
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
        return I18n.format("power.shulkerbullet", (double) cooldownTime / 20d);
    }

    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true)) return;
        if (!checkCooldown(player, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        ShulkerBullet bullet = null;
        if (ReflectionUtil.getVersion().startsWith("v1_11_")) {
            bullet = player.getWorld().spawn(player.getEyeLocation(), ShulkerBullet.class);
            bullet.setShooter(player);
        } else {
            bullet = player.launchProjectile(ShulkerBullet.class);
        }
        Events.rpgProjectiles.put(bullet.getEntityId(), item.getID());
        List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(player.getEyeLocation(), player, range, 0), player.getLocation().toVector(), 30, player.getLocation().getDirection());
        if (!entities.isEmpty()) {
            bullet.setTarget(entities.get(0));
        }
    }
}
