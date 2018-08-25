package think.rpgitems.power.impl;

import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.*;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerTick;

import static think.rpgitems.utils.PowerUtils.checkCooldown;

/**
 * Power particletick.
 * <p>
 * When item held in hand, spawn some particles around the user.
 * With the time {@link #interval} given in ticks.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(immutableTrigger = true)
public class PowerParticleTick extends BasePower implements PowerTick {
    /**
     * Name of particle effect
     */
    @Property(order = 0, required = true)
    @Serializer(PowerParticle.EffectSetter.class)
    @Deserializer(value = PowerParticle.EffectSetter.class, message = "message.error.visualeffect")
    @AcceptedValue(preset = Preset.VISUAL_EFFECT)
    public Effect effect = Effect.MOBSPAWNER_FLAMES;
    /**
     * Interval of particle effect
     */
    @Property(order = 1)
    public int interval = 15;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

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
        if (!checkCooldown(this, player, interval, false)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        if (effect == Effect.SMOKE) {
            player.getWorld().playEffect(player.getLocation().add(0, 2, 0), effect, 4);
        } else {
            player.getWorld().playEffect(player.getLocation(), effect, 0);
        }
        return PowerResult.ok();
    }
}
