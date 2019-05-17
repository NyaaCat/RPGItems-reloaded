package cat.nyaa.rpgitems.power.impl;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.AdminHandler;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import java.util.Optional;

/**
 * Power particle.
 * <p>
 * When right clicked, spawn some particles around the user.
 * </p>
 */
@PowerMeta(immutableTrigger = true, generalInterface = PowerPlain.class)
public class PowerParticle extends BasePower implements PowerRightClick, PowerLeftClick, PowerPlain {
    /**
     * Name of particle effect
     */
    @Property(order = 0, required = true)
    @Serializer(EffectSetter.class)
    @Deserializer(value = EffectSetter.class, message = "message.error.visualeffect")
    @AcceptedValue(preset = Preset.VISUAL_EFFECT)
    public Effect effect = Effect.MOBSPAWNER_FLAMES;

    @Property
    @Serializer(ParticleSetter.class)
    @Deserializer(value = ParticleSetter.class)
    public Particle particle = null;

    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    @Property
    public Material material;

    @Property
    public int dustColor = 0;

    @Property
    public double dustSize = 0;

    @Property
    public int particleCount = 1;

    @Property
    public double offsetX = 0;

    @Property
    public double offsetY = 0;

    @Property
    public double offsetZ = 0;

    @Property
    public double extra = 1;

    @Property
    public boolean force = false;

    private Object data = null;

    @Override
    public String getName() {
        return "particle";
    }

    @Override
    public String displayText() {
        return I18n.format("power.particle");
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    void spawnParticle(Player player) {
        if (particle == null) {
            if (effect == Effect.SMOKE) {
                player.getWorld().playEffect(player.getLocation().add(0, 2, 0), effect, 4);
            } else {
                player.getWorld().playEffect(player.getLocation(), effect, 0);
            }
        } else {
            player.getWorld().spawnParticle(particle, player.getLocation(), particleCount, offsetX, offsetY, offsetZ, extra, getData(), force);
        }
    }

    private Object getData() {
        if (data != null || particle.getDataType().equals(Void.class)) {
            return data;
        } else if (particle.getDataType().equals(ItemStack.class) && material != null && material.isItem()) {
            data = new ItemStack(material);
        } else if (particle.getDataType().equals(BlockData.class) && material != null && material.isBlock()) {
            data = material.createBlockData();
        } else if (particle.getDataType().equals(Particle.DustOptions.class)) {
            data = new Particle.DustOptions(Color.fromRGB(dustColor), (float) dustSize);
        }
        return data;
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        spawnParticle(player);
        return PowerResult.ok();
    }
    public class EffectSetter implements Getter<Effect>, Setter<Effect> {

        @Override
        public String get(Effect effect) {
            return effect.name();
        }

        @Override
        public Optional<Effect> set(String value) {
            PowerParticle.this.data = null;
            try {
                Effect eff = Effect.valueOf(value.toUpperCase());
                if (eff.getType() == Effect.Type.VISUAL) {
                    PowerParticle.this.effect = eff;
                    PowerParticle.this.particle = null;
                    return Optional.empty();
                }
                throw new AdminHandler.CommandException("message.error.visualeffect", value);
            } catch (IllegalArgumentException e) {
                DeprecatedEffect particleEffect = DeprecatedEffect.valueOf(value);
                PowerParticle.this.effect = null;
                PowerParticle.this.particle = particleEffect.getParticle();
                return Optional.empty();
            }
        }
    }

    public class ParticleSetter implements Getter<Particle>, Setter<Particle> {
        @Override
        public String get(Particle object) {
            return object.name();
        }

        @Override
        public Optional<Particle> set(String value) {
            PowerParticle.this.data = null;
            PowerParticle.this.effect = null;
            PowerParticle.this.particle = Particle.valueOf(value);
            return Optional.empty();
        }
    }
}
