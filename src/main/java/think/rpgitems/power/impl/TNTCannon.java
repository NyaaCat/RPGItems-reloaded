package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import javax.annotation.Nullable;

/**
 * Power tntcannon.
 * <p>
 * The tntcannon power will fire active tnt on right click.
 * </p>
 */
@Meta(immutableTrigger = true, implClass = TNTCannon.Impl.class)
public class TNTCannon extends BasePower {

    @Override
    public String getName() {
        return "tntcannon";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.tntcannon", (0) / 20d);
    }

    public class Impl implements PowerRightClick, PowerLivingEntity {
        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
            TNTPrimed tnt = player.getWorld().spawn(player.getLocation().add(0, 1.8, 0), TNTPrimed.class);
            tnt.setVelocity(player.getLocation().getDirection().multiply(2d));
            return PowerResult.ok();
        }

        @Override
        public Power getPowerClass() {
            return TNTCannon.this;
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, @Nullable Double value) {
            player.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
            TNTPrimed tnt = player.getWorld().spawn(entity.getLocation().add(0, 1.8, 0), TNTPrimed.class);
            tnt.setVelocity(entity.getLocation().getDirection().multiply(2d));
            return PowerResult.ok();        }
    }
}
