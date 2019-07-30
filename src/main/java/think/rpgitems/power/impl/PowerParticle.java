package think.rpgitems.power.impl;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.AdminHandler;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import java.util.Optional;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power particle.
 * <p>
 * When right clicked, spawn some particles around the user.
 * </p>
 */
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = PowerParticle.Impl.class)
public class PowerParticle extends BasePower {
    @Property(order = 0, required = true)
    @Serializer(EffectSetter.class)
    @Deserializer(value = EffectSetter.class, message = "message.error.visualeffect")
    @AcceptedValue(preset = Preset.VISUAL_EFFECT)
    private Effect effect = Effect.MOBSPAWNER_FLAMES;

    @Property
    @Serializer(ParticleSetter.class)
    @Deserializer(value = ParticleSetter.class)
    private Particle particle = null;

    @Property
    private int cost = 0;

    @Property
    private long cooldown = 0;

    @Property
    private Material material;

    @Property
    private int dustColor = 0;

    @Property
    private double dustSize = 0;

    @Property
    private int particleCount = 1;

    @Property
    private double offsetX = 0;

    @Property
    private double offsetY = 0;

    @Property
    private double offsetZ = 0;

    @Property
    private double extra = 1;

    @Property
    private boolean force = false;

    @Property
    private boolean requireHurtByEntity = true;

    public class Impl implements PowerRightClick, PowerLeftClick, PowerPlain, PowerHit, PowerHitTaken, PowerHurt, PowerBowShoot {

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack);
            }
            return PowerResult.noop();
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
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            spawnParticle(player);
            return PowerResult.ok();
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            spawnParticle(entity);
            return PowerResult.ok().with(damage);
        }

        @Override
        public Power getPower() {
            return PowerParticle.this;
        }
    }

    private Object getData() {
        if (data != null || getParticle().getDataType().equals(Void.class)) {
            return data;
        } else if (getParticle().getDataType().equals(ItemStack.class) && getMaterial() != null && getMaterial().isItem()) {
            data = new ItemStack(getMaterial());
        } else if (getParticle().getDataType().equals(BlockData.class) && getMaterial() != null && getMaterial().isBlock()) {
            data = getMaterial().createBlockData();
        } else if (getParticle().getDataType().equals(Particle.DustOptions.class)) {
            data = new Particle.DustOptions(Color.fromRGB(getDustColor()), (float) getDustSize());
        }
        return data;
    }

    void spawnParticle(Entity player) {
        if (getParticle() == null) {
            if (getEffect() == Effect.SMOKE) {
                player.getWorld().playEffect(player.getLocation().add(0, 2, 0), getEffect(), 4);
            } else {
                player.getWorld().playEffect(player.getLocation(), getEffect(), 0);
            }
        } else {
            player.getWorld().spawnParticle(getParticle(), player.getLocation(), getParticleCount(), getOffsetX(), getOffsetY(), getOffsetZ(), getExtra(), getData(), isForce());
        }
    }

    private Object data = null;

    /**
     * Cooldown time of this power
     */
    public long getCooldown() {
        return cooldown;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    public int getDustColor() {
        return dustColor;
    }

    public double getDustSize() {
        return dustSize;
    }

    /**
     * Name of particle effect
     */
    public Effect getEffect() {
        return effect;
    }

    public double getExtra() {
        return extra;
    }

    public Material getMaterial() {
        return material;
    }

    @Override
    public String getName() {
        return "particle";
    }

    @Override
    public String displayText() {
        return I18n.format("power.particle");
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public Particle getParticle() {
        return particle;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public boolean isForce() {
        return force;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
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
