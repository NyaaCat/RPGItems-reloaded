package think.rpgitems.power.impl;

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

import javax.annotation.Nullable;
import java.util.Optional;

import static think.rpgitems.power.Utils.checkCooldown;
import static think.rpgitems.power.Utils.sweep;

/**
 * Power particle.
 * <p>
 * When right clicked, spawn some particles around the user.
 * </p>
 */
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = ParticlePower.Impl.class)
public class ParticlePower extends BasePower {
    @Property(order = 0, required = true)
    @Serializer(EffectSetter.class)
    @Deserializer(value = EffectSetter.class, message = "message.error.visualeffect")
    @AcceptedValue(preset = Preset.VISUAL_EFFECT)
    public Effect effect = Effect.MOBSPAWNER_FLAMES;

    @Property
    @Serializer(ParticleSetter.class)
    @Deserializer(value = ParticleSetter.class)
    public Particle particle = null;

    @Property
    public int cost = 0;

    @Property
    public int cooldown = 0;

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

    @Property
    public PlayLocation playLocation = PlayLocation.HIT_LOCATION;

    @Property
    public int delay = 0;

    @Property
    public double firingRange = 20;

    public double getFiringRange() {
        return firingRange;
    }

    public int getDelay() {
        return delay;
    }

    @Property
    public boolean requireHurtByEntity = true;
    private Object data = null;

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

    void spawnParticle(World world, Location location) {
        if (getParticle() == null) {
            if (getEffect() == Effect.SMOKE) {
                world.playEffect(location.add(0, 2, 0), getEffect(), 4);
            } else {
                world.playEffect(location, getEffect(), 0);
            }
        } else {
            world.spawnParticle(getParticle(), location, getParticleCount(), getOffsetX(), getOffsetY(), getOffsetZ(), getExtra(), getData(), isForce());
        }
    }

    public org.bukkit.Particle getParticle() {
        return particle;
    }

    /**
     * Name of particle effect
     */
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
        } else if (getParticle().getDataType().equals(ItemStack.class) && getMaterial() != null && getMaterial().isItem()) {
            data = new ItemStack(getMaterial());
        } else if (getParticle().getDataType().equals(BlockData.class) && getMaterial() != null && getMaterial().isBlock()) {
            data = getMaterial().createBlockData();
        } else if (getParticle().getDataType().equals(org.bukkit.Particle.DustOptions.class)) {
            data = new org.bukkit.Particle.DustOptions(Color.fromRGB(getDustColor()), (float) getDustSize());
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

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
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

    public class Impl implements PowerRightClick, PowerLeftClick, PowerPlain, PowerHit, PowerHitTaken, PowerHurt, PowerBowShoot, PowerBeamHit, PowerProjectileHit, PowerLivingEntity {

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            Location playLocation = player.getLocation();
            PlayLocation playLocation1 = getPlayLocation();
            if (playLocation1.equals(PlayLocation.TARGET)){
                CastUtils.CastLocation castLocation = CastUtils.rayTrace(player, player.getEyeLocation(), player.getEyeLocation().getDirection(), getFiringRange());
                playLocation = castLocation.getTargetLocation();
            }

            return fire(playLocation);
        }

        private PowerResult<Void> fire(Location playLocation) {
            int delay = getDelay();
            new BukkitRunnable(){
                @Override
                public void run() {
                    spawnParticle(playLocation.getWorld(), playLocation);
                }
            }.runTaskLater(RPGItems.plugin, delay);
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return ParticlePower.this;
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
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            if (getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
                int delay = getDelay();
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        spawnParticle(entity);
                    }
                }.runTaskLater(RPGItems.plugin, delay);
            }else if (getPlayLocation().equals(PlayLocation.SELF)){
                int delay = getDelay();
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        spawnParticle(player);
                    }
                }.runTaskLater(RPGItems.plugin, delay);
            }
            return PowerResult.ok().with(damage);
        }

        @Override
        public PowerResult<Double> hitEntity(Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();

            int delay = getDelay();
            new BukkitRunnable(){
                @Override
                public void run() {
                    Location location = event.getLoc();
                    if (getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
                    }else if (getPlayLocation().equals(PlayLocation.SELF)){
                        location = player.getLocation();
                    }
                    spawnParticle(entity.getWorld(), location);
                }
            }.runTaskLater(RPGItems.plugin, delay);
            return PowerResult.ok(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();

            int delay = getDelay();
            new BukkitRunnable(){
                @Override
                public void run() {
                    Location loc = location;
                    if (getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
                    }else if (getPlayLocation().equals(PlayLocation.SELF)){
                        loc = player.getLocation();
                    }
                    spawnParticle(player.getWorld(), loc);
                }
            }.runTaskLater(RPGItems.plugin, delay);

            return PowerResult.ok();
        }

        @Override
        public PowerResult<Void> beamEnd(Player player, ItemStack stack, Location location, BeamEndEvent event) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();

            int delay = getDelay();
            new BukkitRunnable(){
                @Override
                public void run() {
                    Location loc = location;
                    if (getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
                    }else if (getPlayLocation().equals(PlayLocation.SELF)){
                        loc = player.getLocation();
                    }
                    spawnParticle(player.getWorld(), loc);
                }
            }.runTaskLater(RPGItems.plugin, delay);

            return PowerResult.ok();
        }

        @Override
        public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();

            int delay = getDelay();
            new BukkitRunnable(){
                @Override
                public void run() {
                    Location loc = event.getEntity().getLocation();
                    if (getPlayLocation().equals(PlayLocation.HIT_LOCATION)) {
                    }else if (getPlayLocation().equals(PlayLocation.SELF)){
                        loc = player.getLocation();
                    }
                    spawnParticle(player.getWorld(), loc);
                }
            }.runTaskLater(RPGItems.plugin, delay);

            return PowerResult.ok();
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, @Nullable Double value) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            Location location = player.getLocation();
            PlayLocation playLocation = getPlayLocation();
            switch (playLocation){
                case SELF:
                    break;
                case HIT_LOCATION:
                case TARGET:
                    CastUtils.CastLocation castLocation = CastUtils.rayTrace(entity, entity.getEyeLocation(), entity.getEyeLocation().getDirection(), getFiringRange());
                    location = castLocation.getTargetLocation();
                    break;
                case ENTITY:
                    location = entity.getLocation();
                    break;
            }
            return fire(location);
        }
    }

    public class EffectSetter implements Getter<Effect>, Setter<Effect> {

        @Override
        public String get(Effect effect) {
            return effect.name();
        }

        @Override
        public Optional<Effect> set(String value) {
            ParticlePower.this.data = null;
            try {
                Effect eff = Effect.valueOf(value.toUpperCase());
                if (eff.getType() == Effect.Type.VISUAL) {
                    ParticlePower.this.effect = eff;
                    ParticlePower.this.particle = null;
                    return Optional.empty();
                }
                throw new AdminCommands.CommandException("message.error.visualeffect", value);
            } catch (IllegalArgumentException e) {
                DeprecatedEffect particleEffect = DeprecatedEffect.valueOf(value);
                ParticlePower.this.effect = null;
                ParticlePower.this.particle = particleEffect.getParticle();
                return Optional.empty();
            }
        }
    }

    public class ParticleSetter implements Getter<org.bukkit.Particle>, Setter<org.bukkit.Particle> {
        @Override
        public String get(org.bukkit.Particle object) {
            return object.name();
        }

        @Override
        public Optional<org.bukkit.Particle> set(String value) {
            ParticlePower.this.data = null;
            ParticlePower.this.effect = null;
            ParticlePower.this.particle = org.bukkit.Particle.valueOf(value);
            return Optional.empty();
        }
    }
}
