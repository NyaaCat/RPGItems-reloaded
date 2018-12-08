package think.rpgitems.power.impl;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import java.util.Collections;
import java.util.Set;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power particletick.
 * <p>
 * When item held in hand, spawn some particles around the user.
 * With the time {@link #interval} given in ticks.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(immutableTrigger = true)
public class PowerParticleTick extends PowerParticle implements PowerTick {
    /**
     * Interval of particle effect
     */
    @Property(order = 1)
    public int interval = 15;

    @Override
    public String getName() {
        return "particletick";
    }

    @Override
    public String displayText() {
        return I18n.format("power.particletick");
    }

    @Override
    public PowerResult<Void> tick(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, interval, false, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        spawnParticle(player);
        return PowerResult.ok();
    }

    @Override
    public Set<Trigger> getTriggers() {
        return Collections.singleton(Trigger.TICK);
    }
}
