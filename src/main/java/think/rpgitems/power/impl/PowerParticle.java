package think.rpgitems.power.impl;

import org.bukkit.Effect;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

/**
 * Power particle.
 * <p>
 * When right clicked, spawn some particles around the user.
 * </p>
 */
@PowerMeta(immutableTrigger = true)
public class PowerParticle extends BasePower implements PowerRightClick {
    /**
     * Name of particle effect
     */
    @Property(order = 0, required = true)
    @Serializer(EffectSetter.class)
    @Deserializer(value = EffectSetter.class, message = "message.error.visualeffect")
    @AcceptedValue(preset = Preset.VISUAL_EFFECT)
    public Effect effect = Effect.MOBSPAWNER_FLAMES;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    @Override
    public String getName() {
        return "particle";
    }

    @Override
    public String displayText() {
        return I18n.format("power.particle");
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        if (effect == Effect.SMOKE) {
            player.getWorld().playEffect(player.getLocation().add(0, 2, 0), effect, 4);
        } else {
            player.getWorld().playEffect(player.getLocation(), effect, 0);
        }
        return PowerResult.ok();
    }

    public static class EffectSetter implements Getter<Effect>, Setter<Effect> {

        @Override
        public String get(Effect effect) {
            return effect.toString();
        }

        @Override
        public Effect set(String value) {
            Effect eff = Effect.valueOf(value.toUpperCase());
            if(eff.getType() == Effect.Type.VISUAL){
                return eff;
            }
            throw new IllegalArgumentException();
        }
    }
}
