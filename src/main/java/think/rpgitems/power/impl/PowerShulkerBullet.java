package think.rpgitems.power.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerRightClick;
import think.rpgitems.power.Property;

import java.util.List;

import static think.rpgitems.power.PowerResult.ok;
import static think.rpgitems.power.Utils.*;

/**
 * Power shulker bullet.
 * <p>
 * Launches shulker bullet when right clicked.
 * Target nearest entity
 * </p>
 */
@PowerMeta(immutableTrigger = true, withSelectors = true)
public class PowerShulkerBullet extends BasePower implements PowerRightClick {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;
    /**
     * Range of target finding
     */
    @Property(order = 1)
    public double range = 10;

    @Override
    public void init(ConfigurationSection s) {
        cooldown = s.getLong("cooldownTime");
        super.init(s);
    }

    @Override
    public String getName() {
        return "shulkerbullet";
    }

    @Override
    public String displayText() {
        return I18n.format("power.shulkerbullet", (double) cooldown / 20d);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        ShulkerBullet bullet = player.launchProjectile(ShulkerBullet.class);
        bullet.setPersistent(false);
        Events.registerProjectile(bullet.getEntityId(), getItem().getUid());
        List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(this, player.getEyeLocation(), player, range, 0), player.getLocation().toVector(), 30, player.getLocation().getDirection());
        if (!entities.isEmpty()) {
            bullet.setTarget(entities.get(0));
        }
        return ok();
    }
}
