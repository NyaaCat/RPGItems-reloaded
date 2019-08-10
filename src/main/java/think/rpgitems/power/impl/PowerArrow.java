package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power arrow.
 * <p>
 * The arrow power will fire an arrow on right click.
 * </p>
 */
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = PowerArrow.Impl.class)
public class PowerArrow extends BasePower {

    @Property(order = 0)
    private long cooldown = 0;
    @Property
    private int cost = 0;

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    @Override
    public String getName() {
        return "arrow";
    }

    @Override
    public String displayText() {
        return I18n.format("power.arrow", (double) getCooldown() / 20d);
    }

    /**
     * Cooldown time of this power
     */
    public long getCooldown() {
        return cooldown;
    }

    public class Impl implements PowerRightClick, PowerLeftClick, PowerPlain {

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @SuppressWarnings("deprecation")
        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
            Events.registerRPGProjectile(getPower().getItem(), stack, player);
            Arrow arrow = player.launchProjectile(Arrow.class);
            arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            Events.autoRemoveProjectile(arrow.getEntityId());
            arrow.setPersistent(false);
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return PowerArrow.this;
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }
    }
}
