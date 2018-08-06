package think.rpgitems.power.impl;

import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerRightClick;

import java.util.List;

import static think.rpgitems.utils.PowerUtils.*;

/**
 * + * Power shulker bullet.
 * + * <p>
 * + * Launches shulker bullet when right clicked.
 * + * Target nearest entity
 * + * </p>
 * +
 */
public class PowerShulkerBullet extends BasePower implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldownTime = 20;
    /**
     * Cost of this power
     */
    public int consumption = 0;
    /**
     * Range of this power
     */
    @Property(order = 1)
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
        if (!getItem().checkPermission(player, true)) return;
        if (!checkCooldown(this, player, cooldownTime, true)) return;
        if (!getItem().consumeDurability(stack, consumption)) return;
        ShulkerBullet bullet = null;
        bullet = player.launchProjectile(ShulkerBullet.class);
        bullet.setPersistent(false);
        Events.rpgProjectiles.put(bullet.getEntityId(), getItem().getID());
        List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(this, player.getEyeLocation(), player, range, 0), player.getLocation().toVector(), 30, player.getLocation().getDirection());
        if (!entities.isEmpty()) {
            bullet.setTarget(entities.get(0));
        }
    }
}
