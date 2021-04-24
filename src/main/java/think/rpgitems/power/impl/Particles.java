package think.rpgitems.power.impl;

import java.util.Optional;
import javax.annotation.Nullable;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.AdminCommands;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;
import think.rpgitems.utils.cast.CastUtils;

/**
 * Power particle.
 *
 * <p>When right clicked, spawn some particles around the user.
 */
@Meta(
        defaultTrigger = "RIGHT_CLICK",
        generalInterface = {
            PowerLeftClick.class,
            PowerRightClick.class,
            PowerPlain.class,
            PowerSneak.class,
            PowerLivingEntity.class,
            PowerSprint.class,
            PowerHurt.class,
            PowerHit.class,
            PowerHitTaken.class,
            PowerBowShoot.class,
            PowerBeamHit.class,
            PowerLocation.class
        },
        implClass = Particles.Impl.class)
public class Particles extends BasePower {
    @Property(order = 0, required = true)
    @Serializer(EffectSetter.class)
    @Deserializer(value = EffectSetter.class, message = "message.error.visualeffect")
    @AcceptedValue(preset = Preset.VISUAL_EFFECT)
    public Effect effect = Effect.MOBSPAWNER_FLAMES;

    @Property
    @Serializer(ParticleSetter.class)
    @Deserializer(value = ParticleSetter.class)
    public Particle particle = null;

    @Property public Material material;

    @Property public int dustColor = 0;

    @Property public double dustSize = 0;

    @Property public int particleCount = 1;

    @Property public double offsetX = 0;

    @Property public double offsetY = 0;

    @Property public double offsetZ = 0;

    @Property public double extra = 1;

    @Property public boolean force = false;

    @Property public PlayLocation playLocation = PlayLocation.HIT_LOCATION;

    @Property public int delay = 0;

    @Property public double firingRange = 20;

    public double getFiringRange() {
        return firingRange;
    }

    public int getDelay() {
        return delay;
    }

    @Property public boolean requireHurtByEntity = true;
    private Object data = null;

    void spawnParticle(Entity player) {
        if (getParticle() == null) {
            if (getEffect() == Effect.SMOKE) {
                player.getWorld().playEffect(player.getLocation().add(0, 2, 0), getEffect(), 4);
            } else {
                player.getWorld().playEffect(player.getLocation(), getEffect(), 0);
            }
        } else {
            player.getWorld()
                    .spawnParticle(
                            getParticle(),
                            player.getLocation(),
                            getParticleCount(),
                            getOffsetX(),
                            getOffsetY(),
                            getOffsetZ(),
                            getExtra(),
                            getData(),
                            isForce());
        }
    }

    void spawnParticle(World world, Location location) {
        if (getParticle() == null) {
            if (getEffect() == Effect.SMOKE) {
                world.playEffect(location.add(0, 2, 0), getEffect(), 4);
            } else {
                world.playEffect(location, getEffect(), 0);
            }
        } else {
            world.spawnParticle(
                    getParticle(),
                    location,
                    getParticleCount(),
                    getOffsetX(),
                    getOffsetY(),
                    getOffsetZ(),
                    getExtra(),
                    getData(),
                    isForce());
        }
    }

    public org.bukkit.Particle getParticle() {
        return particle;
    }

    /** Name of particle effect */
    public Effect getEffect() {
        return effect;
    }

    public int getParticleCount() {
        return particleCount;
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

    public double getExtra() {
        return extra;
    }

    private Object getData() {
        if (data != null || getParticle().getDataType().equals(Void.class)) {
            return data;
        } else if (getParticle().getDataType().equals(ItemStack.class)
                && getMaterial() != null
                && getMaterial().isItem()) {
            data = new ItemStack(getMaterial());
        } else if (getParticle().getDataType().equals(BlockData.class)
                && getMaterial() != null
                && getMaterial().isBlock()) {
            data = getMaterial().createBlockData();
        } else if (getParticle().getDataType().equals(org.bukkit.Particle.DustOptions.class)) {
            data =
                    new org.bukkit.Particle.DustOptions(
                            Color.fromRGB(getDustColor()), (float) getDustSize());
        }
        return data;
    }

    public boolean isForce() {
        return force;
    }

    public Material getMaterial() {
        return material;
    }

    public int getDustColor() {
        return dustColor;
    }

    public double getDustSize() {
        return dustSize;
    }

    public PlayLocation getPlayLocation() {
        return playLocation;
    }

    @Override
    public String getName() {
        return "particle";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.particle");
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    @SuppressWarnings("unused")
    public enum DeprecatedEffect {
        CLOUD(org.bukkit.Particle.CLOUD),
        COLOURED_DUST(org.bukkit.Particle.FALLING_DUST),
        CRIT(org.bukkit.Particle.CRIT),
        EXPLOSION(org.bukkit.Particle.EXPLOSION_NORMAL),
        EXPLOSION_HUGE(org.bukkit.Particle.EXPLOSION_HUGE),
        EXPLOSION_LARGE(org.bukkit.Particle.EXPLOSION_LARGE),
        FIREWORKS_SPARK(org.bukkit.Particle.FIREWORKS_SPARK),
        FLAME(org.bukkit.Particle.FLAME),
        FLYING_GLYPH(org.bukkit.Particle.ENCHANTMENT_TABLE),
        FOOTSTEP(org.bukkit.Particle.NAUTILUS), // TODO: FOOTSTEP is gone
        HAPPY_VILLAGER(org.bukkit.Particle.VILLAGER_HAPPY),
        HEART(org.bukkit.Particle.HEART),
        INSTANT_SPELL(org.bukkit.Particle.SPELL_INSTANT),
        ITEM_BREAK(org.bukkit.Particle.ITEM_CRACK),
        LARGE_SMOKE(org.bukkit.Particle.SMOKE_LARGE),
        LAVA_POP(org.bukkit.Particle.LAVA),
        LAVADRIP(org.bukkit.Particle.DRIP_LAVA),
        MAGIC_CRIT(org.bukkit.Particle.CRIT_MAGIC),
        NOTE(org.bukkit.Particle.NOTE),
        PARTICLE_SMOKE(org.bukkit.Particle.SMOKE_NORMAL),
        PORTAL(org.bukkit.Particle.PORTAL),
        POTION_SWIRL(org.bukkit.Particle.SPELL_MOB),
        POTION_SWIRL_TRANSPARENT(org.bukkit.Particle.SPELL_MOB_AMBIENT),
        SLIME(org.bukkit.Particle.SLIME),
        SMALL_SMOKE(org.bukkit.Particle.TOWN_AURA),
        SNOW_SHOVEL(org.bukkit.Particle.SNOW_SHOVEL),
        SNOWBALL_BREAK(org.bukkit.Particle.SNOWBALL),
        SPELL(org.bukkit.Particle.SPELL),
        SPLASH(org.bukkit.Particle.WATER_SPLASH),
        TILE_BREAK(org.bukkit.Particle.BLOCK_CRACK),
        TILE_DUST(org.bukkit.Particle.BLOCK_DUST),
        VILLAGER_THUNDERCLOUD(org.bukkit.Particle.VILLAGER_ANGRY),
        VOID_FOG(org.bukkit.Particle.SUSPENDED_DEPTH),
        WATERDRIP(org.bukkit.Particle.DRIP_WATER),
        WITCH_MAGIC(org.bukkit.Particle.SPELL_WITCH),
        ;

        private final org.bukkit.Particle particle;

        DeprecatedEffect(org.bukkit.Particle particle) {
            this.particle = particle;
        }

        public org.bukkit.Particle getParticle() {
            return particle;
        }
    }

    public static class Impl
            implements PowerRightClick<Particles>,
                    PowerLeftClick<Particles>,
                    PowerPlain<Particles>,
                    PowerHit<Particles>,
                    PowerHitTaken<Particles>,
                    PowerHurt<Particles>,
                    PowerBowShoot<Particles>,
                    PowerBeamHit<Particles>,
                    PowerProjectileHit<Particles>,
                    PowerLivingEntity<Particles>,
                    PowerLocation<Particles> {

        @Override
        public PowerResult<Double> takeHit(
                Particles power,
                Player target,
                ItemStack stack,
                double damage,
                EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Particles power, Player player, ItemStack stack) {
            Location playLocation = player.getLocation();
            PlayLocation playLocation1 = power.getPlayLocation();
            if (playLocation1.equals(PlayLocation.TARGET)) {
                CastUtils.CastLocation castLocation =
                        CastUtils.rayTrace(
                                player,
                                player.getEyeLocation(),
                                player.getEyeLocation().getDirection(),
                                power.getFiringRange());
                playLocation = castLocation.getTargetLocation();
            }

            return fire(power, playLocation);
        }

        private PowerResult<Void> fire(Particles power, Location playLocation) {
            int delay = power.getDelay();
            new BukkitRunnable() {
                @Override
                public void run() {
                    power.spawnParticle(playLocation.getWorld(), playLocation);
                }
            }.runTaskLater(RPGItems.plugin, delay);
            return PowerResult.ok();
        }

        @Override
        public Class<? extends Particles> getPowerClass() {
            return Particles.class;
        }

        @Override
        public PowerResult<Void> hurt(
                Particles power, Player target, ItemStack stack, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> rightClick(
                Particles power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(
                Particles power, Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(power, player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Void> leftClick(
                Particles power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Double> hit(
                Particles power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                EntityDamageByEntityEvent event) {
            if (power.getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
                int delay = power.getDelay();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        power.spawnParticle(entity);
                    }
                }.runTaskLater(RPGItems.plugin, delay);
            } else if (power.getPlayLocation().equals(PlayLocation.SELF)) {
                int delay = power.getDelay();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        power.spawnParticle(player);
                    }
                }.runTaskLater(RPGItems.plugin, delay);
            }
            return PowerResult.ok().with(damage);
        }

        @Override
        public PowerResult<Double> hitEntity(
                Particles power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                BeamHitEntityEvent event) {
            int delay = power.getDelay();
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location location = event.getLoc();
                    if (power.getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
                    } else if (power.getPlayLocation().equals(PlayLocation.SELF)) {
                        location = player.getLocation();
                    }
                    power.spawnParticle(entity.getWorld(), location);
                }
            }.runTaskLater(RPGItems.plugin, delay);
            return PowerResult.ok(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(
                Particles power,
                Player player,
                ItemStack stack,
                Location location,
                BeamHitBlockEvent event) {
            int delay = power.getDelay();
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location loc = location;
                    if (power.getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
                    } else if (power.getPlayLocation().equals(PlayLocation.SELF)) {
                        loc = player.getLocation();
                    }
                    power.spawnParticle(player.getWorld(), loc);
                }
            }.runTaskLater(RPGItems.plugin, delay);

            return PowerResult.ok();
        }

        @Override
        public PowerResult<Void> beamEnd(
                Particles power,
                Player player,
                ItemStack stack,
                Location location,
                BeamEndEvent event) {
            int delay = power.getDelay();
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location loc = location;
                    if (power.getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
                    } else if (power.getPlayLocation().equals(PlayLocation.SELF)) {
                        loc = player.getLocation();
                    }
                    power.spawnParticle(player.getWorld(), loc);
                }
            }.runTaskLater(RPGItems.plugin, delay);

            return PowerResult.ok();
        }

        @Override
        public PowerResult<Void> projectileHit(
                Particles power, Player player, ItemStack stack, ProjectileHitEvent event) {
            int delay = power.getDelay();
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location loc = event.getEntity().getLocation();
                    if (power.getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
                    } else if (power.getPlayLocation().equals(PlayLocation.SELF)) {
                        loc = player.getLocation();
                    }
                    power.spawnParticle(player.getWorld(), loc);
                }
            }.runTaskLater(RPGItems.plugin, delay);

            return PowerResult.ok();
        }

        @Override
        public PowerResult<Void> fire(
                Particles power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                @Nullable Double value) {
            Location location = player.getLocation();
            PlayLocation playLocation = power.getPlayLocation();
            switch (playLocation) {
                case SELF:
                    break;
                case HIT_LOCATION:
                case TARGET:
                    CastUtils.CastLocation castLocation =
                            CastUtils.rayTrace(
                                    entity,
                                    entity.getEyeLocation(),
                                    entity.getEyeLocation().getDirection(),
                                    power.getFiringRange());
                    location = castLocation.getTargetLocation();
                    break;
                case ENTITY:
                    location = entity.getLocation();
                    break;
            }
            return fire(power, location);
        }

        @Override
        public PowerResult<Void> fire(
                Particles power, Player player, ItemStack stack, Location location) {
            PlayLocation playLocation = power.getPlayLocation();
            switch (playLocation) {
                case SELF:
                    location = player.getLocation();
                    break;
            }
            return fire(power, location);
        }
    }

    public class EffectSetter implements Getter<Effect>, Setter<Effect> {

        @Override
        public String get(Effect effect) {
            return effect.name();
        }

        @Override
        public Optional<Effect> set(String value) {
            Particles.this.data = null;
            try {
                Effect eff = Effect.valueOf(value.toUpperCase());
                if (eff.getType() == Effect.Type.VISUAL) {
                    Particles.this.effect = eff;
                    Particles.this.particle = null;
                    return Optional.empty();
                }
                throw new AdminCommands.CommandException("message.error.visualeffect", value);
            } catch (IllegalArgumentException e) {
                DeprecatedEffect particleEffect = DeprecatedEffect.valueOf(value);
                Particles.this.effect = null;
                Particles.this.particle = particleEffect.getParticle();
                return Optional.empty();
            }
        }
    }

    public class ParticleSetter
            implements Getter<org.bukkit.Particle>, Setter<org.bukkit.Particle> {
        @Override
        public String get(org.bukkit.Particle object) {
            return object.name();
        }

        @Override
        public Optional<org.bukkit.Particle> set(String value) {
            Particles.this.data = null;
            Particles.this.effect = null;
            Particles.this.particle = org.bukkit.Particle.valueOf(value);
            return Optional.empty();
        }
    }
}
