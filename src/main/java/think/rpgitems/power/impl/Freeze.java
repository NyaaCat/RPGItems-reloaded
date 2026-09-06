package think.rpgitems.power.impl;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

/**
 * Power freeze.
 * <p>
 * The freeze power will set the target's freeze time to {@link #ticks} ticks.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "HIT", generalInterface = {PowerLivingEntity.class, PowerHit.class, PowerBeamHit.class, PowerTick.class}, implClass = Freeze.Impl.class)
public class Freeze extends BasePower {

    @Property(order = 0)
    public int ticks = 20;
    @Property
    public int cost = 0;
    @Property
    public boolean lockTime = false;
    @Property
    public boolean additive = false;

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    public boolean isAdditive() {
        return additive;
    }

    @Override
    public String getName() {
        return "freeze";
    }

    @Override
    public String displayText() {
        if (!additive) {
            if (lockTime) {
                return I18n.formatDefault("power.freeze.locked.set", (double) getTicks() / 20d);
            }
            return I18n.formatDefault("power.freeze.default.set", (double) getTicks() / 20d);
        } else {
            if (lockTime) {
                return I18n.formatDefault("power.freeze.locked.additive", (double) getTicks() / 20d);
            }
            return I18n.formatDefault("power.freeze.default.additive", (double) getTicks() / 20d);
        }
    }

    public int getTicks() {
        return ticks;
    }

    public boolean isLockTime() {
        return lockTime;
    }

    public class Impl implements PowerHit, PowerLivingEntity, PowerProjectileHit, PowerBeamHit, PowerTick {

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, Double value) {
            PowerActivateEvent powerEvent = new PowerActivateEvent(player, stack, getPower());
            if (!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            entity.setFreezeTicks(isAdditive() ? entity.getFreezeTicks() + getTicks() : getTicks());
            entity.lockFreezeTicks(isLockTime());
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return Freeze.this;
        }

        @Override
        public PowerResult<Double> hitEntity(Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            return null;
        }

        @Override
        public PowerResult<Void> beamEnd(Player player, ItemStack stack, Location location, BeamEndEvent event) {
            return null;
        }

        @Override
        public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
            if (event.getHitEntity() instanceof LivingEntity livingEntity) {
                return fire(player, stack, livingEntity, 0.0);
            }
            return PowerResult.fail();
        }

        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            return fire(player, stack, player, 0.0);
        }
    }
}
