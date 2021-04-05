package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

/**
 * Power arrow.
 * <p>
 * The arrow power will fire an arrow on right click.
 * </p>
 */
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = Arrows.Impl.class)
public class Arrows extends BasePower {

    @Override
    public String getName() {
        return "arrow";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.arrow", (double) 0);
    }

    public class Impl implements PowerRightClick, PowerLeftClick, PowerPlain {

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @SuppressWarnings("deprecation")
        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
            Events.registerRPGProjectile(getPower().getItem(), stack, player);
            Arrow arrow = player.launchProjectile(org.bukkit.entity.Arrow.class);
            arrow.setPickupStatus(org.bukkit.entity.Arrow.PickupStatus.DISALLOWED);
            Events.autoRemoveProjectile(arrow.getEntityId());
            arrow.setPersistent(false);
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return Arrows.this;
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }
    }
}
