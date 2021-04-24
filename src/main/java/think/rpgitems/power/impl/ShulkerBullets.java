package think.rpgitems.power.impl;

import static think.rpgitems.power.PowerResult.ok;
import static think.rpgitems.power.Utils.*;

import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.data.Context;
import think.rpgitems.power.*;

/**
 * Power shulker bullet.
 *
 * <p>Launches shulker bullet when right clicked. Target nearest entity
 */
@Meta(
        defaultTrigger = "RIGHT_CLICK",
        withSelectors = true,
        generalInterface = PowerPlain.class,
        implClass = ShulkerBullets.Impl.class)
public class ShulkerBullets extends BasePower {

    @Property(order = 1)
    public double range = 10;

    @Override
    public void init(ConfigurationSection s) {
        super.init(s);
    }

    @Override
    public String getName() {
        return "shulkerbullet";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.shulkerbullet", (double) 0 / 20d);
    }

    /** Range of target finding */
    public double getRange() {
        return range;
    }

    public static class Impl
            implements PowerRightClick<ShulkerBullets>,
                    PowerLeftClick<ShulkerBullets>,
                    PowerSneak<ShulkerBullets>,
                    PowerSprint<ShulkerBullets>,
                    PowerPlain<ShulkerBullets>,
                    PowerBowShoot<ShulkerBullets> {
        @Override
        public PowerResult<Void> leftClick(
                ShulkerBullets power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        @SuppressWarnings("deprecation")
        public PowerResult<Void> fire(ShulkerBullets power, Player player, ItemStack stack) {
            Events.registerRPGProjectile(power.getItem(), stack, player);
            org.bukkit.entity.ShulkerBullet bullet =
                    player.launchProjectile(
                            org.bukkit.entity.ShulkerBullet.class,
                            player.getEyeLocation().getDirection());
            bullet.setPersistent(false);
            List<LivingEntity> entities =
                    getLivingEntitiesInCone(
                            getNearestLivingEntities(
                                    power, player.getEyeLocation(), player, power.getRange(), 0),
                            player.getLocation().toVector(),
                            30,
                            player.getLocation().getDirection());
            if (!entities.isEmpty()) {
                Context.instance()
                        .putExpiringSeconds(
                                player.getUniqueId(), "shulkerbullet.target", entities.get(0), 3);
                bullet.setTarget(entities.get(0));
            }
            return ok();
        }

        @Override
        public Class<? extends ShulkerBullets> getPowerClass() {
            return ShulkerBullets.class;
        }

        @Override
        public PowerResult<Void> rightClick(
                ShulkerBullets power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sneak(
                ShulkerBullets power,
                Player player,
                ItemStack stack,
                PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sprint(
                ShulkerBullets power,
                Player player,
                ItemStack stack,
                PlayerToggleSprintEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(
                ShulkerBullets power, Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(power, player, itemStack).with(e.getForce());
        }
    }
}
