package think.rpgitems.power.impl;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Context;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;
import think.rpgitems.utils.LightContext;
import think.rpgitems.utils.cast.CastUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.Double.max;
import static java.lang.Double.min;
import static think.rpgitems.Events.*;
import static think.rpgitems.power.Utils.*;

/**
 * Power AOEDamage.
 * <p>
 * On trigger the power will deal {@link #damage damage}
 * to all entities within the {@link #range range}.
 * By default, the user will not be targeted
 * as well if not set via {@link #selfapplication selfapplication}.
 * </p>
 */
@Meta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = {
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
}, implClass = AOEDamage.Impl.class)
public class AOEDamage extends BasePower {

    @Property
    public int cooldown = 0;
    @Property
    public int range = 10;
    @Property
    public int minrange = 0;
    @Property
    public double angle = 180;
    @Property
    public int count = 100;
    @Property
    public boolean incluePlayers = false;
    @Property
    public boolean selfapplication = false;
    @Property
    public boolean mustsee = false;
    @Property
    public String name = null;
    @Property
    public int cost = 0;
    @Property
    public double damage = 0;

    @Property
    public long delay = 0;

    @Property
    public boolean suppressMelee = false;

    @Property
    public boolean selectAfterDelay = false;

    @Property
    public double firingRange = 64;

    @Property
    public FiringLocation firingLocation = FiringLocation.SELF;

    @Property
    public boolean castOff = false;

    public boolean isCastOff() {
        return castOff;
    }

    public FiringLocation getFiringLocation() {
        return firingLocation;
    }

    public double getFiringRange() {
        return firingRange;
    }

    /**
     * Select target after delay.
     */
    public boolean isSelectAfterDelay() {
        return selectAfterDelay;
    }

    /**
     * Maximum view angle
     */
    public double getAngle() {
        return angle;
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

    /**
     * Maximum count, excluding the user
     */
    public int getCount() {
        return count;
    }

    /**
     * Damage of this power
     */
    public double getDamage() {
        return damage;
    }

    /**
     * Delay of the damage
     */
    public long getDelay() {
        return delay;
    }

    /**
     * Minimum radius
     */
    public int getMinrange() {
        return minrange;
    }

    /**
     * Range of the power
     */
    public int getRange() {
        return range;
    }

    /**
     * Whether include players
     */
    public boolean isIncluePlayers() {
        return incluePlayers;
    }

    /**
     * Whether only apply to the entities that player have line of sight
     */
    public boolean isMustsee() {
        return mustsee;
    }

    /**
     * Whether damage will be apply to the user
     */
    public boolean isSelfapplication() {
        return selfapplication;
    }

    /**
     * Whether to suppress the hit trigger
     */
    public boolean isSuppressMelee() {
        return suppressMelee;
    }

    /**
     * Display text of this power. Will use default text in case of null
     */
    @Override
    public String getName() {
        return "AOEDamage";
    }

    @Override
    public String displayText() {
        return getName() != null ? getName() : "Deal damage to nearby mobs";
    }

    public enum FiringLocation {
        SELF, TARGET
    }

    public class Impl implements PowerOffhandClick, PowerPlain, PowerLeftClick, PowerRightClick, PowerHit, PowerSprint, PowerSneak, PowerHurt, PowerHitTaken, PowerTick, PowerBowShoot, PowerSneaking, PowerBeamHit, PowerProjectileHit, PowerLivingEntity, PowerLocation {

        @Override
        public PowerResult<Void> rightClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            Supplier<Location> traceResultSupplier = player::getEyeLocation;
            if (getFiringLocation().equals(FiringLocation.TARGET)) {
                if (isCastOff()) {
                    CastUtils.CastLocation castLocation = CastUtils.rayTrace(player, player.getEyeLocation(), player.getEyeLocation().getDirection(), getFiringRange());
                    Location targetLocation = castLocation.getTargetLocation();
                    traceResultSupplier = () -> targetLocation;
                } else {
                    traceResultSupplier = () -> {
                        CastUtils.CastLocation castLocation = CastUtils.rayTrace(player, player.getEyeLocation(), player.getEyeLocation().getDirection(), getFiringRange());
                        Location targetLocation = castLocation.getTargetLocation();
                        return targetLocation;
                    };
                }
            }

            Supplier<Location> finalTraceResultSupplier = traceResultSupplier;
            return fire(player, stack, () -> {
                List<LivingEntity> nearbyEntities;
                List<LivingEntity> ent;
                if (getFiringLocation().equals(FiringLocation.TARGET)) {
                    Location targetLocation = finalTraceResultSupplier.get();
                    ent = getNearestLivingEntities(getPower(), targetLocation, player, getRange(), getMinrange());
                } else {
                    nearbyEntities = getNearestLivingEntities(getPower(), player.getLocation(), player, getRange(), getMinrange());
                    ent = getLivingEntitiesInCone(nearbyEntities, player.getEyeLocation().toVector(), getAngle(), player.getEyeLocation().getDirection());
                }
                return ent;
            });
        }

        private PowerResult<Void> fire(Player player, ItemStack stack, Supplier<List<LivingEntity>> supplier) {
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("targets",supplier);
            argsMap.put("damage",damage);
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent())
                return PowerResult.fail();
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            Context.instance().putTemp(player.getUniqueId(), DAMAGE_SOURCE, getNamespacedKey().toString());
            Context.instance().putTemp(player.getUniqueId(), OVERRIDING_DAMAGE, getDamage());
            Context.instance().putTemp(player.getUniqueId(), SUPPRESS_MELEE, isSuppressMelee());
            if (isSelfapplication()) dealDamage(player, getDamage());
            LivingEntity[] entities = supplier.get().toArray(new LivingEntity[0]);
            int c = getCount();
            int hitEntities = 0;

            if (getDelay() <= 0) {
                for (int i = 0; i < c && i < entities.length; ++i) {
                    LivingEntity e = entities[i];
                    if ((isMustsee() && !player.hasLineOfSight(e))
                            || (e == player)
                            || (!isIncluePlayers() && e instanceof Player)
                    ) {
                        c++;
                        continue;
                    }
                    hitEntities++;
                    LightContext.putTemp(player.getUniqueId(), OVERRIDING_DAMAGE, getDamage());
                    LightContext.putTemp(player.getUniqueId(), SUPPRESS_MELEE, isSuppressMelee());
                    LightContext.putTemp(player.getUniqueId(), DAMAGE_SOURCE_ITEM, stack);
                    e.damage(getDamage(), player);
                    LightContext.removeTemp(player.getUniqueId(), SUPPRESS_MELEE);
                    LightContext.removeTemp(player.getUniqueId(), OVERRIDING_DAMAGE);
                    LightContext.removeTemp(player.getUniqueId(), DAMAGE_SOURCE_ITEM);
                }
            } else {
                hitEntities++;
                (new BukkitRunnable() {
                    @Override
                    public void run() {
                        LivingEntity[] entities1 = entities;
                        if (isSelectAfterDelay()) {
                            entities1 = supplier.get().toArray(new LivingEntity[0]);
                        }
                        int c = getCount();

                        for (int i = 0; i < c && i < entities1.length; ++i) {
                            LivingEntity e = entities1[i];
                            if ((isMustsee() && !player.hasLineOfSight(e))
                                    || (e == player)
                                    || (!isIncluePlayers() && e instanceof Player)
                            ) {
                                c++;
                                continue;
                            }
                            LightContext.putTemp(player.getUniqueId(), OVERRIDING_DAMAGE, getDamage());
                            LightContext.putTemp(player.getUniqueId(), SUPPRESS_MELEE, isSuppressMelee());
                            LightContext.putTemp(player.getUniqueId(), DAMAGE_SOURCE_ITEM, stack);
                            e.damage(getDamage(), player);
                            LightContext.removeTemp(player.getUniqueId(), SUPPRESS_MELEE);
                            LightContext.removeTemp(player.getUniqueId(), OVERRIDING_DAMAGE);
                            LightContext.removeTemp(player.getUniqueId(), DAMAGE_SOURCE_ITEM);
                        }
                    }
                }).runTaskLater(RPGItems.plugin, getDelay());
            }

            return hitEntities > 0 ? PowerResult.ok() : PowerResult.noop();
        }

        @Override
        public Power getPower() {
            return AOEDamage.this;
        }

        private void dealDamage(LivingEntity entity, double damage) {
            if (entity.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                PotionEffect e = entity.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                if (e.getAmplifier() >= 4) return;
            }
            double health = entity.getHealth();
            double newHealth = health - damage;
            newHealth = max(newHealth, 0.1);
            newHealth = min(newHealth, entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            entity.setHealth(newHealth);
        }

        @Override
        public PowerResult<Void> leftClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> offhandClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack).with(damage);
        }

        @Override
        public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            return fire(target, stack);
        }

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            return fire(target, stack).with(damage);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sneaking(Player player, ItemStack stack) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Double> hitEntity(Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            Location location = entity.getLocation();
            int range = getRange();
            return fire(player, stack, () -> getNearbyEntities(player, location, range)).with(damage);
        }

        private List<LivingEntity> getNearbyEntities(Player player, Location location, int range) {
            return Utils.getNearbyEntities(getPower(), location, player, range).stream()
                    .filter(entity -> entity instanceof LivingEntity)
                    .map(entity -> ((LivingEntity) entity))
                    .collect(Collectors.toList());
        }

        @Override
        public PowerResult<Void> hitBlock(Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            int range = getRange();
            return fire(player, stack, () -> getNearbyEntities(player, location, range));
        }

        @Override
        public PowerResult<Void> beamEnd(Player player, ItemStack stack, Location location, BeamEndEvent event) {
            int range = getRange();
            return fire(player, stack, () -> getNearbyEntities(player, location, range));
        }

        @Override
        public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
            int range = getRange();
            return fire(player, stack, () -> getNearbyEntities(player, event.getEntity().getLocation(), range));
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, @Nullable Double value) {
            Supplier<Location> traceResultSupplier = entity::getEyeLocation;
            if (getFiringLocation().equals(FiringLocation.TARGET)) {
                if (isCastOff()) {
                    CastUtils.CastLocation castLocation = CastUtils.rayTrace(entity, entity.getEyeLocation(), entity.getEyeLocation().getDirection(), getFiringRange());
                    Location targetLocation = castLocation.getTargetLocation();
                    traceResultSupplier = () -> targetLocation;
                } else {
                    traceResultSupplier = () -> {
                        CastUtils.CastLocation castLocation = CastUtils.rayTrace(entity, entity.getEyeLocation(), entity.getEyeLocation().getDirection(), getFiringRange());
                        Location targetLocation = castLocation.getTargetLocation();
                        return targetLocation;
                    };
                }
            }

            Supplier<Location> finalTraceResultSupplier = traceResultSupplier;
            return fire(player, stack, () -> {
                List<LivingEntity> nearbyEntities;
                List<LivingEntity> ent;
                if (getFiringLocation().equals(FiringLocation.TARGET)) {
                    Location targetLocation = finalTraceResultSupplier.get();
                    ent = getNearestLivingEntities(getPower(), targetLocation, player, getRange(), getMinrange());
                } else {
                    nearbyEntities = getNearestLivingEntities(getPower(), player.getLocation(), player, getRange(), getMinrange());
                    ent = getLivingEntitiesInCone(nearbyEntities, player.getEyeLocation().toVector(), getAngle(), player.getEyeLocation().getDirection());
                }
                return ent;
            });
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, Location location) {
            int range = getRange();
            return fire(player, stack, () -> getNearbyEntities(player, location, range));
        }
    }


}
