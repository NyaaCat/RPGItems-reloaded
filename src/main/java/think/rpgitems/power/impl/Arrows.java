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
 *
 * <p>The arrow power will fire an arrow on right click.
 */
@Meta(
        defaultTrigger = "RIGHT_CLICK",
        generalInterface = PowerPlain.class,
        implClass = Arrows.Impl.class)
public class Arrows extends BasePower {

    @Override
    public String getName() {
        return "arrow";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.arrow", (double) 0);
    }

    public static class Impl
            implements PowerRightClick<Arrows>, PowerLeftClick<Arrows>, PowerPlain<Arrows> {

        @Override
        public PowerResult<Void> rightClick(
                Arrows power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(Arrows power, Player player, ItemStack stack) {
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
            Events.registerRPGProjectile(power.getItem(), stack, player);
            Arrow arrow = player.launchProjectile(org.bukkit.entity.Arrow.class);
            arrow.setPickupStatus(org.bukkit.entity.Arrow.PickupStatus.DISALLOWED);
            Events.autoRemoveProjectile(arrow.getEntityId());
            arrow.setPersistent(false);
            return PowerResult.ok();
        }

        @Override
        public Class<? extends Arrows> getPowerClass() {
            return Arrows.class;
        }

        @Override
        public PowerResult<Void> leftClick(
                Arrows power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }
    }
}
