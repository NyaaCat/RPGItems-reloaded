package think.rpgitems.power.impl;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import javax.annotation.Nullable;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power rumble.
 * <p>
 * The rumble power sends a shockwave through the ground
 * and sends any hit entities flying with power {@link #power}.
 * The wave will travel {@link #distance} blocks.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class, implClass = Rumble.Impl.class)
public class Rumble extends BasePower {

    @Property(order = 0)
    public int cooldown = 0;
    @Property(order = 1)
    public int power = 2;
    @Property(order = 2, required = true)
    public int distance = 15;
    @Property
    public int cost = 0;

    @Property
    public double damage = 0;

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    public double getDamage() {
        return damage;
    }

    /**
     * Maximum distance of rumble
     */
    public int getDistance() {
        return distance;
    }

    @Override
    public String getName() {
        return "rumble";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.rumble", (double) getCooldown() / 20d);
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Power of rumble
     */
    public int getPower() {
        return power;
    }

    public class Impl implements PowerRightClick, PowerLeftClick, PowerSneak, PowerSprint, PowerPlain, PowerBowShoot, PowerLivingEntity {
        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(final Player player, ItemStack stack) {
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower());
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            final Location location = player.getLocation().add(0, -0.2, 0);
            final Vector direction = player.getLocation().getDirection();
            return fire(player, location, direction);
        }

        private PowerResult<Void> fire(Player player, Location location, Vector direction) {
            RumbleManager.getInstance().register(
                    new ActiveRumble(player, location, direction, Rumble.this)
            );
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return Rumble.this;
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
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
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, @Nullable Double value) {
            return fire(player, entity.getLocation(), entity.getLocation().getDirection());
        }
    }

}
