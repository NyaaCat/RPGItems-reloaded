package think.rpgitems.power.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.data.Context;
import think.rpgitems.power.*;

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
@PowerMeta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class, implClass = PowerShulkerBullet.Impl.class)
public class PowerShulkerBullet extends BasePower {

    @Property(order = 0)
    private long cooldown = 0;
    @Property
    private int cost = 0;
    @Property(order = 1)
    private double range = 10;

    @Override
    public void init(ConfigurationSection s) {
        cooldown = s.getLong("cooldownTime");
        super.init(s);
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    @Override
    public String getName() {
        return "shulkerbullet";
    }

    @Override
    public String displayText() {
        return I18n.format("power.shulkerbullet", (double) getCooldown() / 20d);
    }

    /**
     * Cooldown time of this power
     */
    public long getCooldown() {
        return cooldown;
    }

    /**
     * Range of target finding
     */
    public double getRange() {
        return range;
    }

    public class Impl implements PowerRightClick, PowerLeftClick, PowerSneak, PowerSprint, PowerPlain, PowerBowShoot {
        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        @SuppressWarnings("deprecation")
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            Events.registerRPGProjectile(getItem(), stack, player);
            ShulkerBullet bullet = player.launchProjectile(ShulkerBullet.class, player.getEyeLocation().getDirection());
            bullet.setPersistent(false);
            List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(getPower(), player.getEyeLocation(), player, getRange(), 0), player.getLocation().toVector(), 30, player.getLocation().getDirection());
            if (!entities.isEmpty()) {
                Context.instance().putExpiringSeconds(player.getUniqueId(), "shulkerbullet.target", entities.get(0), 3);
                bullet.setTarget(entities.get(0));
            }
            return ok();
        }

        @Override
        public Power getPower() {
            return PowerShulkerBullet.this;
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(player, itemStack).with(e.getForce());
        }
    }
}
