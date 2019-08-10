package think.rpgitems.power.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Context;
import think.rpgitems.power.*;

import java.util.List;

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
@PowerMeta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class, implClass = PowerAOEDamage.Impl.class)
public class PowerAOEDamage extends BasePower {

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

    public class Impl implements PowerOffhandClick, PowerPlain, PowerLeftClick, PowerRightClick, PowerHit, PowerSprint, PowerSneak, PowerHurt, PowerHitTaken, PowerTick, PowerBowShoot, PowerSneaking {


        @Override
        public PowerResult<Void> rightClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            Context.instance().putTemp(player.getUniqueId(), DAMAGE_SOURCE, getNamespacedKey().toString());
            Context.instance().putTemp(player.getUniqueId(), OVERRIDING_DAMAGE, getDamage());
            Context.instance().putTemp(player.getUniqueId(), SUPPRESS_MELEE, isSuppressMelee());
            if (isSelfapplication()) dealDamage(player, getDamage());
            List<LivingEntity> nearbyEntities = getNearestLivingEntities(getPower(), player.getLocation(), player, getRange(), getMinrange());
            List<LivingEntity> ent = getLivingEntitiesInCone(nearbyEntities, player.getEyeLocation().toVector(), getAngle(), player.getEyeLocation().getDirection());
            LivingEntity[] entities = ent.toArray(new LivingEntity[0]);
            int c = getCount();
            for (int i = 0; i < c && i < entities.length; ++i) {
                LivingEntity e = entities[i];
                if ((isMustsee() && !player.hasLineOfSight(e))
                            || (e == player)
                            || (!isIncluePlayers() && e instanceof Player)
                ) {
                    c++;
                    continue;
                }
                if (getDelay() <= 0) {
                    e.damage(getDamage(), player);
                } else {
                    (new BukkitRunnable() {
                        @Override
                        public void run() {
                            Context.instance().putTemp(player.getUniqueId(), OVERRIDING_DAMAGE, getDamage());
                            Context.instance().putTemp(player.getUniqueId(), SUPPRESS_MELEE, isSuppressMelee());
                            Context.instance().putTemp(player.getUniqueId(), DAMAGE_SOURCE_ITEM, stack);
                            e.damage(getDamage(), player);
                            Context.instance().putTemp(player.getUniqueId(), SUPPRESS_MELEE, isSuppressMelee());
                            Context.instance().putTemp(player.getUniqueId(), OVERRIDING_DAMAGE, null);
                            Context.instance().removeTemp(player.getUniqueId(), DAMAGE_SOURCE_ITEM);
                        }
                    }).runTaskLater(RPGItems.plugin, getDelay());
                }

            }
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return PowerAOEDamage.this;
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


}
