package think.rpgitems.power.impl;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.BaseTriggers;
import think.rpgitems.power.trigger.Trigger;

import java.util.Collections;
import java.util.Set;

/**
 * Power particletick.
 * <p>
 * When item held in hand, spawn some particles around the user.
 * With the time {@link #interval} given in ticks.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "TICK", implClass = ParticleTick.Impl.class)
public class ParticleTick extends ParticlePower {
    @Property(order = 1)
    public int interval = 15;

    /**
     * Interval of particle effect
     */
    public int getInterval() {
        return interval;
    }

    @Override
    public String getName() {
        return "particletick";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.particletick");
    }

    @Override
    public Set<Trigger> getTriggers() {
        return Collections.singleton(BaseTriggers.TICK);
    }

    public class Impl implements PowerTick, PowerSneaking, PowerPlain {

        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            spawnParticle(player);
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return ParticleTick.this;
        }

        @Override
        public PowerResult<Void> sneaking(Player player, ItemStack stack) {
            return fire(player, stack);
        }
    }
}
