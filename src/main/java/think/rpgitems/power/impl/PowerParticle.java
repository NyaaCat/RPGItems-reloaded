package think.rpgitems.power.impl;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.Handler;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import java.util.Optional;

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
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        spawnParticle(player);
        return PowerResult.ok();
    }

    void spawnParticle(Player player) {
        if (particle == null) {
            if (effect == Effect.SMOKE) {
                player.getWorld().playEffect(player.getLocation().add(0, 2, 0), effect, 4);
            } else {
                player.getWorld().playEffect(player.getLocation(), effect, 0);
            }
        } else {
            player.getWorld().spawnParticle(particle, player.getLocation(), particleCount, getData());
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

    @SuppressWarnings("unused")
    public enum DeprecatedEffect {
        CLOUD(Particle.CLOUD),
        COLOURED_DUST(Particle.FALLING_DUST),
        CRIT(Particle.CRIT),
        EXPLOSION(Particle.EXPLOSION_NORMAL),
        EXPLOSION_HUGE(Particle.EXPLOSION_HUGE),
        EXPLOSION_LARGE(Particle.EXPLOSION_LARGE),
        FIREWORKS_SPARK(Particle.FIREWORKS_SPARK),
        FLAME(Particle.FLAME),
        FLYING_GLYPH(Particle.ENCHANTMENT_TABLE),
        FOOTSTEP(Particle.NAUTILUS), // TODO: FOOTSTEP is gone
        HAPPY_VILLAGER(Particle.VILLAGER_HAPPY),
        HEART(Particle.HEART),
        INSTANT_SPELL(Particle.SPELL_INSTANT),
        ITEM_BREAK(Particle.ITEM_CRACK),
        LARGE_SMOKE(Particle.SMOKE_LARGE),
        LAVA_POP(Particle.LAVA),
        LAVADRIP(Particle.DRIP_LAVA),
        MAGIC_CRIT(Particle.CRIT_MAGIC),
        NOTE(Particle.NOTE),
        PARTICLE_SMOKE(Particle.SMOKE_NORMAL),
        PORTAL(Particle.PORTAL),
        POTION_SWIRL(Particle.SPELL_MOB),
        POTION_SWIRL_TRANSPARENT(Particle.SPELL_MOB_AMBIENT),
        SLIME(Particle.SLIME),
        SMALL_SMOKE(Particle.TOWN_AURA),
        SNOW_SHOVEL(Particle.SNOW_SHOVEL),
        SNOWBALL_BREAK(Particle.SNOWBALL),
        SPELL(Particle.SPELL),
        SPLASH(Particle.WATER_SPLASH),
        TILE_BREAK(Particle.BLOCK_CRACK),
        TILE_DUST(Particle.BLOCK_DUST),
        VILLAGER_THUNDERCLOUD(Particle.VILLAGER_ANGRY),
        VOID_FOG(Particle.SUSPENDED_DEPTH),
        WATERDRIP(Particle.DRIP_WATER),
        WITCH_MAGIC(Particle.SPELL_WITCH),
        ;

        private final Particle particle;

        DeprecatedEffect(Particle particle) {
            this.particle = particle;
        }

        public Particle getParticle() {
            return particle;
        }
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
                throw new Handler.CommandException("message.error.visualeffect", value);
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
